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

/**
 * Encapsulates the encryption and decryption of data. Implementations of this
 * interface should guarantee encryption and decryption of types.
 * 
 * I.e. If you encrypt a byte array this a method then the complimentary decrypt
 * will successfully decrypt it. However if you encrypt a String and then
 * convert that String to bytes it is up to the implementation as to whether the
 * decryption of bytes will work.
 *
 */
public interface EncryptionProvider {

	/**
	 * Encrypts a byte array
	 * 
	 * @param toEncode
	 * @param aad
	 *            optional additional authentication data
	 * @return encrypted byte array
	 * @throws EncryptionException
	 */
	byte[] encrypt(byte[] toEncode, byte[] aad) throws EncryptionException;

	/**
	 * Decrypts a previously encrypted byte array
	 * 
	 * @param toDecode
	 * @param aad
	 *            optional additional authentication data
	 * @return
	 * @throws EncryptionException
	 */
	byte[] decrypt(byte[] toDecode, byte[] aad) throws EncryptionException;

	/**
	 * Encrypts a String and returns a String representation of that encoding
	 * 
	 * @param toEncode
	 * @param aad
	 *            optional additional authentication data
	 * @return
	 * @throws EncryptionException
	 */
	String encrypt(String toEncode, String aad) throws EncryptionException;

	/**
	 * Takes a previously encrypted String and returns the original value
	 * 
	 * @param toDecode
	 * @param aad
	 *            optional additional authentication data
	 * @return
	 * @throws EncryptionException
	 */
	String decrypt(String toDecode, String aad) throws EncryptionException;

	/**
	 * Validate whether the String appears to be encrypted
	 * 
	 * @param property
	 * @return
	 */
	boolean isEncrypted(String property);

}
