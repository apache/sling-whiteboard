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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jcr.Session;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.resource.encryption.EncryptionProvider;
import org.apache.sling.resource.encryption.EncryptableValueMap;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Designate(ocd = EncryptPostProcessor.Configuration.class)
public class EncryptPostProcessor implements SlingPostProcessor {

	@ObjectClassDefinition(name = "Apache Sling Encryption Post Processor", description = "Defines options for field encryption")
	public @interface Configuration {

		@AttributeDefinition(name = "Suffix", description = "Define the suffix which will uniquely identify a field to be encrypted")
		String suffix() default "@Encrypt";

		@AttributeDefinition(name = "Inline", description = "Whether the encrypted flag is set on itself or another field")
		boolean inline() default false;

	}

	@Reference(policyOption = ReferencePolicyOption.GREEDY)
	public EncryptionProvider ep;

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	private Configuration config;

	@Override
	public void process(SlingHttpServletRequest slingRequest, List<Modification> modifications) throws Exception {

		Set<Modification> mods = modifications.stream()
				.filter(modification -> modification.getSource().endsWith(config.suffix())).collect(Collectors.toSet());

		if (mods.size() == 0) {
			return;
		}

		ResourceResolver resolver = slingRequest.getResourceResolver();
		Session session = resolver.adaptTo(Session.class);

		for (Modification mod : mods) {
			String encryptPropertyPath = mod.getSource();

			String propertyPath = encryptPropertyPath.substring(0, encryptPropertyPath.lastIndexOf(config.suffix()));
			String resourcePath = propertyPath.substring(0, propertyPath.lastIndexOf('/'));

			if (config.inline()) {
				session.move(encryptPropertyPath, propertyPath);
			}
			
			EncryptableValueMap map = resolver.resolve(resourcePath).adaptTo(EncryptableValueMap.class);
			map.encrypt(propertyPath.substring(resourcePath.length() + 1, propertyPath.length()));
			session.removeItem(encryptPropertyPath);
		}

		modifications.removeAll(mods);

		mods.forEach(mod -> {
			logger.debug("removed {} for source {}", mod.getType().toString(), mod.getSource());
		});
	}

	@Activate
	@Modified
	public void init(Configuration config) {
		this.config = config;
	}

}
