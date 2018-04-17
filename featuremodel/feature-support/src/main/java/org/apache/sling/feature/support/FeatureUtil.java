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
package org.apache.sling.feature.support;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.sling.feature.Application;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.support.artifact.ArtifactHandler;
import org.apache.sling.feature.support.artifact.ArtifactManager;
import org.apache.sling.feature.support.json.FeatureJSONReader;
import org.apache.sling.feature.support.json.FeatureJSONReader.SubstituteVariables;
import org.apache.sling.feature.support.process.ApplicationBuilder;
import org.apache.sling.feature.support.process.BuilderContext;
import org.apache.sling.feature.support.process.FeatureProvider;
import org.apache.sling.feature.support.resolver.FeatureResolver;
import org.apache.sling.feature.support.resolver.FeatureResource;

public class FeatureUtil {
    /**
     * Get an artifact id for the Apache Felix framework
     * @param version The version to use or {@code null} for the default version
     * @return The artifact id
     * @throws IllegalArgumentException If the provided version is invalid
     */
    public static ArtifactId getFelixFrameworkId(final String version) {
        return new ArtifactId("org.apache.felix",
                "org.apache.felix.framework",
                version != null ? version : "5.6.10", null, null);
    }

    static final Comparator<String> FEATURE_PATH_COMP = new Comparator<String>() {

        @Override
        public int compare(final String o1, final String o2) {
            // windows path conversion
            final String key1 = o1.replace(File.separatorChar, '/');
            final String key2 = o2.replace(File.separatorChar, '/');

            final int lastSlash1 = key1.lastIndexOf('/');
            final int lastSlash2 = key2.lastIndexOf('/');
            if ( lastSlash1 == -1 || lastSlash2 == -1 ) {
                return o1.compareTo(o2);
            }
            final String path1 = key1.substring(0, lastSlash1 + 1);
            final String path2 = key2.substring(0, lastSlash2 + 1);
            if ( path1.equals(path2) ) {
                return o1.compareTo(o2);
            }
            if ( path1.startsWith(path2) ) {
                return 1;
            } else if ( path2.startsWith(path1) ) {
                return -1;
            }
            return o1.compareTo(o2);
        }
    };

    private static void processDir(final List<String> paths, final File dir)
    throws IOException {
        for(final File f : dir.listFiles()) {
            if ( f.isFile() && !f.getName().startsWith(".")) {
                // check if file is a reference
                if ( f.getName().endsWith(".ref") || f.getName().endsWith(".json") ) {
                    processFile(paths, f);
                }
            }
        }
    }

    public static List<String> parseFeatureRefFile(final File file)
    throws IOException {
        final List<String> result = new ArrayList<>();
        final List<String> lines = Files.readAllLines(file.toPath());
        for(String line : lines) {
            line = line.trim();
            if ( !line.isEmpty() && !line.startsWith("#") ) {
                if ( line.indexOf(':') == -1 ) {
                    result.add(new File(line).getAbsolutePath());
                } else {
                    result.add(line);
                }
            }
        }
        return result;
    }

    private static void processFile(final List<String> paths, final File f)
    throws IOException {
        if ( f.getName().endsWith(".ref") ) {
            paths.addAll(parseFeatureRefFile(f));
        } else {
            paths.add(f.getAbsolutePath());
        }
    }

    /**
     * Get the list of feature files.
     * If the provided list of files is {@code null} or an empty array, the default is used.
     * The default checks for the following places, the first one found is used. If none is
     * found an empty list is returned.
     * <ol>
     *   <li>A directory named {@code feature} in the current directory
     *   <li>A file named {@code features.json} in the current directory
     *   <li>A directory named {@code feature} in the home directory
     *   <li>A file named {@code features.json} in the home directory
     * </ol>
     *
     * The list of files is processed one after the other. If it is relative, it is
     * first tried to be resolved against the current directory and then against the
     * home directory.
     * If an entry denotes a directory, all children ending in {@code .json} or {@code .ref} of that directory are read.
     * If a file ends in {@code .ref} the contents is read and every line not starting with the
     * hash sign is considered a reference to a feature artifact.
     *
     * @param homeDirectory If relative files should be resolved, this is the directory to use
     * @param files Optional list of files. If none is provided, a default is used.
     * @return The list of files.
     * @throws IOException If an error occurs.
     */
    public static List<String> getFeatureFiles(final File homeDirectory, final String... files) throws IOException {
        String[] featureFiles = files;
        if ( featureFiles == null || featureFiles.length == 0 ) {
            // Default value - check feature directory otherwise features file
            final File[] candidates = new File[] {
                    new File(homeDirectory, "features"),
                    new File(homeDirectory, "features.json"),
                    new File("features"),
                    new File("features.json")
            };
            File f = null;
            for(final File c : candidates) {
                if ( c.exists() ) {
                    f = c;
                    break;
                }
            }
            // nothing found, we default to the first candidate and fail later
            if ( f == null ) {
                f = candidates[0];
            }

            featureFiles = new String[] {f.getAbsolutePath()};
        }

        final List<String> paths = new ArrayList<>();
        for(final String name : featureFiles) {
            // check for absolute
            if ( name.indexOf(':') > 1 ) {
                paths.add(name);
            } else {
                // file or relative
                File f = null;
                final File test = new File(name);
                if ( test.isAbsolute() ) {
                    f = test;
                } else {
                    final File[] candidates = {
                            new File(homeDirectory, name),
                            new File(homeDirectory, "features" + File.separatorChar + name),
                            new File(name),
                            new File("features" + File.separatorChar + name),
                    };
                    for(final File c : candidates) {
                        if ( c.exists() && c.isFile() ) {
                            f = c;
                            break;
                        }
                    }
                }

                if ( f != null && f.exists() ) {
                    if ( f.isFile() ) {
                        processFile(paths, f);
                    } else {
                        processDir(paths, f);
                    }
                } else {
                    // we simply add the path and fail later on
                    paths.add(new File(name).getAbsolutePath());
                }
            }
        }

        Collections.sort(paths, FEATURE_PATH_COMP);
        return paths;
    }

    /**
     * Assemble an application based on the given files.
     *
     * Read the features and assemble the application
     * @param app The optional application to use as a base.
     * @param featureFiles The feature files.
     * @param artifactManager The artifact manager
     * @param fr
     * @return The assembled application
     * @throws IOException If a feature can't be read or no feature is found.
     * @see #getFeatureFiles(File, String...)
     */
    public static Application assembleApplication(
            Application app,
            final ArtifactManager artifactManager, FeatureResolver fr, final String... featureFiles)
    throws IOException {
        final List<Feature> features = new ArrayList<>();
        for(final String initFile : featureFiles) {
            final Feature f = getFeature(initFile, artifactManager);
            features.add(f);
        }

        return assembleApplication(app, artifactManager, fr, features.toArray(new Feature[0]));
    }

    public static Feature[] sortFeatures(final FeatureResolver fr, final Feature... features) {
        final List<Feature> featureList = new ArrayList<>();
        for(final Feature f : features) {
            featureList.add(f);
        }

        final List<Feature> sortedFeatures;
        if (fr != null) {
            // order by dependency chain
            final List<FeatureResource> sortedResources = fr.orderResources(featureList);

            sortedFeatures = new ArrayList<>();
            for (final FeatureResource rsrc : sortedResources) {
                Feature f = rsrc.getFeature();
                if (!sortedFeatures.contains(f)) {
                    sortedFeatures.add(rsrc.getFeature());
                }
            }
        } else {
            sortedFeatures = featureList;
            Collections.sort(sortedFeatures);
        }
        return sortedFeatures.toArray(new Feature[sortedFeatures.size()]);
    }

    public static Application assembleApplication(
            Application app,
            final ArtifactManager artifactManager, FeatureResolver fr, final Feature... features)
    throws IOException {
        if ( features.length == 0 ) {
            throw new IOException("No features found.");
        }

        app = ApplicationBuilder.assemble(app, new BuilderContext(new FeatureProvider() {

            @Override
            public Feature provide(final ArtifactId id) {
                try {
                    final ArtifactHandler handler = artifactManager.getArtifactHandler("mvn:" + id.toMvnPath());
                    try (final FileReader r = new FileReader(handler.getFile())) {
                        final Feature f = FeatureJSONReader.read(r, handler.getUrl(), SubstituteVariables.RESOLVE);
                        return f;
                    }

                } catch (final IOException e) {
                    // ignore
                }
                return null;
            }
        }), sortFeatures(fr, features));

        // check framework
        if ( app.getFramework() == null ) {
            // use hard coded Apache Felix
            app.setFramework(getFelixFrameworkId(null));
        }

        return app;
    }

    /**
     * Read the feature
     *
     * @param file The feature file
     * @param artifactManager The artifact manager to read the feature
     * @return The read feature
     * @throws IOException If reading fails
     */
    public static Feature getFeature(final String file,
            final ArtifactManager artifactManager) throws IOException {
        return getFeature(file, artifactManager, SubstituteVariables.RESOLVE);
    }

    public static Feature getFeature(final String file,
            final ArtifactManager artifactManager, final SubstituteVariables substituteVariables)
    throws IOException {
        final ArtifactHandler featureArtifact = artifactManager.getArtifactHandler(file);

        try (final FileReader r = new FileReader(featureArtifact.getFile())) {
            final Feature f = FeatureJSONReader.read(r, featureArtifact.getUrl(), substituteVariables);
            return f;
        }
    }
}
