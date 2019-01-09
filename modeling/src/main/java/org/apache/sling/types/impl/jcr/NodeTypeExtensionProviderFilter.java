/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.types.impl.jcr;

import static org.osgi.framework.Constants.SERVICE_RANKING;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.types.spi.ExtensionProvider;
import org.apache.sling.types.spi.ExtensionProviderFilter;
import org.jetbrains.annotations.NotNull;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Component;

@Component(
	property = {
        SERVICE_RANKING + "=0"
    }
)
public class NodeTypeExtensionProviderFilter implements ExtensionProviderFilter {

	@SuppressWarnings("null")
	@Override
	@NotNull
	public <T extends ExtensionProvider> Stream<ServiceReference<T>> filter(
			@NotNull Collection<ServiceReference<T>> refs, @NotNull Resource resource) {
		Node node = resource.adaptTo(Node.class);

		if (node == null) {
			return Stream.empty();
		}

		return refs.stream().filter(r -> {
			String[] types = PropertiesUtil.toStringArray(r.getProperty("sling.resource.jcr.nodeType"));
			if (types == null) {
				return false;
			}
			return Arrays.stream(types).anyMatch(t -> {
				try {
					return node.isNodeType(t);
				} catch (RepositoryException e) {
					throw new RuntimeException(e);
				}
			});
		});
	}
}
