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
package org.apache.sling.feature.modelconverter.impl;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.sling.feature.io.ArtifactManager;
import org.apache.sling.feature.io.ArtifactManagerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main {

    private static Logger LOGGER;

    private static String runModes;

    private static String output;

    private static String input;

    private static boolean createApp = false;

    private static boolean includeModelInfo = false;

    private static String repoUrls;

    private static String propsFile;

    /**
     * Parse the command line parameters and update a configuration object.
     * @param args Command line parameters
     * @return Configuration object.
     */
    private static void parseArgs(final String[] args) {
        final Option repoOption =  Option.builder("u").hasArg().argName("Set repository url")
                .desc("repository url").required().build();

        final Option modelOption =  new Option("f", true, "Set feature files/directories");
        final Option propsOption =  new Option("p", true, "sling.properties file");
        final Option runModeOption =  new Option("r", true, "Set run modes (comma separated)");
        final Option createAppOption = new Option("a", false, "If enabled, create application json");
        createAppOption.setArgs(0);
        final Option includeModelOption = new Option("i", false, "Include model filename as metadata for artifacts");
        includeModelOption.setArgs(0);

        final Option outputOption = Option.builder("o").hasArg().argName("Set output file")
                .desc("output file").build();

        final Options options = new Options();
        options.addOption(repoOption);
        options.addOption(modelOption);
        options.addOption(createAppOption);
        options.addOption(outputOption);
        options.addOption(includeModelOption);
        options.addOption(propsOption);
        options.addOption(runModeOption);

        final CommandLineParser parser = new DefaultParser();
        try {
            final CommandLine cl = parser.parse(options, args);

            if ( cl.hasOption(repoOption.getOpt()) ) {
                repoUrls = cl.getOptionValue(repoOption.getOpt());
            }
            if ( cl.hasOption(modelOption.getOpt()) ) {
                input = cl.getOptionValue(modelOption.getOpt());
            }
            if ( cl.hasOption(createAppOption.getOpt()) ) {
                createApp = true;
            }
            if ( cl.hasOption(includeModelOption.getOpt()) ) {
                includeModelInfo = true;
            }
            if ( cl.hasOption(runModeOption.getOpt()) ) {
                runModes = cl.getOptionValue(runModeOption.getOpt());
            }
            if ( cl.hasOption(outputOption.getOpt()) ) {
                output = cl.getOptionValue(outputOption.getOpt());
            }
            if ( cl.hasOption(propsOption.getOpt()) ) {
                propsFile = cl.getOptionValue(propsOption.getOpt());
            }
        } catch ( final ParseException pe) {
            LOGGER.error("Unable to parse command line: {}", pe.getMessage(), pe);
            System.exit(1);
        }
        if ( input == null ) {
            LOGGER.error("Required argument missing: model file or directory");
            System.exit(1);
        }
    }

    private static ArtifactManager getArtifactManager() {
        final ArtifactManagerConfig amConfig = new ArtifactManagerConfig();
        if ( repoUrls != null ) {
            amConfig.setRepositoryUrls(repoUrls.split(","));
        }
        try {
            return ArtifactManager.getArtifactManager(amConfig);
        } catch ( IOException ioe) {
            LOGGER.error("Unable to create artifact manager " + ioe.getMessage(), ioe);
            System.exit(1);
        }
        // we never reach this, but have to keep the compiler happy
        return null;
    }

    public static void main(final String[] args) {
        // setup logging
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
        System.setProperty("org.slf4j.simpleLogger.levelInBrackets", "true");
        System.setProperty("org.slf4j.simpleLogger.showLogName", "false");

        LOGGER = LoggerFactory.getLogger("modelconverter");

        LOGGER.info("Apache Sling Provisiong Model to Feature Application Converter");
        LOGGER.info("");

        parseArgs(args);

        final ArtifactManagerConfig amConfig = new ArtifactManagerConfig();
        if ( repoUrls != null ) {
            amConfig.setRepositoryUrls(repoUrls.split(","));
        }
        final ArtifactManager am = getArtifactManager();

        final File f = new File(input);
        final List<File> files = new ArrayList<>();
        if ( f.isDirectory() ) {
            for(final File file : f.listFiles()) {
                if ( file.isFile() && !file.getName().startsWith(".") ) {
                    files.add(file);
                }
            }
            if ( files.isEmpty() ) {
                LOGGER.error("No files found in {}", f);
                System.exit(1);
            }
            Collections.sort(files);
        } else {
            files.add(f);
        }
        boolean isJson = false;
        boolean isTxt = false;
        for(final File t : files) {
            if ( t.getName().endsWith(".json") ) {
                if ( isTxt ) {
                    LOGGER.error("Input files are a mixture of JSON and txt");
                    System.exit(1);
                }
                isJson = true;
            } else {
                if ( isJson ) {
                    LOGGER.error("Input files are a mixture of JSON and txt");
                    System.exit(1);
                }
                isTxt = true;
            }
        }

        if ( isTxt ) {
            if ( output == null ) {
                output = createApp ? "application.json" : "feature.json";
            }
            ProvisioningToFeature.convert(files, output, runModes, createApp, includeModelInfo, propsFile);
        } else {
            if ( output == null ) {
                output = createApp ? "application.txt" : "feature.txt";
            }
            try {
                FeatureToProvisioning.convert(files, output, createApp, am);
            } catch ( final IOException ioe) {
                LOGGER.error("Unable to read feature/application files " + ioe.getMessage(), ioe);
                System.exit(1);
            } catch ( final Exception e) {
                LOGGER.error("Problem generating application", e);
                System.exit(1);
            }
        }
    }

}
