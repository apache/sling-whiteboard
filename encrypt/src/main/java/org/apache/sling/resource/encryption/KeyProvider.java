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

import java.security.GeneralSecurityException;
import java.security.Key;

/**
 * Provides access to a primary key for encryption as well as supporting
 * multiple secondary keys. Key rotation is enabled by providing a new primary
 * key and taking the existing primary key and making it a secondary. The
 * secondary key should be provided until such a time that these key(s) are no
 * longer needed and/or that all prior uses of that key has been re-encrypted
 * with the new primary key.
 * 
 * Each KeyProvider is responsible for maininting an associated list of ID's for
 * the contained keys. ID's should be unique for each key and consist of a
 * byte[] of a consistent length
 * 
 * Examples implementations of a KeyProvider would provide access:
 * <ul>
 * <li>java keystore
 * <li>osgi based configuration
 * <li>third party key vaults
 * </ul>
 * 
 */
public interface KeyProvider {

	public static String TYPE = "provider.type";

	/**
	 * Provides an ID to access the primary encryption key, this ID must be uniquely
	 * associated to the key such that no other key that is managed has the same ID
	 * 
	 * @return an array of byte[] of consistent length that
	 */
	byte[] getPrimaryKeyID();

	/**
	 * ID's are byte arrays of consistent length which uniquely identifies a key
	 * 
	 * @return length of byte[] for ID
	 */
	int getIdLength();

	/**
	 * Provides the key associated with this ID, either the primary key or one of
	 * the maintained secondary keys
	 * 
	 * @param alias
	 * @return the requested key, or null if the id does not match an existing key
	 */
	Key getKey(byte[] id) throws GeneralSecurityException;

}
