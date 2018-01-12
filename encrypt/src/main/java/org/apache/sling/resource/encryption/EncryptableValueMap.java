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

import org.apache.sling.api.resource.ModifiableValueMap;

/**
 * The <code>EncryptableValueMap</code> provides for specifying the encryption
 * and decryption specific values within a resource.
 *
 */
public interface EncryptableValueMap extends ModifiableValueMap {

	/**
	 * Encrypts and stores the existing property value. Currently supports String
	 * and String[]. Values that already encrypted will be re-encrypted.
	 * 
	 * @param name
	 *            property
	 */
	void encrypt(String property);

	/**
	 * Decrypts and stores the existing property value . Properties that are not
	 * encrypted will not change.
	 * 
	 * @param name
	 *            property
	 */
	void decrypt(String property);

}
