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
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
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
import org.h2.mvstore.StreamStore;
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
public class MvStoreResourceProvider extends ResourceProvider<MvSession> implements AdapterFactory {

    private static final Logger LOG = LoggerFactory.getLogger(MvStoreResourceProvider.class);

    private static final String CHILDREN = "_children";

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
    private StreamStore binaryStore;

    @Override
    public void start(ProviderContext ctx) {
        LOG.info("mvprovider has started");
        super.start(ctx);
        store = MVStore.open("dataStore");
        binaryStore = new StreamStore(store.openMap("_binaries"));
    }

    @Override
    public void stop() {
        super.stop();
        store.close();
    }

    @Override
    public Resource getResource(ResolveContext<MvSession> context, String resourcePath, ResourceContext resourceContext,
            Resource parentResource) {
        LOG.info("GET {} ", resourcePath);
        MVMap<String, Object> properties = store.openMap(resourcePath);
        if (resourcePath.equals("/content/apache/fake")) {
            final ResourceProvider rp = context.getParentResourceProvider();
            return rp.getResource(context.getParentResolveContext(), resourcePath, resourceContext, parentResource);
        }
        if (properties.isEmpty()) {
            return null;
        }
        return new MvResource(context.getResourceResolver(), resourcePath, new MvValueMap(properties, binaryStore));
    }

    @Override
    public Resource create(ResolveContext<MvSession> ctx, String path, Map<String, Object> properties)
            throws PersistenceException {
        LOG.info("CREATE  {} ", path);
        MVMap<String, Object> oldProps = store.openMap(path);
        if (oldProps.isEmpty()) {
            String parent = parentPath(path);
            MVMap<String, String[]> parentResource = store.openMap(CHILDREN);
            String[] children = parentResource.getOrDefault(parent, new String[] {});
            String[] newChildren = Arrays.copyOf(children, children.length + 1);
            newChildren[children.length] = path;
            parentResource.put(parent, newChildren);
        }
        MvValueMap data = new MvValueMap(oldProps, binaryStore);
        data.putAll(properties);
        store.commit();
        return new MvResource(ctx.getResourceResolver(), path, data);
    }

    public String currentName(String path) {
        LOG.info("CURRENT NAME  {} ", path);
        int index = path.lastIndexOf('/');
        return path.substring(index + 1, path.length());
    }

    private String parentPath(String path) {
        int index = path.lastIndexOf('/');
        return path.substring(0, index);
    }

    @Override
    public void commit(ResolveContext<MvSession> ctx) throws PersistenceException {
        LOG.info("COMMIT  {} ", ctx.getProviderState());
        store.commit();
    }

    @Override
    public void delete(ResolveContext<MvSession> ctx, Resource resource) throws PersistenceException {
        LOG.info("DELETE  {} ", resource.getName());
        if (!(resource instanceof MvResource)) {
            throw new PersistenceException("can not delete resource of type" + resource.getClass());
        }
        MVMap<String, String[]> parentResource = store.openMap(CHILDREN);
        String parentPath = parentPath(resource.getPath());
        String[] childNames = parentResource.get(parentPath);
        String[] newChildren = new String[childNames.length - 1];
        int newIndex = 0;
        for (int index = 0; index < childNames.length; ++index) {
            if (!childNames[index].equals(resource.getName())) {
                newChildren[newIndex++] = childNames[index];
            }
        }
        parentResource.put(parentPath, newChildren);
        Deque<String> resourceToDelete = new LinkedList<>();
        resourceToDelete.add(resource.getPath());
        deleteDescendents(parentResource, resourceToDelete);
    }

    private void deleteDescendents(MVMap<String, String[]> parentMap, Deque<String> pathsToDelete) {
        if (pathsToDelete.isEmpty()) {
            return;
        }
        String currentPath = pathsToDelete.pop();
        String[] children = parentMap.getOrDefault(currentPath, new String[] {});
        store.removeMap(store.openMap(currentPath));
        for (int i = 0; i < children.length; ++i) {
            pathsToDelete.add(children[i]);
        }
        deleteDescendents(parentMap, pathsToDelete);
    }

    @Override
    public Iterator<Resource> listChildren(ResolveContext<MvSession> resolveContext, Resource resource) {
        LOG.info("LIST CHILDREN");
        MVMap<String, String[]> parentMap = store.openMap(CHILDREN);
        List<Resource> response = new ArrayList<>();
        String[] childNames = parentMap.getOrDefault(resource.getPath(), new String[] {});
        for (int i = 0; i < childNames.length; ++i) {
            String childPath = childNames[i];
            LOG.info("child found {}", childPath);
            response.add(new MvResource(resolveContext.getResourceResolver(), childPath,
                    new MvValueMap(store.openMap(childPath), binaryStore)));
        }
        return response.iterator();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        return (AdapterType) ((MvResource) adaptable).getValueMap();
    }

}
