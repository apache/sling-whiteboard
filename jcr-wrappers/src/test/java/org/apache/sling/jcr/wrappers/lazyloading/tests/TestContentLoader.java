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
package org.apache.sling.jcr.wrappers.lazyloading.tests;

import org.apache.sling.jcr.wrappers.lazyloading.impl.ContentLoader;
import org.apache.sling.jcr.wrappers.SessionWrapper;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import org.apache.jackrabbit.oak.commons.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TestContentLoader implements ContentLoader {

    private static final Logger log = LoggerFactory.getLogger(TestContentLoader.class);
    
    private final Set<String> contentPathsLoaded = new HashSet<>();
    
    // We need to find out under which paths we have content, to be able
    // to create nodes when a parent is asked for child nodes.
    private static final Set<String> availableContentPaths = getAvailableContentPaths();
    
    /** Our test resources are found under that path */
    public static final String RESOURCE_ROOT = "/test-content";
    
    /** Our test system view export files have this suffix */
    public static final String SYSVIEW_SUFFIX = ".sysview.xml";
    
    /** We need to know which paths have content, in order to find out 
     *  which intermediate nodes need to be created when walking down the 
     *  content tree.
     */
    private static Set<String> getAvailableContentPaths() {
        try {
            final URL contentRootURL = TestContentLoader.class.getResource(RESOURCE_ROOT);
            if("file".equals(contentRootURL.getProtocol())) {
                final Set<String> tmp = new TreeSet<>();
                setContentPaths(Paths.get(contentRootURL.toURI()).toFile(), "", tmp);
                log.debug("Available content paths: {}", tmp);
                return Collections.unmodifiableSet(tmp);
            } else {
                throw new IOException("Expected a file at " + RESOURCE_ROOT + ", got " + contentRootURL);
            }
        } catch(Exception e) {
            log.error("Error getting available content paths", e);
        }
        return null;
    }
    
    /** Set (in allPaths) the paths for which we have sysview files */
    private static void setContentPaths(File rootDir, String basePath, Set<String> allPaths) {
        Stream.of(rootDir.list()).forEach(
                name -> {
                    final File f = new File(rootDir, name);
                    final String childPath = basePath + "/" + name;
                    if(f.isDirectory()) {
                        setContentPaths(f, childPath, allPaths);
                    } else if(f.getName().endsWith(SYSVIEW_SUFFIX)) {
                        addPaths(allPaths, childPath);
                    }
                }
        );
    }
    
    /** Add all the paths from newPath to allPaths */
    private static void addPaths(Set<String> allPaths, String newPath) {
        allPaths.add(newPath);
        final String parent = PathUtils.getParentPath(newPath);
        if(parent.length() > 1) {
            addPaths(allPaths, parent);
        }
    }
    
    private Node getOrCreateNodeAndAncestors(SessionWrapper s, String path) throws RepositoryException {
        if(s.nodeExists(path)) {
            return s.getNode(path);
        }
        log.debug("Loading or creating node {}", path);
        
        final String nodeName = PathUtils.getName(path);
        final String parentPath = PathUtils.getParentPath(path);
        log.debug("Looking for {}", parentPath);
        final Node parent = getOrCreateNodeAndAncestors(s, parentPath);
        log.debug("Creating {}/{}", parent.getPath(), nodeName);
        return createIntermediateNode(parent, nodeName);
    }
    
    private static String removeSysviewSuffix(String path) {
        if(path.endsWith(SYSVIEW_SUFFIX)) {
            return path.substring(0, path.length() - SYSVIEW_SUFFIX.length());
        }
        return path;
    }
    
    private void loadTestDataIfAvailable(SessionWrapper s, final String path) throws RepositoryException {
        if(contentPathsLoaded.contains(path)) {
            return;
        }
        contentPathsLoaded.add(path);
        try (
            InputStream is = getClass().getResourceAsStream(RESOURCE_ROOT + path + SYSVIEW_SUFFIX);
        ){
            if(is != null) {
                log.debug("Importing {}", path);
                final Node parent = getOrCreateNodeAndAncestors(s, path);
                s.getWorkspace().importXML(parent.getPath(), is, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
                s.refresh(true);
            }
        } catch(IOException ioe) {
            throw new RepositoryException(ioe.getMessage());
        }
        s.save();
    }
    
    /** If we know we have descendants under basePath, load the direct
     *  children of basePath.
     */
    private void loadChildrenIfAvailable(final SessionWrapper s, String basePath) throws RepositoryException {
        
        final List<RepositoryException> exceptions = new ArrayList<>();
        availableContentPaths.stream().forEach(path -> 
            {
                final String parentPath = PathUtils.getParentPath(path);
                if(basePath.equals(parentPath)) {
                    try {
                        if(s.getWrappedSession().nodeExists(path)) {
                            // Do nothing, node already exists
                        } else if(path.endsWith(SYSVIEW_SUFFIX)) {
                            loadTestDataIfAvailable(s, removeSysviewSuffix(path));
                        } else {
                            getOrCreateNodeAndAncestors(s, path);
                        }
                    } catch(RepositoryException e) {
                        exceptions.add(e);
                    }
                }
            }
        );
        
        if(!exceptions.isEmpty()) {
            throw exceptions.get(0);
        }
    }

    @Override
    public Node createIntermediateNode(Node parent, String childName) throws RepositoryException {
        return parent.addNode(childName);
    }
    
    @Override
    public void prepareForGetChildNodes(SessionWrapper s, String parentPath) throws RepositoryException {
        log.debug("loadChildren {}", parentPath);
        loadChildrenIfAvailable(s, parentPath);
    }

    @Override
    public void prepareForGetNode(SessionWrapper s, String path) throws RepositoryException {
        loadTestDataIfAvailable(s, path);
    }

    @Override
    public void prepareForQuery(SessionWrapper s, Query q) throws RepositoryException {
        // Simplistic way of finding out if the query affects content
        // that we have to load. For a real implementation we'll probably
        // need to analyze the Query Object Model in a more precise way.
        if(q.getStatement().contains("/content/")) {
            // Simplistiy way of loading content, we might
            // need a more subtle strategy if that's possible. Maybe query a
            // different back-end index to find out what's actually required
            TestBase.visitRecursively(s.getRootNode(), path -> true, null);
        }
    }
}