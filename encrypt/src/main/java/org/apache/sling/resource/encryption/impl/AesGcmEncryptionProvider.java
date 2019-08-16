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
package org.apache.sling.resource.encryption.impl;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;

import org.apache.sling.resource.encryption.EncryptionException;
import org.apache.sling.resource.encryption.EncryptionProvider;
import org.apache.sling.resource.encryption.KeyProvider;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a basic implementation of the EncryptionProvider which utilizes
 * 'AES/GCM/NoPadding' encryption. Before using this in a production environment
 * please consult with your Security team. Individual organizations may require
 * a higer degree of encryption.
 * 
 */
@Component(immediate = true, property = { Constants.SERVICE_DESCRIPTION + "=Sling Encryption Service Provider",
        Constants.SERVICE_VENDOR
                + "=The Apache Software Foundation" }, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = AesGcmEncryptionProvider.Configuration.class)
public class AesGcmEncryptionProvider implements EncryptionProvider {

    @ObjectClassDefinition(name = "Apache Sling Encryption Provider - AES/GCM ")
    public @interface Configuration {

        @AttributeDefinition(name = "Key Provider Target", description = "KeyProvider Filter")
        String keyProvider_target() default "(provider.type=KeyStore)";

        @AttributeDefinition(name = "Encryption Prefix", description = "Prepends to Encrypted string to identify content that is encrypted")
        String encryptionPrefix() default "\uD83D\uDD12";

    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY, target = "(name=)")
    public KeyProvider keyProvider;

    private SecureRandom random;

    private int ivSize;

    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private String id = "\uD83D\uDD12";

    private static final Charset UTF8 = StandardCharsets.UTF_8;

    private static final int GCM_TAG_LENGTH = 128;

    /** Default logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Provides an initial check to make sure that the pieces of the security
     * implementation are present
     * 
     * @param config
     * @throws GeneralSecurityException
     */
    @Activate
    @Modified
    public void init(Configuration config) throws GeneralSecurityException {
        Key secretKey = keyProvider.getKey(keyProvider.getPrimaryKeyID());
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        this.random = SecureRandom.getInstance("SHA1PRNG");
        this.ivSize = cipher.getIV().length;
        this.id = config.encryptionPrefix();
    }

    private Cipher getCipher(int cipherMode, byte[] iv, byte[] aad, byte[] keyId) throws GeneralSecurityException {
        Key secretKey = keyProvider.getKey(keyId);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(cipherMode, secretKey, spec);
        cipher.updateAAD(aad);
        return cipher;
    }

    private byte[] generateIV() {
        byte[] iv = new byte[ivSize];
        random.nextBytes(iv);
        return iv;
    }

    /**
     * Encrypts the byte[] using a random IV which is then prepended to the results.
     */
    @Override
    public byte[] encrypt(byte[] toEncode, byte[] aad) throws EncryptionException {
        byte[] iv = generateIV();
        byte[] keyId = keyProvider.getPrimaryKeyID();
        byte[] byteEncyrpted;
        try {
            byteEncyrpted = getCipher(Cipher.ENCRYPT_MODE, iv, aad, keyId).doFinal(toEncode);
        } catch (Exception e) {
            logger.debug("unable to decrypt {}", e);
            throw new EncryptionException(e);
        }
        ByteBuffer buffer = ByteBuffer.allocate(iv.length + keyId.length + byteEncyrpted.length);
        buffer.put(iv).put(keyId).put(byteEncyrpted);
        return buffer.array();
    }

    /**
     * Encryption occurs on the underlying bytes of the String. Before returning the
     * encrypted bytes are converted to a Base64 representation and prepended with
     * an ID to indicate that the String represents an encoded value.
     */
    @Override
    public String encrypt(String value, String aad) throws EncryptionException {
        byte[] encoded = encrypt(value.getBytes(), aad.getBytes(UTF8));
        return id + new String(Base64.getEncoder().encode(encoded), UTF8);
    }

    /**
     * Decrypts the supplied byte[] using the the IV that was prepending to the
     * byte[]
     */
    @Override
    public byte[] decrypt(byte[] toDecode, byte[] aad) throws EncryptionException {
        byte[] iv = new byte[ivSize];
        byte[] keyId = new byte[keyProvider.getIdLength()];
        byte[] byteEncrypted = new byte[toDecode.length - (ivSize + keyProvider.getIdLength())];
        ByteBuffer buffer = ByteBuffer.wrap(toDecode);
        buffer.get(iv).get(keyId).get(byteEncrypted);
        try {
            return getCipher(Cipher.DECRYPT_MODE, iv, aad, keyId).doFinal(byteEncrypted);
        } catch (GeneralSecurityException e) {
            throw new EncryptionException(e);
        }
    }

    /**
     * Decrypts the String after first removing the prepending ID and decrypting the
     * remaining Base64 encoded byte[]
     */
    @Override
    public String decrypt(String value, String aad) throws EncryptionException {
        byte[] bValue = value.substring(id.length()).getBytes(UTF8);
        byte[] toDecode;
        try {
            toDecode = Base64.getDecoder().decode(bValue);
        } catch (IllegalArgumentException e) {
            throw new EncryptionException("non-encrypted value");
        }
        return new String(decrypt(toDecode, aad.getBytes(UTF8)), UTF8);
    }

    @Override
    public boolean isEncrypted(String value) {
        return (value.startsWith(id) && value.length() > id.length() + ivSize);
    }

}
