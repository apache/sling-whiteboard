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
package org.apache.sling.feature.support.io;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.sling.feature.Feature;
import org.apache.sling.feature.support.artifact.ArtifactHandler;
import org.apache.sling.feature.support.artifact.ArtifactManager;
import org.apache.sling.feature.support.json.FeatureJSONReader;
import org.apache.sling.feature.support.json.FeatureJSONReader.SubstituteVariables;

public class FileUtils {

    /** The extension for a reference file. */
    public static final String EXTENSION_REF_FILE = ".ref";

    /** The extension for a feature file. */
    public static final String EXTENSION_FEATURE_FILE = ".json";

    /** The default directory to search for features. */
    public static final String DEFAULT_DIRECTORY = "features";

    /** The default name of the feature file. */
    public static final String DEFAULT_FEATURE_FILE = "feature" + EXTENSION_FEATURE_FILE;

    /**
     * Parse a feature reference file
     * @param file The file
     * @return The referenced features
     * @throws IOException If reading fails
     */
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

    /**
     * Get the list of feature files.
     * If the provided list of files is {@code null} or an empty array, the default is used.
     * The default checks for the following places, the first one found is used. If none is
     * found an empty list is returned.
     * <ol>
     *   <li>A directory named {@link #DEFAULT_DIRECTORY} in the current directory
     *   <li>A file named {@link #DEFAULT_FEATURE_FILE} in the current directory
     *   <li>A directory named {@link #DEFAULT_DIRECTORY} in the home directory
     *   <li>A file named {@link #DEFAULT_FEATURE_FILE} in the home directory
     * </ol>
     *
     * The list of files is processed one after the other. If it is relative, it is
     * first tried to be resolved against the current directory and then against the
     * home directory.
     * If an entry denotes a directory, all children ending in {@link #EXTENSION_FEATURE_FILE} or
     * {@link #EXTENSION_REF_FILE} of that directory are read.
     * If a file ends in {@link #EXTENSION_REF_FILE} the contents is read and every line not
     * starting with the hash sign is considered a reference to a feature artifact.
     *
     * @param homeDirectory If relative files should be resolved, this is the directory to use
     * @param files Optional list of files. If none is provided, a default is used.
     * @return The list of files.
     * @throws IOException If an error occurs.
     */
    public static List<String> getFeatureFiles(final File homeDirectory, final String... files)
    throws IOException {
        String[] featureFiles = files;
        if ( featureFiles == null || featureFiles.length == 0 ) {
            // Default value - check feature directory otherwise features file
            final File[] candidates = new File[] {
                    new File(homeDirectory, DEFAULT_DIRECTORY),
                    new File(homeDirectory, DEFAULT_FEATURE_FILE),
                    new File(DEFAULT_DIRECTORY),
                    new File(DEFAULT_FEATURE_FILE)
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
                            new File(homeDirectory, DEFAULT_DIRECTORY + File.separatorChar + name),
                            new File(name),
                            new File(DEFAULT_DIRECTORY + File.separatorChar + name),
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
     * Read the feature
     *
     * @param file The feature file
     * @param artifactManager The artifact manager to read the feature
     * @param substituteVariables Variable substitution handling
     * @return The read feature
     * @throws IOException If reading fails
     */
    public static Feature getFeature(final String file,
            final ArtifactManager artifactManager,
            final SubstituteVariables substituteVariables)
    throws IOException {
        final ArtifactHandler featureArtifact = artifactManager.getArtifactHandler(file);

        try (final FileReader r = new FileReader(featureArtifact.getFile())) {
            final Feature f = FeatureJSONReader.read(r, featureArtifact.getUrl(), substituteVariables);
            return f;
        }
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
                if ( f.getName().endsWith(EXTENSION_REF_FILE) || f.getName().endsWith(EXTENSION_FEATURE_FILE) ) {
                    processFile(paths, f);
                }
            }
        }
    }



    private static void processFile(final List<String> paths, final File f)
    throws IOException {
        if ( f.getName().endsWith(EXTENSION_REF_FILE) ) {
            paths.addAll(parseFeatureRefFile(f));
        } else {
            paths.add(f.getAbsolutePath());
        }
    }
}
