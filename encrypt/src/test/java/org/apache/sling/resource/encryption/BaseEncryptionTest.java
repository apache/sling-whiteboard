/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.resource.encryption;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import javax.crypto.NoSuchPaddingException;

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.resource.encryption.impl.EncryptableValueMapAdapterFactory;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class BaseEncryptionTest {

    @Rule
    public final SlingContext context = new SlingContext();

    private static String START_PATH = "/content/sample/en";

    private static String ARRAY_PATH = "/content/sample/en/testpage1/jcr:content";

    String encryptedProperty;

    @SuppressWarnings("serial")
    @Before
    public void setUp() throws IOException, GeneralSecurityException {
        context.load().json("/data.json", START_PATH);
        EncryptionProvider cipherProvider = new Base64EncryptionProvider();
        context.registerService(EncryptionProvider.class, cipherProvider);
        EncryptableValueMapAdapterFactory factory = new EncryptableValueMapAdapterFactory();
        injectCipherProvider(factory, cipherProvider);
        context.registerService(AdapterFactory.class, factory, new HashMap<String, Object>() {
            {
                put(AdapterFactory.ADAPTABLE_CLASSES, new String[] { Resource.class.getName() });
                put(AdapterFactory.ADAPTER_CLASSES, new String[] { EncryptableValueMap.class.getName() });
            }
        });

        this.encryptedProperty = "bar";
    }

    private void injectCipherProvider(EncryptableValueMapAdapterFactory factory, EncryptionProvider cipherProvider) {
        Class<?> resolverClass = factory.getClass();
        java.lang.reflect.Field resolverField;
        try {
            resolverField = resolverClass.getDeclaredField("encryptionProvider");
            resolverField.setAccessible(true);
            resolverField.set(factory, cipherProvider);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }

    }

    /**
     * Tests initial access to the EncryptionProvider and that the test provider
     * returns expected results Cipher classes
     * 
     * @throws Exception
     */
    @Test
    public void testCipherProvider() throws Exception {
        EncryptionProvider cipher = context.getService(EncryptionProvider.class);
        assertNotNull(cipher);

        String encrypted = cipher.encrypt(START_PATH, "foo");
        assertNotEquals(START_PATH, encrypted);
        String decoded = cipher.decrypt(encrypted, "foo");
        assertEquals(START_PATH, decoded);
    }

    /**
     * Tests encryption and decryption of a String value
     * 
     */
    @Test
    public void testUnencryptedString() {
        Resource resource = context.resourceResolver().getResource(START_PATH);

        ValueMap map = resource.adaptTo(ValueMap.class);
        EncryptableValueMap encrytionMap = resource.adaptTo(EncryptableValueMap.class);

        String property = "jcr:primaryType";

        // get original value
        String value = encrytionMap.get(property, String.class);
        assertEquals("app:Page", value);

        // encrypt property and validate property appears to be the same
        encrytionMap.encrypt(property);
        value = encrytionMap.get(property, String.class);
        assertEquals("app:Page", value);

        // validate the underlying value is encrypted
        value = map.get(property, "fail");
        assertNotEquals("app:Page", value);
        assertNotEquals("fail", value);

        // decrypt property and validate the underlying map is back to normal
        encrytionMap.decrypt(property);
        value = map.get(property, String.class);
        assertEquals("app:Page", value);
    }

    /**
     * Tests Encodind and Decoding of an array of String values
     * 
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    @Test
    public void testArrayofValues() {
        Resource resource = context.resourceResolver().getResource(ARRAY_PATH);

        ValueMap map = resource.adaptTo(ValueMap.class);
        EncryptableValueMap encrytionMap = resource.adaptTo(EncryptableValueMap.class);

        String property = "foo";

        // get original values
        String[] value = encrytionMap.get(property, String[].class);
        assertArrayEquals(new String[] { "foo", "dog" }, value);

        // encrypt property and verifies it looks good
        encrytionMap.encrypt(property);
        value = encrytionMap.get(property, String[].class);
        assertArrayEquals(new String[] { "foo", "dog" }, value);

        // verify underlying map is encrypted
        value = map.get(property, String[].class);
        assertNotEquals("foo", value[0]);
        assertNotEquals("dog", value[1]);

        // decrypt the property and validate
        encrytionMap.decrypt(property);
        value = encrytionMap.get(property, String[].class);
        assertArrayEquals(new String[] { "foo", "dog" }, value);

        // verify underlying map is decrypted
        value = map.get(property, String[].class);
        assertArrayEquals(new String[] { "foo", "dog" }, value);

    }

    /**
     * Tests the decryption and handling of pre-existing values
     * 
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    @Test
    public void testPreEncryptedArrayofValues() {
        Resource resource = context.resourceResolver().getResource(ARRAY_PATH);

        ValueMap map = resource.adaptTo(ValueMap.class);
        EncryptableValueMap encryptionMap = resource.adaptTo(EncryptableValueMap.class);

        // verify original is encrypted
        String[] value = map.get(encryptedProperty, String[].class);
        assertNotEquals("foo", value[0]);
        assertNotEquals("dog", value[1]);

        // get decrypted values and validate
        value = (String[]) encryptionMap.get(encryptedProperty);
        assertArrayEquals(new String[] { "foo", "dog" }, value);

        // decrypt pre-encrypted properties
        encryptionMap.decrypt(encryptedProperty);
        value = (String[]) map.get(encryptedProperty);
        assertArrayEquals(new String[] { "foo", "dog" }, value);
    }

}
