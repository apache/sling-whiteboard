/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.feature.io;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.io.spi.ArtifactProvider;
import org.apache.sling.feature.io.spi.ArtifactProviderContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * The artifact manager is the central service to get artifacts.
 * It uses {@link ArtifactProvider}s to get artifacts. The
 * providers are loaded using the service loader.
 */
public class ArtifactManager {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** The map of providers. */
    private final Map<String, ArtifactProvider> providers;

    /** The configuration */
    private final ArtifactManagerConfig config;

    /**
     * Get an artifact manager based on the configuration
     * @param config The configuration
     * @return The artifact manager
     * @throws IOException If the manager can't be initialized
     */
    public static ArtifactManager getArtifactManager(final ArtifactManagerConfig config) throws IOException {
        final ServiceLoader<ArtifactProvider> loader = ServiceLoader.load(ArtifactProvider.class);
        final Map<String, ArtifactProvider> providers = new HashMap<>();
        for(final ArtifactProvider provider : loader) {
            providers.put(provider.getProtocol(), provider);
        }

        final String[] repositoryURLs = new String[config.getRepositoryUrls().length];
        int index = 0;
        for(final String urlString : config.getRepositoryUrls()) {
            repositoryURLs[index] = urlString;
            index++;
        }
        // default
        if ( !providers.containsKey("*") ) {
            providers.put("*", new DefaultArtifactHandler());
        }

        return new ArtifactManager(config, providers);
    }

    ArtifactManager(final ArtifactManagerConfig config, final Map<String, ArtifactProvider> providers)
    throws IOException {
        this.config = config;
        this.providers = providers;
        try {
            for(final ArtifactProvider provider : this.providers.values()) {
                provider.init(config);
            }
        } catch ( final IOException io) {
            shutdown();
            throw io;
        }
    }

    /**
     * Shutdown the artifact manager.
     */
    public void shutdown() {
        for(final ArtifactProvider provider : this.providers.values()) {
            provider.shutdown();
        }
        this.providers.clear();
    }

    private final File getArtifactFromProviders(final String url, final String relativeCachePath) throws IOException {
        final int pos = url.indexOf(":");
        final String scheme = url.substring(0, pos);

        ArtifactProvider provider = this.providers.get(scheme);
        if ( provider == null ) {
            provider = this.providers.get("*");
        }
        if ( provider == null ) {
            throw new IOException("No URL provider found for " + url);
        }
        return provider.getArtifact(url, relativeCachePath);
    }

    /**
     * Get the full artifact url and file for an artifact.
     * @param url Artifact url or relative path.
     * @return Absolute url and file in the form of a handler.
     * @throws IOException If something goes wrong.
     */
    public ArtifactHandler getArtifactHandler(final String url) throws IOException {
        logger.debug("Trying to get artifact for {}", url);

        final String path;

        if ( url.startsWith("mvn:") ) {
            // mvn url
            path = ArtifactId.fromMvnUrl(url).toMvnPath();

        } else if ( url.startsWith(":") ) {
            // repository path
            path = url.substring(1);

        } else if ( url.indexOf(":/") > 0 ) {

            // absolute URL
            int pos = url.indexOf(":/") + 2;
            while ( url.charAt(pos) == '/') {
                pos++;
            }
            final File file = this.getArtifactFromProviders(url, url.substring(pos));
            if ( file == null || !file.exists()) {
                throw new IOException("Artifact " + url + " not found.");
            }
            return new ArtifactHandler(url, file);

        } else {
            // file (either relative or absolute)
            final File f = new File(url);
            if ( !f.exists()) {
                throw new IOException("Artifact " + url + " not found.");
            }
            return new ArtifactHandler(f.toURI().toString(), f);
        }
        logger.debug("Querying repositories for {}", path);

        for(final String repoUrl : this.config.getRepositoryUrls()) {
            final StringBuilder builder = new StringBuilder();
            builder.append(repoUrl);
            builder.append('/');
            builder.append(path);

            final String artifactUrl = builder.toString();
            final int pos = artifactUrl.indexOf(":");
            final String scheme = artifactUrl.substring(0, pos);

            ArtifactProvider handler = this.providers.get(scheme);
            if ( handler == null ) {
                handler = this.providers.get("*");
            }
            if ( handler == null ) {
                throw new IOException("No URL handler found for " + artifactUrl);
            }

            logger.debug("Checking {} to get artifact from {}", handler, artifactUrl);

            final File file = handler.getArtifact(artifactUrl, path);
            if ( file != null ) {
                logger.debug("Found artifact {}", artifactUrl);
                return new ArtifactHandler(artifactUrl, file);
            }

            // check for SNAPSHOT
            final int lastSlash = artifactUrl.lastIndexOf('/');
            final int startSnapshot = artifactUrl.indexOf("-SNAPSHOT", lastSlash + 1);

            if ( startSnapshot > -1 ) {
                // special snapshot handling
                final String metadataUrl = artifactUrl.substring(0, lastSlash) + "/maven-metadata.xml";
                try {
                    final ArtifactHandler metadataHandler = this.getArtifactHandler(metadataUrl);

                    final String contents = getFileContents(metadataHandler);

                    final String latestVersion = getLatestSnapshot(contents);
                    if ( latestVersion != null ) {
                        final String name = artifactUrl.substring(lastSlash); // includes slash
                        final String fullURL = artifactUrl.substring(0, lastSlash) + name.replace("SNAPSHOT", latestVersion);
                        int pos2 = fullURL.indexOf(":/") + 2;
                        while ( fullURL.charAt(pos2) == '/') {
                            pos2++;
                        }
                        final File file2 = this.getArtifactFromProviders(fullURL, path);
                        if ( file2 == null || !file2.exists()) {
                            throw new IOException("Artifact " + fullURL + " not found.");
                        }
                        return new ArtifactHandler(artifactUrl, file2);
                    }
                } catch ( final IOException ignore ) {
                    // we ignore this but report the original 404
                }
            }
        }

        throw new IOException("Artifact " + url + " not found in any repository.");
    }

    protected String getFileContents(final ArtifactHandler handler) throws IOException {
        final StringBuilder sb = new StringBuilder();
        for(final String line : Files.readAllLines(handler.getFile().toPath())) {
            sb.append(line).append('\n');
        }

        return sb.toString();
    }

    public static String getValue(final String xml, final String[] xpath) {
        String value = null;
        int pos = 0;
        for(final String name : xpath) {
            final String element = '<' + name + '>';

            pos = xml.indexOf(element, pos);
            if ( pos == -1 ) {
                final String elementWithAttributes = '<' + name + ' ';
                pos = xml.indexOf(elementWithAttributes, pos);
                if ( pos == -1 ) {
                    break;
                }
            }
            pos = xml.indexOf('>', pos) + 1;
        }
        if ( pos != -1 ) {
            final int endPos = xml.indexOf("</", pos);
            if ( endPos != -1 ) {
                value = xml.substring(pos, endPos).trim();
            }
        }
        return value;
    }
    public static String getLatestSnapshot(final String mavenMetadata) {
        final String timestamp = getValue(mavenMetadata, new String[] {"metadata", "versioning", "snapshot", "timestamp"});
        final String buildNumber = getValue(mavenMetadata, new String[] {"metadata", "versioning", "snapshot", "buildNumber"});

        if ( timestamp != null && buildNumber != null ) {
            return timestamp + '-' + buildNumber;
        }

        return null;
    }

    private static final class DefaultArtifactHandler implements ArtifactProvider {

        private final Logger logger = LoggerFactory.getLogger(this.getClass());

        private volatile File cacheDir;

        private volatile ArtifactProviderContext config;

        @Override
        public String getProtocol() {
            return "*";
        }

        @Override
        public void init(final ArtifactProviderContext config) throws IOException {
            this.cacheDir = config.getCacheDirectory();
            this.config = config;
        }

        @Override
        public void shutdown() {
            this.config = null;
            this.cacheDir = null;
        }

        @Override
        public File getArtifact(final String url, final String relativeCachePath) {
            logger.debug("Checking url to be local file {}", url);
            // check if this is already a local file
            try {
                final File f = new File(new URL(url).toURI());
                if ( f.exists() ) {
                    this.config.incLocalArtifacts();
                    return f;
                }
                return null;
            } catch ( final URISyntaxException ise) {
                // ignore
            } catch ( final IllegalArgumentException iae) {
                // ignore
            } catch ( final MalformedURLException mue) {
                // ignore
            }
            logger.debug("Checking remote url {}", url);
            try {
                // check for url
                if ( url.indexOf(":") == -1 ) {
                    return null;
                }

                final String filePath = (this.cacheDir.getAbsolutePath() + File.separatorChar + relativeCachePath).replace('/', File.separatorChar);
                final File cacheFile = new File(filePath);

                if ( !cacheFile.exists() ) {
                    cacheFile.getParentFile().mkdirs();
                    final URL u = new URL(url);
                    final URLConnection con = u.openConnection();
                    con.connect();

                    final InputStream readIS = con.getInputStream();
                    final byte[] buffer = new byte[32768];
                    int l;
                    OutputStream os = null;
                    try {
                        os = new FileOutputStream(cacheFile);
                        while ( (l = readIS.read(buffer)) >= 0 ) {
                            os.write(buffer, 0, l);
                        }
                    } finally {
                        try {
                            readIS.close();
                        } catch ( final IOException ignore) {
                            // ignore
                        }
                        if ( os != null ) {
                            try {
                                os.close();
                            } catch ( final IOException ignore ) {
                                // ignore

                            }
                        }
                    }
                    this.config.incDownloadedArtifacts();
                } else {
                    this.config.incCachedArtifacts();
                }
                return cacheFile;
            } catch ( final Exception e) {
                logger.info("Artifact not found in one repository", e);
                // ignore for now
                return null;
            }
        }

        @Override
        public String toString() {
            return "DefaultArtifactHandler";
        }
    }
}
