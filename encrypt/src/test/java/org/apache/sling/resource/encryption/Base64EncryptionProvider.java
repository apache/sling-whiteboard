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

import java.io.UnsupportedEncodingException;
import java.util.Base64;

public class Base64EncryptionProvider implements EncryptionProvider {

    private static final String ID = "|";

    @Override
    public byte[] encrypt(byte[] toEncode, byte[] aad) throws EncryptionException {
        return Base64.getEncoder().encode(toEncode);
    }

    @Override
    public byte[] decrypt(byte[] toDecode, byte[] aad) throws EncryptionException {
        return Base64.getDecoder().decode(toDecode);
    }

    @Override
    public String encrypt(String toEncode, String aad) throws EncryptionException {
        try {
            return ID + new String(encrypt(toEncode.getBytes("UTF-8"), aad.getBytes("UTF-8")), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new EncryptionException(e);
        }
    }

    @Override
    public String decrypt(String toDecode, String aad) throws EncryptionException {
        try {
            return new String(decrypt(toDecode.substring(1).getBytes("UTF-8"), aad.getBytes("UTF-8")), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new EncryptionException(e);
        }
    }

    @Override
    public boolean isEncrypted(String property) {
        return property.startsWith(ID);
    }

}
