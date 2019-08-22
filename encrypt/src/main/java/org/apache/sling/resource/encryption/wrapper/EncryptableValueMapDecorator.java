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
package org.apache.sling.resource.encryption.wrapper;

import java.util.Map;
import java.util.stream.Stream;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.wrappers.ModifiableValueMapDecorator;
import org.apache.sling.resource.encryption.EncryptionException;
import org.apache.sling.resource.encryption.EncryptionProvider;
import org.apache.sling.resource.encryption.EncryptableValueMap;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The <code>EncryptionValueMap</code> is an easy way to encrypt and decrypt
 * properties of a resource. With resources you can use
 * {@link Resource#adaptTo()} to obtain a map that support encryption.
 * <p>
 *
 * A <code>ValueMap</code> should be immutable.
 */
public class EncryptableValueMapDecorator extends ModifiableValueMapDecorator
        implements ModifiableValueMap, EncryptableValueMap {

    private EncryptionProvider ep;

    /** Default logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public EncryptableValueMapDecorator(Map<String, Object> base, EncryptionProvider encryptionProvider) {
        super(base);
        ep = encryptionProvider;
    }

    /**
     * Encrypts the value for this
     * 
     * @param property
     *            property
     */
    @Nullable
    public void encrypt(String property) {
        super.put(property, doEncrypt(property, get(property)));
    }

    /**
     * Sets a String property with the given name as being a non-encrypted property.
     * If a String value currently exists for the property and that value is
     * currently encrypted, that value will be decrypted. Parameters that are
     * already decrypted will not change.
     * 
     * @param property
     *            The name of the property
     */
    public void decrypt(String property) {
        super.put(property, get(property));
    };

    @Override
    public Object get(Object key) {
        Object reply = super.get(key);
        if (isEncrypted(reply)) {
            return doDecrypt((String) key, reply);
        }
        return reply;
    }

    @Override
    public Object put(String key, Object value) {
        Object prior = super.put(key, value);
        if (isEncrypted(prior)) {
            super.put(key, doEncrypt(key, value));
            return doDecrypt(key, prior);
        }
        return prior;
    }

    /**
     * Method to encrypt an Object value.
     * 
     * @param value
     *            to be encrypted
     * @return the encrypted value
     */
    @SuppressWarnings("unchecked")
    private <T> T doEncrypt(String property, T value) {
        T reply = null;

        if (value instanceof String) {
            try {
                reply = (T) ep.encrypt((String) value, property);
            } catch (EncryptionException e) {
                logger.debug("unable to encrypt value {} of property {}", value, property);
                reply = value;
            }
        }

        if (value instanceof String[]) {
            reply = (T) Stream.of((String[]) value).map(string -> {
                try {
                    return ep.encrypt(string, property);
                } catch (EncryptionException e) {
                    logger.debug("unable to encrypt value {} of property {}", string, property);
                    return string;
                }
            }).toArray(String[]::new);
        }

        return reply;
    }

    /**
     * Decrypt the object
     * 
     * @param value
     *            String representation of the encrypted value
     * @return decrypted value
     */
    @SuppressWarnings("unchecked")
    private <T> T doDecrypt(String property, T value) {
        T reply = null;

        if (value instanceof String) {
            try {
                reply = (T) ep.decrypt((String) value, property);
            } catch (EncryptionException e) {
                logger.debug("unable to decrypt value {} of property {}", value, property);
                reply = value;
            }
        }

        if (value instanceof String[]) {
            reply = (T) Stream.of((String[]) value).map(string -> {
                try {
                    return ep.decrypt(string, property);
                } catch (EncryptionException e) {
                    logger.debug("unable to decrypt value {} of property {}", string, property);
                    return string;
                }
            }).toArray(String[]::new);
        }

        return reply;
    }

    private boolean isEncrypted(Object value) {
        if (value == null) {
            return false;
        }

        if (value instanceof String) {
            return ep.isEncrypted((String) value);
        }

        if (value instanceof String[]) {
            String[] temp = (String[]) value;
            if (temp.length > 0) {
                return ep.isEncrypted(temp[0]);
            }
        }
        return false;
    }

}
