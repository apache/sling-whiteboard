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
package org.apache.sling.mvresource.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.spi.resource.provider.ProviderContext;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = { ResourceProvider.class, AdapterFactory.class }, property = {
        Constants.SERVICE_DESCRIPTION + "=Sling Mv Resource Provider",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation",
        ResourceProvider.PROPERTY_ROOT + "=/content/apache/fake", ResourceProvider.PROPERTY_MODIFIABLE + "=true",
        "adaptables=org.apache.sling.mvresource.impl.MvResource",
        "adapters=org.apache.sling.api.resource.ModifiableValueMap",
        ResourceProvider.PROPERTY_AUTHENTICATE + "=" + ResourceProvider.AUTHENTICATE_REQUIRED })
public class MvStoreResourceProvider extends ResourceProvider<Object> implements AdapterFactory {

    private static final Logger LOG = LoggerFactory.getLogger(MvStoreResourceProvider.class);

    @ObjectClassDefinition(name = "Apache Sling Resource Provider", description = "Configure an instance of the file system "
            + "resource provider in terms of provider root and file system location")
    public @interface Config {

        @AttributeDefinition(name = "DataStore File Name", description = "File system directory mapped to the virtual "
                + "resource tree. This property must not be an empty string. If the path is "
                + "relative it is resolved against sling.home or the current working directory. "
                + "The path may be a file or folder. If the path does not address an existing "
                + "file or folder, an empty folder is created.")
        String provider_file();

        @AttributeDefinition(name = "Provider Root", description = "Location in the virtual resource tree where the "
                + "file system resources are mapped in. This property must not be an empty string.")
        String provider_root();

        @AttributeDefinition(name = "Cache Size", description = "Cache size in MB")
        int provider_cache_size() default 1024;

        // Internal Name hint for web console.
        String webconsole_configurationFactory_nameHint() default "{provider.fs.mode}: {"
                + ResourceProvider.PROPERTY_ROOT + "}";
    }

    MVStore store;

    @Override
    public void start(ProviderContext ctx) {
        LOG.info("mvprovider has started");
        super.start(ctx);
        store = MVStore.open("dataStore");
    }

    @Override
    public void stop() {
        super.stop();
        store.close();
    }

    @Override
    public Resource getResource(ResolveContext<Object> context, String resourcePath, ResourceContext resourceContext,
            Resource parentResource) {
        LOG.info("GET {} ", resourcePath);
        MVMap<String, Object> properties = store.openMap(resourcePath);
        if (properties.isEmpty()) {
            if (resourcePath.equals("/content/apache/fake")) {
                properties.put("jcr:primaryType", "sling:Site");
                properties.put("jcr:title", "Fake Site");
                properties.put("jcr:language", "en");
                properties.put("sling:url", "https://sling.apache.org");
                store.commit();
            }
            return null;
        }
        return new MvResource(context.getResourceResolver(), resourcePath, properties);
    }

    @Override
    public Resource create(ResolveContext<Object> ctx, String path, Map<String, Object> properties)
            throws PersistenceException {
        LOG.info("CREATE  {} ", path);
        try {
            String parent = parentPath(path);
            String child = currentName(path);
            MVMap<String, String[]> parentResource = store.openMap("_children");
            String[] children = parentResource.getOrDefault(parent, new String[] {});
            String[] newChildren = Arrays.copyOf(children, children.length + 1);
            newChildren[children.length] = child;
            parentResource.put(parent, newChildren);
            MVMap<String, Object> data = store.openMap(path);
            data.putAll(properties);
            store.commit();
            return new MvResource(ctx.getResourceResolver(), path, data);
        } catch (Exception e) {
            LOG.error("Error occured in creation {}", e);
            throw e;
        }

    }

    private String currentName(String path) {
        LOG.info("CURRENT NAME  {} ", path);
        int index = path.lastIndexOf('/');
        return path.substring(index + 1, path.length());
    }

    private String parentPath(String path) {
        int index = path.lastIndexOf('/');
        return path.substring(0, index);
    }

    @Override
    public void delete(ResolveContext<Object> ctx, Resource resource) throws PersistenceException {
        LOG.info("DELETE  {} ", resource.getName());
        if (!(resource instanceof MVMap)) {
            throw new PersistenceException();
        }
        MVMap<String, Object> map = ((MvResource) resource).getMVMap();
        store.removeMap(map);
        store.commit();
    }

    @Override
    public Iterator<Resource> listChildren(ResolveContext<Object> resolveContext, Resource resource) {
        LOG.info("LIST CHILDREN");
        MVMap<String, String[]> parentResource = store.openMap("_children");
        List<Resource> response = new ArrayList<>();
        String[] childNames = parentResource.getOrDefault(resource.getPath(), new String[] {});
        for (int i = 0; i < childNames.length; ++i) {
            String childPath = resource.getPath() + "/" + childNames[i];
            LOG.info("child found {}", childPath);
            response.add(new MvResource(resolveContext.getResourceResolver(), childPath, store.openMap(childPath)));
        }
        return response.iterator();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        return (AdapterType) ((MvResource) adaptable).getValueMap();
    }

}
