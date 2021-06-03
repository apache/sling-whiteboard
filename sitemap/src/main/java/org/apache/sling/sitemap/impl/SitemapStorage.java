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
package org.apache.sling.sitemap.impl;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.serviceusermapping.ServiceUserMapped;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.apache.sling.sitemap.impl.SitemapUtil.*;

@Component(service = SitemapStorage.class)
@Designate(ocd = SitemapStorage.Configuration.class)
public class SitemapStorage {

    @ObjectClassDefinition(name = "Apache Sling Sitemap - Storage")
    @interface Configuration {

        @AttributeDefinition(name = "Path", description = "The path under which sitemap files generated in the " +
                "background will be stored.")
        String storagePath() default "/var/sitemaps";

        @AttributeDefinition(name = "Max State Age", description = "The number of milliseconds after which an " +
                "intermediate state will deleted.")
        int stateMaxAge() default 60 * 30 * 1000;
    }

    static final String PN_SITEMAP_ENTRIES = "entries";
    static final String PN_SITEMAP_SIZE = "size";

    private static final Logger LOG = LoggerFactory.getLogger(SitemapStorage.class);
    private static final Map<String, Object> AUTH = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE,
            "sitemap-writer");
    private static final String STATE_EXTENSION = ".part";
    private static final String XML_EXTENSION = ".xml";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    @Reference(target = "(subServiceName=sitemap-writer)")
    private ServiceUserMapped serviceUserMapped;

    private String rootPath = "/var/sitemaps";
    private int maxStateAge = Integer.MAX_VALUE;

    @Activate
    protected void activate(Configuration configuration) {
        rootPath = configuration.storagePath();
        maxStateAge = configuration.stateMaxAge();
    }

    @NotNull
    public ValueMap getState(@NotNull Resource sitemapRoot, @NotNull String name) throws IOException {
        String statePath = getSitemapFilePath(sitemapRoot, name) + STATE_EXTENSION;
        try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(AUTH)) {
            Resource state = resolver.getResource(statePath);
            // make a copy to read properties fully
            if (state == null) {
                return ValueMap.EMPTY;
            }
            ValueMap stateProperties = state.getValueMap();
            Calendar lastModified = stateProperties.get(JcrConstants.JCR_LASTMODIFIED, Calendar.class);
            if (lastModified != null) {
                // advance lastModified by maxStateAge to get the point in time the state would expire
                lastModified.add(Calendar.MILLISECOND, maxStateAge);
                // check if the expire time is in the future, if so return the state, if not fallback to an empty state
                if (lastModified.after(Calendar.getInstance())) {
                    return new ValueMapDecorator(new HashMap<>(state.getValueMap()));
                } else if (LOG.isDebugEnabled()) {
                    LOG.debug("State at {} expired at {}", statePath, lastModified.getTime().toGMTString());
                }
            }
            return ValueMap.EMPTY;
        } catch (LoginException ex) {
            throw new IOException("Cannot read state at " + statePath, ex);
        }
    }

    void writeState(@NotNull Resource sitemapRoot, @NotNull String name, @NotNull Map<String, Object> state)
            throws IOException {
        String statePath = getSitemapFilePath(sitemapRoot, name) + STATE_EXTENSION;
        try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(AUTH)) {
            Resource folder = getOrCreateFolder(resolver, ResourceUtil.getParent(statePath));
            String stateName = ResourceUtil.getName(statePath);
            Resource stateResource = folder.getChild(stateName);
            if (stateResource != null) {
                ModifiableValueMap properties = stateResource.adaptTo(ModifiableValueMap.class);
                if (properties == null) {
                    throw new IOException("Cannot modify properties of existing state: " + statePath);
                }
                properties.putAll(state);
                properties.put(JcrConstants.JCR_LASTMODIFIED, Calendar.getInstance());
            } else {
                Map<String, Object> properties = new HashMap<>(state.size() + 1);
                properties.putAll(state);
                properties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                properties.put(JcrConstants.JCR_LASTMODIFIED, Calendar.getInstance());
                resolver.create(folder, stateName, properties);
            }
            resolver.commit();
        } catch (LoginException | PersistenceException ex) {
            throw new IOException("Cannot create state at " + statePath, ex);
        }
    }

    void removeState(@NotNull Resource sitemapRoot, @NotNull String name) throws IOException {
        String statePath = getSitemapFilePath(sitemapRoot, name) + STATE_EXTENSION;
        try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(AUTH)) {
            Resource stateResource = resolver.getResource(statePath);
            if (stateResource != null) {
                resolver.delete(stateResource);
                resolver.commit();
            }
        } catch (LoginException | PersistenceException ex) {
            throw new IOException("Cannot create state at " + statePath, ex);
        }
    }

    String writeSitemap(@NotNull Resource sitemapRoot, @NotNull String name, @NotNull InputStream data, int size,
                        int entries) throws IOException {
        String sitemapFilePath = getSitemapFilePath(sitemapRoot, name);
        String statePath = sitemapFilePath + STATE_EXTENSION;
        sitemapFilePath = sitemapFilePath + XML_EXTENSION;
        try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(AUTH)) {
            String sitemapFileName = ResourceUtil.getName(sitemapFilePath);
            Resource folder = getOrCreateFolder(resolver, ResourceUtil.getParent(sitemapFilePath));

            Resource sitemapResource = folder.getChild(sitemapFileName);

            if (sitemapResource == null) {
                Map<String, Object> properties = new HashMap<>(3);
                properties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
                properties.put(PN_SITEMAP_ENTRIES, entries);
                properties.put(PN_SITEMAP_SIZE, size);
                sitemapResource = resolver.create(folder, sitemapFileName, properties);
            } else {
                ModifiableValueMap properties = sitemapResource.adaptTo(ModifiableValueMap.class);
                if (properties == null) {
                    throw new IOException("Cannot overwrite existing sitemap at: " + sitemapFilePath);
                }
                properties.put(PN_SITEMAP_ENTRIES, entries);
                properties.put(PN_SITEMAP_SIZE, size);
            }

            Resource sitemapContent = sitemapResource.getChild(JcrConstants.JCR_CONTENT);
            if (sitemapContent == null) {
                Map<String, Object> properties = new HashMap<>();
                properties.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_RESOURCE);
                properties.put(JcrConstants.JCR_ENCODING, "UTF-8");
                properties.put(JcrConstants.JCR_MIMETYPE, "application/xml");
                properties.put(JcrConstants.JCR_LASTMODIFIED, Calendar.getInstance());
                properties.put(JcrConstants.JCR_DATA, data);
                resolver.create(sitemapResource, JcrConstants.JCR_CONTENT, properties);
            } else {
                ModifiableValueMap properties = sitemapContent.adaptTo(ModifiableValueMap.class);
                if (properties == null) {
                    throw new IOException("Cannot overwrite existing sitemap at: " + sitemapFilePath);
                }
                properties.put(JcrConstants.JCR_LASTMODIFIED, Calendar.getInstance());
                properties.put(JcrConstants.JCR_DATA, data);
            }

            Resource stateResource = resolver.getResource(statePath);
            if (stateResource != null) {
                resolver.delete(stateResource);
            }

            resolver.commit();
        } catch (LoginException | PersistenceException ex) {
            throw new IOException("Cannot create sitemap at " + sitemapFilePath, ex);
        }

        return sitemapFilePath;
    }

    Set<SitemapStorageInfo> getSitemaps(Resource sitemapRoot) {
        return getSitemaps(sitemapRoot, Collections.emptySet());
    }

    /**
     * Returns an info object of all sitemaps for the given sitemap root.
     * <p>
     * When the sitemap root is not a top level sitemap root then only the sitemaps corresponding to the given names
     * are returned. If the sitemap is a top level sitemap root and no names are passed, an info for all sitemaps will
     * be returned.
     *
     * @param sitemapRoot
     * @param names
     * @return
     */
    Set<SitemapStorageInfo> getSitemaps(Resource sitemapRoot, Collection<String> names) {
        Resource topLevelSitemapRoot = getTopLevelSitemapRoot(sitemapRoot);
        Predicate<SitemapStorageInfo> filter;

        if (!isTopLevelSitemapRoot(sitemapRoot) || names.size() > 0) {
            // return only those that match at least on of the names requested
            filter = info -> names.stream()
                    .map(name -> getSitemapSelector(sitemapRoot, topLevelSitemapRoot, name))
                    .anyMatch(selector -> info.getSitemapSelector().equals(selector));
        } else {
            filter = any -> true;
        }

        String storagePath = rootPath + topLevelSitemapRoot.getPath();
        try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(AUTH)) {
            Resource storageResource = resolver.getResource(storagePath);
            if (storageResource == null) {
                LOG.debug("Resource at {} does not exist.", storagePath);
                return Collections.emptySet();
            }
            return StreamSupport.stream(storageResource.getChildren().spliterator(), false)
                    .filter(child -> child.getName().endsWith(XML_EXTENSION))
                    .map(child -> new SitemapStorageInfo(
                            child.getPath(),
                            child.getName().substring(0, child.getName().lastIndexOf('.')),
                            Optional.ofNullable(child.getChild(JcrConstants.JCR_CONTENT))
                                    .map(Resource::getValueMap)
                                    .map(content -> content.get(JcrConstants.JCR_LASTMODIFIED, Calendar.class))
                                    .orElse(null),
                            child.getValueMap().get(PN_SITEMAP_SIZE, 0),
                            child.getValueMap().get(PN_SITEMAP_ENTRIES, 0)))
                    .filter(filter)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (LoginException ex) {
            LOG.warn("Could not list sitemaps from storage: {}", ex.getMessage());
            return Collections.emptySet();
        }
    }

    boolean copySitemap(Resource sitemapRoot, String sitemapSelector, OutputStream output) throws IOException {
        if (!isTopLevelSitemapRoot(sitemapRoot)) {
            return false;
        }
        String sitemapFilePath = rootPath + sitemapRoot.getPath() + '/' + sitemapSelector + XML_EXTENSION;
        try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(AUTH)) {
            Resource sitemap = resolver.getResource(sitemapFilePath);
            InputStream data = sitemap != null ? sitemap.adaptTo(InputStream.class) : null;

            if (data != null) {
                IOUtils.copyLarge(data, output);
                return true;
            } else {
                LOG.debug("Could not copy data from resource: {}", sitemapFilePath);
                return false;
            }
        } catch (LoginException ex) {
            LOG.warn("Could not copy sitemap to output: {}", ex.getMessage());
            return false;
        }
    }

    @NotNull
    private String getSitemapFilePath(@NotNull Resource sitemapRoot, @NotNull String name) {
        Resource topLevelSitemapRoot = getTopLevelSitemapRoot(sitemapRoot);
        return rootPath + topLevelSitemapRoot.getPath() + '/' + getSitemapSelector(sitemapRoot, topLevelSitemapRoot, name);
    }

    /**
     * Creates the given path and its ancestors using sling:Folder jcr:primaryType for all.
     *
     * @param resolver
     * @param path
     * @return
     * @throws PersistenceException
     */
    private static Resource getOrCreateFolder(@NotNull ResourceResolver resolver, @NotNull String path)
            throws PersistenceException {
        Resource folder = resolver.getResource(path);
        if (folder == null) {
            String parentPath = ResourceUtil.getParent(path);

            if (parentPath == null) {
                throw new PersistenceException("Cannot create parent path of " + path);
            }

            Resource parent = getOrCreateFolder(resolver, parentPath);
            folder = resolver.create(parent, ResourceUtil.getName(path), Collections.singletonMap(
                    JcrConstants.JCR_PRIMARYTYPE, "sling:Folder"
            ));
        }
        return folder;
    }
}
