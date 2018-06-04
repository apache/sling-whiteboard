/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.fileoptim.impl;

import java.io.IOException;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.fileoptim.FileOptimizerService;
import org.apache.sling.fileoptim.impl.FileOptimizerEventHandler.Config;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An event filter to trigger to optimize images when they have been saved by
 * compressing.
 */
@Component(service = EventHandler.class, immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = Config.class)
public class FileOptimizerEventHandler implements EventHandler {

	@ObjectClassDefinition(name = "%event.handler.name", description = "%event.handler.description", localization = "OSGI-INF/l10n/bundle")
	public @interface Config {
		@AttributeDefinition(name = "%event.handler.filter.name", description = "%event.handler.filter.description")
		String event_filter() default "(&(resourceType=sling:File)(|(path=*.png)(path=*.jpg)))";

		@AttributeDefinition(name = "%event.handler.topics.name", description = "%event.handler.topics.description")
		String[] event_topics() default { SlingConstants.TOPIC_RESOURCE_ADDED, SlingConstants.TOPIC_RESOURCE_CHANGED };
	}

	private static final Logger log = LoggerFactory.getLogger(FileOptimizerEventHandler.class);

	@Reference
	private FileOptimizerService fileOptimizer;

	private ResourceResolver resourceResolver;

	@Reference
	private ResourceResolverFactory rrf;

	@Activate
	@Modified
	public void activate(Config config) throws LoginException {
		deactivate();
		resourceResolver = rrf.getServiceResourceResolver(null);
	}

	@Deactivate
	public void deactivate() {
		if (resourceResolver != null) {
			resourceResolver.close();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
	 */
	@Override
	public void handleEvent(Event event) {
		String path = (String) event.getProperty(SlingConstants.PROPERTY_PATH);
		Resource fileResource = resourceResolver.getResource(path);
		if (fileResource != null && fileOptimizer.canOptimize(fileResource)) {
			try {
				fileOptimizer.optimizeFile(fileResource, true);
			} catch (IOException e) {
				log.error("Exception saving optimized file", e);
			}
		}
	}

}
