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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;

import javax.crypto.spec.SecretKeySpec;

/**
 * Utility for the creation of the keystore used by junit
 *
 */
public class Create {

	private static String KEY_ALGORITHM = "AES";
	
	public static void main(String[] args) throws GeneralSecurityException, IOException {
		KeyStore ks = KeyStore.getInstance("JCEKS");
		ks.load(null);
		char[] password = "secret".toCharArray();
		ks.setKeyEntry("old", getCipher1(),password,(Certificate[])null);
		ks.setKeyEntry("new", getCipher2(),password,(Certificate[])null);
		FileOutputStream writeStream = new FileOutputStream("./src/test/resources/keystore.jks");
		ks.store(writeStream, password);
		writeStream.close();
		System.out.println(new File("./keystore").getAbsolutePath());
	}
	
	private static Key getCipher1() throws GeneralSecurityException {
		return new SecretKeySpec("passwordpassword".getBytes(), KEY_ALGORITHM);
	}
	
	private static Key getCipher2() throws GeneralSecurityException {
		return new SecretKeySpec("password2passwor".getBytes(), KEY_ALGORITHM);
	}

}
