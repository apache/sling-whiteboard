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

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.security.GeneralSecurityException;
import java.util.HashMap;

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.resource.encryption.impl.AesGcmEncryptionProvider;
import org.apache.sling.resource.encryption.impl.AesGcmEncryptionProvider.Configuration;
import org.apache.sling.resource.encryption.impl.OSGiKeyProvider;
import org.apache.sling.resource.encryption.wrapper.EncryptableValueMapDecorator;
import org.junit.Before;

public class EncryptionOSGiStoreTest extends BaseEncryptionTest {

	private static String START_PATH = "/content/sample/en";

	@SuppressWarnings("serial")
	@Before
	public synchronized void setUp() {

		context.load().json("/data3.json", START_PATH);
		AesGcmEncryptionProvider encryptionProvider = new AesGcmEncryptionProvider();
		OSGiKeyProvider kp = new OSGiKeyProvider();
		try {

			kp.init(getConfig());

			injectKeyProvider(encryptionProvider, kp);
			encryptionProvider.init(new Configuration() {

				@Override
				public Class<? extends Annotation> annotationType() {
					// TODO Auto-generated method stub
					return null;
				}

				@Override
				public String keyProvider_target() {
					return null;
				}

				@Override
				public String encryptionPrefix() {
					return "\uD83D\uDD12";
				}
			});

		} catch (NullPointerException | GeneralSecurityException | IOException e) {
			assertTrue(false);
		}
		context.registerService(EncryptionProvider.class, encryptionProvider);

		context.registerService(AdapterFactory.class, adapterFactory(encryptionProvider),
				new HashMap<String, Object>() {
					{
						put(AdapterFactory.ADAPTABLE_CLASSES, new String[] { Resource.class.getName() });
						put(AdapterFactory.ADAPTER_CLASSES, new String[] { EncryptableValueMap.class.getName() });
					}
				});
		this.encryptedProperty = "cat";
	}

	private AdapterFactory adapterFactory(EncryptionProvider ep) {
		return new AdapterFactory() {
			@SuppressWarnings("unchecked")
			public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
				ValueMap map = ((Resource) adaptable).adaptTo(ModifiableValueMap.class);
				if (map == null) {
					map = ((Resource) adaptable).adaptTo(ValueMap.class);
				}
				return (AdapterType) new EncryptableValueMapDecorator(map, ep);
			}
		};
	}

	private void injectKeyProvider(EncryptionProvider ep, KeyProvider key) {
		Class<?> resolverClass = ep.getClass();
		java.lang.reflect.Field resolverField;
		try {
			resolverField = resolverClass.getDeclaredField("keyProvider");
			resolverField.setAccessible(true);
			resolverField.set(ep, key);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}

	}

	private OSGiKeyProvider.Configuration getConfig() {
		return new OSGiKeyProvider.Configuration() {

			@Override
			public Class<? extends Annotation> annotationType() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String keyAlgorithm() {
				return "AES";
			}

			@Override
			public String primaryAlias() {
				return "cGFzc3dvcmRwYXNzd29yZA==";
			}

			@Override
			public String[] secondaryAliases() {
				return new String[] {};
			}
		};
	}
}
