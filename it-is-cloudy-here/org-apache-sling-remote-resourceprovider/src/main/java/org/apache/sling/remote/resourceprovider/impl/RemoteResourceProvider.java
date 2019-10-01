/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.remote.resourceprovider.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.threads.ThreadPool;
import org.apache.sling.commons.threads.ThreadPoolManager;
import org.apache.sling.contentparser.api.ContentParser;
import org.apache.sling.contentparser.api.ParserOptions;
import org.apache.sling.contentparser.json.JSONParserOptions;
import org.apache.sling.remote.resourceprovider.Directory;
import org.apache.sling.remote.resourceprovider.File;
import org.apache.sling.remote.resourceprovider.RemoteResourceEvent;
import org.apache.sling.remote.resourceprovider.RemoteResourceEventHandler;
import org.apache.sling.remote.resourceprovider.RemoteResourceEventType;
import org.apache.sling.remote.resourceprovider.RemoteResourceReference;
import org.apache.sling.remote.resourceprovider.RemoteStorageProvider;
import org.apache.sling.remote.resourceprovider.impl.paths.ShallowReference;
import org.apache.sling.remote.resourceprovider.impl.paths.ShallowReferenceTree;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.sling.remote.resourceprovider.RemoteStorageProvider.SLING_META_FILE;

public final class RemoteResourceProvider extends ResourceProvider<RemoteResourceProviderContext> implements RemoteResourceEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteResourceProvider.class);
    private static final String ANY = "";
    private final ThreadPoolManager threadPoolManager;
    private final ThreadPool threadPool;
    private final ContentParser jsonParser;
    private final InMemoryResourceCache cache;
    private final ShallowReferenceTree tree;
    private final RemoteStorageProvider remoteStorageProvider;
    private final boolean requiresAuthentication;
    private final Map<String, Set<String>> accessMappings;
    private final Map<String, Set<String>> negativeHits;

    private static final ParserOptions JSON_PARSER_OPTIONS = new JSONParserOptions().detectCalendarValues(true).defaultPrimaryType(null);
    private static final Map<String, Object> FILE_RESOURCE_PROPERTIES = new HashMap<>();
    static {
        FILE_RESOURCE_PROPERTIES.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, "nt:file");
    }

    RemoteResourceProvider(ThreadPoolManager threadPoolManager, ContentParser jsonParser, InMemoryResourceCache cache,
                           RemoteStorageProvider remoteStorageProvider,
                           boolean requiresAuthentication) {
        this.threadPoolManager = threadPoolManager;
        this.threadPool = threadPoolManager.get(remoteStorageProvider.getClass().getName() + "-" + System.currentTimeMillis());
        this.jsonParser = jsonParser;
        this.cache = cache;
        negativeHits = new ConcurrentHashMap<>();
        tree = new ShallowReferenceTree(removed -> {
            for (String resourceRemoved : removed.getProvidedResourcePaths()) {
                this.cache.remove(resourceRemoved);
                if (!requiresAuthentication) {
                    this.cache.remove(ResourceUtil.getParent(resourceRemoved));
                }
            }
            String slingPath = remoteStorageProvider.slingPath(removed.getPath());
            int lastSlash = slingPath.lastIndexOf('/');
            if (lastSlash > 0) {
                String fileName = slingPath.substring(lastSlash + 1);
                if (SLING_META_FILE.equals(fileName)) {
                    slingPath = ResourceUtil.getParent(slingPath);
                }
            }
            for (String negativeHit : negativeHits.keySet()) {
                if (negativeHit.startsWith(slingPath + "/")) {
                    negativeHits.remove(negativeHit);
                }
            }
        });
        this.remoteStorageProvider = remoteStorageProvider;
        this.remoteStorageProvider.registerEventHandler(this);
        this.requiresAuthentication = requiresAuthentication;
        accessMappings = new ConcurrentHashMap<>();

    }

    void cleanup() {
        if (threadPool != null) {
            threadPoolManager.release(threadPool);
        }
        cache.clear();
        accessMappings.clear();
        negativeHits.clear();
    }

    @Override
    public void handleEvent(RemoteResourceEvent event) {
        threadPool.submit(() -> {
            /*
             * we don't care if the remote resource was changed or deleted; the affected resources from the cached view have to be
             * changed anyways, so we just remove all remote resources from the shallow tree
             */
            for (String referencePath : event.getPaths()) {
                tree.remove(referencePath);
            }
        });
    }

    @Override
    public RemoteResourceProviderContext authenticate(@NotNull Map<String, Object> authenticationInfo) {
        return new RemoteResourceProviderContext(authenticationInfo);
    }

    @Override
    public @Nullable Resource getResource(@NotNull ResolveContext<RemoteResourceProviderContext> ctx, @NotNull String path,
                                          @NotNull ResourceContext resourceContext, @Nullable Resource parent) {
        RemoteResourceProviderContext context = ctx.getProviderState();
        Map<String, Object> authenticationInfo = context == null ? Collections.emptyMap() :
                ctx.getProviderState().getAuthenticationInfo();
        CacheableResource cacheableResource = getOrBuildResource(path, authenticationInfo);
        if (cacheableResource != null) {
            return new CacheableResourceWrapper(ctx, cacheableResource);
        }
        return null;
    }

    @Override
    public @Nullable Iterator<Resource> listChildren(@NotNull ResolveContext<RemoteResourceProviderContext> ctx, @NotNull Resource parent) {
        RemoteResourceProviderContext context = ctx.getProviderState();
        Map<String, Object> authenticationInfo = context == null ? Collections.emptyMap() :
                ctx.getProviderState().getAuthenticationInfo();
        String user = extractUser(authenticationInfo);
        if (requiresAuthentication && user == null) {
            return null;
        }
        CacheableResource _parent = getOrBuildResource(parent.getPath(), authenticationInfo);
        if (_parent != null) {
            if (_parent.getChildrenSet() == null) {
                RemoteResourceReference remoteResourceReference = _parent.getRemoteResourceReference();
                if (remoteResourceReference.getType() == RemoteResourceReference.Type.FILE) {
                    if (SLING_META_FILE.equals(remoteResourceReference.getName())) {
                        final LinkedHashSet<CacheableResource> children = new LinkedHashSet<>();
                        try {
                            String storagePath = _parent.getRemoteResourceReference().getPath();
                            String storageParentPath = ResourceUtil.getParent(storagePath);
                            if (storageParentPath != null) {
                                File file = remoteStorageProvider.getFile(remoteResourceReference, authenticationInfo);
                                if (file == null) {
                                    throw new IOException(String.format("Cannot retrieve file %s.", remoteResourceReference.getPath()));
                                }
                                jsonParser.parse(
                                        (path, properties) -> {
                                            String slingParentPath = remoteStorageProvider.slingPath(storageParentPath);
                                            if (slingParentPath != null) {
                                                String slingPath = ResourceUtil.normalize(slingParentPath + path);
                                                if (!"/".equals(path) && slingPath != null &&
                                                        slingPath.startsWith(parent.getPath() + "/")) {
                                                    CacheableResource child = queryCaches(slingPath, user);
                                                    if (child == null) {
                                                        child = new CacheableResource(remoteStorageProvider, remoteResourceReference,
                                                                slingPath, properties);
                                                        populateCaches(child, user);
                                                    }
                                                    children.add(child);

                                                }
                                            }
                                        },
                                        file.getInputStream(),
                                        JSON_PARSER_OPTIONS);
                            }
                        } catch (IOException e) {
                            LOGGER.error(String.format("Unable to parse file %s provided by %s.", remoteResourceReference.getPath(),
                                    remoteResourceReference.getProvider()), e);
                        }
                        _parent.setChildren(children);
                    } else {
                        _parent.setChildren(new LinkedHashSet<>(Collections.emptySet()));
                    }
                } else if (remoteResourceReference.getType() == RemoteResourceReference.Type.DIRECTORY) {
                    LinkedHashSet<CacheableResource> children = new LinkedHashSet<>();
                    Directory directory = remoteStorageProvider.getDirectory(remoteResourceReference, authenticationInfo);
                    if (directory != null) {
                        for (RemoteResourceReference child : directory.getChildren()) {
                            String slingPath = remoteStorageProvider.slingPath(child.getPath());
                            if (slingPath != null) {
                                if (child.getType() == RemoteResourceReference.Type.FILE) {
                                    if (SLING_META_FILE.equals(child.getName())) {
                                        try {
                                            File file = remoteStorageProvider.getFile(child, authenticationInfo);
                                            if (file != null) {
                                                jsonParser.parse((String path, Map<String, Object> properties) -> {
                                                            if (path.length() > 1 && !path.substring(1).contains("/")) {
                                                                String childSlingPath =
                                                                        ResourceUtil.normalize(ResourceUtil.getParent(slingPath) + path);
                                                                CacheableResource resource = queryCaches(slingPath, user);
                                                                if (resource == null) {
                                                                    resource = new CacheableResource(remoteStorageProvider, child,
                                                                            childSlingPath, properties);
                                                                    populateCaches(resource, user);
                                                                }
                                                                children.add(resource);

                                                            }
                                                        },
                                                        file.getInputStream(),
                                                        JSON_PARSER_OPTIONS);
                                            }
                                        } catch (IOException e) {
                                            LOGGER.error("Unable to parse file " + child.getPath(), e);
                                        }
                                    } else {
                                        CacheableResource resource = queryCaches(slingPath, user);
                                        if (resource == null) {
                                            File file = remoteStorageProvider.getFile(child, authenticationInfo);
                                            if (file != null) {
                                                resource = buildResource(slingPath, file);
                                                populateCaches(resource, user);
                                            }
                                        }
                                        if (resource != null) {
                                            children.add(resource);
                                        }
                                    }
                                } else if (child.getType() == RemoteResourceReference.Type.DIRECTORY) {
                                    CacheableResource resource = queryCaches(slingPath, user);
                                    if (resource == null) {
                                        Directory d = remoteStorageProvider.getDirectory(child, authenticationInfo);
                                        if (d != null) {
                                            resource = buildResource(authenticationInfo, slingPath, child, d);
                                            populateCaches(resource, user);
                                        }
                                    }
                                    children.add(resource);
                                }
                            }
                        }
                    }
                    if (!requiresAuthentication) {
                        _parent.setChildren(children);
                    }
                    return getCacheableResourceWrapperIterator(ctx, children);
                }
            }
            return getCacheableResourceWrapperIterator(ctx, _parent.getChildrenSet());
        }
        return null;
    }

    @Nullable
    private Iterator<Resource> getCacheableResourceWrapperIterator(@NotNull ResolveContext<RemoteResourceProviderContext> ctx,
                                                                   Set<CacheableResource> children) {
        if (children != null && !children.isEmpty()) {
            Iterator<CacheableResource> iterator = children.iterator();
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public Resource next() {
                    return new CacheableResourceWrapper(ctx, iterator.next());
                }
            };
        }
        return null;
    }

    @Nullable
    private CacheableResource getOrBuildResource(@NotNull String slingPath, @NotNull Map<String, Object> authenticationInfo) {
        String user = extractUser(authenticationInfo);
        if (requiresAuthentication && user == null) {
            return null;
        }
        if (isNegativeHit(slingPath, user)) {
            return null;
        }
        CacheableResource cacheableResource = queryCaches(slingPath, user);
        if (cacheableResource == null) {
            String storagePath = remoteStorageProvider.storagePath(slingPath);
            RemoteResourceReference resource = remoteStorageProvider.findResource(slingPath, authenticationInfo);
            if (resource != null) {
                if (storagePath.equals(resource.getPath())) {
                    if (resource.getType() == RemoteResourceReference.Type.FILE) {
                        cacheableResource = buildResource(slingPath, resource);
                    } else if (resource.getType() == RemoteResourceReference.Type.DIRECTORY) {
                        Directory directory = remoteStorageProvider.getDirectory(resource, authenticationInfo);
                        if (directory != null) {
                            cacheableResource = buildResource(authenticationInfo, slingPath, resource, directory);
                        }
                    }
                } else if (resource.getType() == RemoteResourceReference.Type.DIRECTORY) {
                    Directory directory = remoteStorageProvider.getDirectory(resource, authenticationInfo);
                    if (directory != null && storagePath.startsWith(resource.getPath())) {
                        String relativePath = storagePath.substring(resource.getPath().length());
                        for (RemoteResourceReference r : directory.getChildren()) {
                            if (SLING_META_FILE.equals(r.getName())) {
                                File metaFile = remoteStorageProvider.getFile(r, authenticationInfo);
                                if (metaFile != null) {
                                    AtomicReference<CacheableResource> resourceReference = new AtomicReference<>();
                                    try {
                                        jsonParser.parse((String path, Map<String, Object> properties) -> {
                                                    if (relativePath.equals(path)) {
                                                        resourceReference
                                                                .set(new CacheableResource(remoteStorageProvider, r, slingPath,
                                                                        properties));
                                                    }
                                                },
                                                metaFile.getInputStream(),
                                                JSON_PARSER_OPTIONS);
                                        cacheableResource = resourceReference.get();
                                    } catch (IOException e) {
                                        LOGGER.error("Unable to parse file " + metaFile.getPath(), e);
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
                if (cacheableResource != null) {
                    populateCaches(cacheableResource, user);
                } else {
                    markNegativeHit(slingPath, user);
                }
            } else {
                markNegativeHit(slingPath, user);
            }
        }
        if (requiresAuthentication) {
            if (accessMappings.containsKey(slingPath) &&
                    accessMappings.get(slingPath).contains(user)) {
                return cacheableResource;
            }
            return null;
        } else {
            return cacheableResource;
        }
    }

    @NotNull
    private CacheableResource buildResource(@NotNull String path, @NotNull RemoteResourceReference reference) {
        return new CacheableResource(remoteStorageProvider, reference, path, FILE_RESOURCE_PROPERTIES);
    }

    @NotNull
    private CacheableResource buildResource(@NotNull Map<String, Object> authenticationInfo,
                                            @NotNull String path,
                                            @NotNull RemoteResourceReference reference,
                                            @NotNull Directory directory) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ResourceResolver.PROPERTY_RESOURCE_TYPE, "sling:Folder");
        for (RemoteResourceReference remoteResourceReference : directory.getChildren()) {
            if (remoteResourceReference.getType() == RemoteResourceReference.Type.FILE &&
                    SLING_META_FILE.equals(remoteResourceReference.getName())) {
                try {
                    File file = remoteStorageProvider.getFile(remoteResourceReference, authenticationInfo);
                    if (file != null) {
                        jsonParser.parse(
                                (String parsedPath, Map<String, Object> propertiesMap) -> {
                                    if ("/".equals(parsedPath)) {
                                        propertiesMap.forEach(properties::put);
                                    }
                                },
                                file.getInputStream(),
                                JSON_PARSER_OPTIONS
                        );
                        // special case - a meta-file augments the directory's properties
                        ShallowReference shallowReference = tree.getReference(remoteResourceReference.getPath());
                        if (shallowReference == null) {
                            shallowReference = new ShallowReference(remoteResourceReference.getPath());
                            tree.add(shallowReference);
                        }
                        shallowReference.markProvidedResource(path);
                    }
                } catch (IOException e) {
                    LOGGER.error(String.format("Unable to parse file %s.", remoteResourceReference.getPath()), e);
                }
                break;
            }
        }
        return new CacheableResource(remoteStorageProvider, reference, path, properties);
    }

    private CacheableResource queryCaches(@NotNull String slingPath, @NotNull String user) {
        if (requiresAuthentication && ANY.equals(user)) {
            throw new IllegalStateException(String.format("Cannot determine user for RemoteStorageProvider %s requiring " +
                    "authentication.", remoteStorageProvider));
        }
        Set<String> allowed = accessMappings.get(slingPath);
        if (allowed != null && allowed.contains(user)) {
            return cache.get(slingPath);
        }
        return null;
    }

    private void populateCaches(@NotNull CacheableResource cacheableResource, @NotNull String user) {
        CacheableResource previous = cache.get(cacheableResource.getPath());
        if (!cacheableResource.equals(previous)) {
            cache.put(cacheableResource.getPath(), cacheableResource);
            RemoteResourceReference reference = cacheableResource.getRemoteResourceReference();
            ShallowReference shallowReference = tree.getReference(reference.getPath());
            if (shallowReference == null) {
                shallowReference = new ShallowReference(reference.getPath());
                tree.add(shallowReference);
            }
            shallowReference.markProvidedResource(cacheableResource.getPath());
        }
        if (requiresAuthentication && ANY.equals(user)) {
            throw new IllegalStateException(String.format("Cannot determine user for RemoteStorageProvider %s requiring " +
                    "authentication.", remoteStorageProvider));
        }
        Set<String> allowed = accessMappings.computeIfAbsent(cacheableResource.getPath(), key -> new HashSet<>());
        allowed.add(user);
    }

    private boolean isNegativeHit(@NotNull String slingPath, @NotNull String user) {
        Set<String> negativeHitsSet = negativeHits.get(slingPath);
        return negativeHitsSet != null && negativeHitsSet.contains(user);
    }

    private void markNegativeHit(@NotNull String slingPath, @NotNull String user) {
        Set<String> negativeHitsSet = negativeHits.computeIfAbsent(slingPath, key -> new HashSet<>());
        negativeHitsSet.add(user);
    }

    private String extractUser(@NotNull Map<String, Object> authenticationInfo) {
        if (requiresAuthentication) {
            return (String) authenticationInfo.get(ResourceResolverFactory.USER);
        }
        return ANY;
    }



}
