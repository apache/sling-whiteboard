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
package org.apache.sling.feature.launcher.impl;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.sling.feature.Application;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.io.ArtifactHandler;
import org.apache.sling.feature.io.ArtifactManager;
import org.apache.sling.feature.launcher.impl.launchers.FrameworkLauncher;
import org.apache.sling.feature.launcher.spi.Launcher;
import org.apache.sling.feature.launcher.spi.LauncherPrepareContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the launcher main class.
 * It parses command line parameters and prepares the launcher.
 */
public class Main {

    private static Logger LOGGER;

    public static Logger LOG() {
        if ( LOGGER == null ) {
            LOGGER = LoggerFactory.getLogger("launcher");
        }
        return LOGGER;
    }

    /** Split a string into key and value */
    private static String[] split(final String val) {
        final int pos = val.indexOf('=');
        if ( pos == -1 ) {
            return new String[] {val, "true"};
        }
        return new String[] {val.substring(0, pos), val.substring(pos + 1)};
    }

    /**
     * Parse the command line parameters and update a configuration object.
     * @param args Command line parameters
     * @return Configuration object.
     */
    private static void parseArgs(final LauncherConfig config, final String[] args) {
        Main.LOG().info("Assembling configuration...");
        final Options options = new Options();

        final Option repoOption =  new Option("u", true, "Set repository url");
        final Option modelOption =  new Option("f", true, "Set feature files/directories");
        final Option appOption =  new Option("a", true, "Set application file");
        final Option fwkProperties = new Option("D", true, "Set framework properties");
        fwkProperties.setArgs(20);
        final Option debugOption = new Option("v", true, "Verbose");
        debugOption.setArgs(0);
        final Option installerOption = new Option("I", true, "Use OSGi installer for additional artifacts.");
        installerOption.setArgs(0);
        options.addOption(repoOption);
        options.addOption(appOption);
        options.addOption(modelOption);
        options.addOption(fwkProperties);
        options.addOption(debugOption);
        options.addOption(installerOption);

        final CommandLineParser clp = new BasicParser();
        try {
            final CommandLine cl = clp.parse(options, args);

            if ( cl.hasOption(repoOption.getOpt()) ) {
                final String value = cl.getOptionValue(repoOption.getOpt());
                config.setRepositoryUrls(value.split(","));
            }
            if ( cl.hasOption(modelOption.getOpt()) ) {
                final String value = cl.getOptionValue(modelOption.getOpt());
                config.setFeatureFiles(value.split(","));
            }
            if ( cl.hasOption(fwkProperties.getOpt()) ) {
                for(final String value : cl.getOptionValues(fwkProperties.getOpt())) {
                    final String[] keyVal = split(value);

                    config.getInstallation().getFrameworkProperties().put(keyVal[0], keyVal[1]);
                }
            }
            if ( cl.hasOption(debugOption.getOpt()) ) {
                System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
                LOGGER = null;
            }
            if ( cl.hasOption(installerOption.getOpt()) ) {
                config.setUseInstaller();
            }
            if ( cl.hasOption(appOption.getOpt()) ) {
                config.setApplicationFile(cl.getOptionValue(appOption.getOpt()));
            }
        } catch ( final ParseException pe) {
            Main.LOG().error("Unable to parse command line: {}", pe.getMessage(), pe);
            System.exit(1);
        }
    }

    public static void main(final String[] args) {
        // setup logging
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
        System.setProperty("org.slf4j.simpleLogger.levelInBrackets", "true");
        System.setProperty("org.slf4j.simpleLogger.showLogName", "false");
        Main.LOG().info("");
        Main.LOG().info("Apache Sling Application Launcher");
        Main.LOG().info("---------------------------------");

        // check if launcher has already been created
        final LauncherConfig launcherConfig = new LauncherConfig();
        parseArgs(launcherConfig, args);

        ArtifactManager artifactManager = null;
        try {

            Main.LOG().info("Initializing...");
            try {
                artifactManager = ArtifactManager.getArtifactManager(launcherConfig);
            } catch ( final IOException ioe) {
                Main.LOG().error("Unable to setup artifact manager: {}", ioe.getMessage(), ioe);
                System.exit(1);
            }
            Main.LOG().info("Artifact Repositories: {}", Arrays.toString(launcherConfig.getRepositoryUrls()));
            Main.LOG().info("Assembling provisioning model...");

            try {
                final Launcher launcher = new FrameworkLauncher();
                final Application app = FeatureProcessor.createApplication(launcherConfig, artifactManager);

                Main.LOG().info("");
                Main.LOG().info("Assembling launcher...");
                final ArtifactManager aMgr = artifactManager;
                final LauncherPrepareContext ctx = new LauncherPrepareContext() {

                    @Override
                    public File getArtifactFile(final ArtifactId artifact) throws IOException {
                        final ArtifactHandler handler = aMgr.getArtifactHandler(":" + artifact.toMvnPath());
                        return handler.getFile();
                    }

                    @Override
                    public void addAppJar(final File jar) {
                        launcherConfig.getInstallation().addAppJar(jar);
                    }
                };
                launcher.prepare(ctx, app);

                FeatureProcessor.prepareLauncher(launcherConfig, artifactManager, app);

            } catch ( final Exception iae) {
                Main.LOG().error("Error while assembling launcher: {}", iae.getMessage(), iae);
                System.exit(1);
            }

            Main.LOG().info("Using {} local artifacts, {} cached artifacts, and {} downloaded artifacts",
                    launcherConfig.getLocalArtifacts(), launcherConfig.getCachedArtifacts(), launcherConfig.getDownloadedArtifacts());
        } finally {
            if ( artifactManager != null ) {
                artifactManager.shutdown();
            }
        }

        try {
            run(launcherConfig);
        } catch ( final Exception iae) {
            Main.LOG().error("Error while running launcher: {}", iae.getMessage(), iae);
            System.exit(1);
        }
    }

    private static final String STORAGE_PROPERTY = "org.osgi.framework.storage";

    private static final String START_LEVEL_PROP = "org.osgi.framework.startlevel.beginning";

    /**
     * Run launcher.
     * @param config The configuration
     * @throws Exception If anything goes wrong
     */
    private static void run(final LauncherConfig config) throws Exception {
        Main.LOG().info("");
        Main.LOG().info("Starting launcher...");
        Main.LOG().info("Launcher Home: {}", config.getHomeDirectory().getAbsolutePath());
        Main.LOG().info("Cache Directory: {}", config.getCacheDirectory().getAbsolutePath());
        Main.LOG().info("Startup Mode: {}", config.getStartupMode());
        Main.LOG().info("");

        final Installation installation = config.getInstallation();

        // set sling home, and use separate locations for launchpad and properties
        installation.getFrameworkProperties().put("sling.home", config.getHomeDirectory().getAbsolutePath());
        installation.getFrameworkProperties().put("sling.launchpad", config.getHomeDirectory().getAbsolutePath() + "/launchpad");
        installation.getFrameworkProperties().put("sling.properties", "conf/sling.properties");


        // additional OSGi properties
        // move storage inside launcher
        if ( installation.getFrameworkProperties().get(STORAGE_PROPERTY) == null ) {
            installation.getFrameworkProperties().put(STORAGE_PROPERTY, config.getHomeDirectory().getAbsolutePath() + File.separatorChar + "framework");
        }
        // set start level to 30
        if ( installation.getFrameworkProperties().get(START_LEVEL_PROP) == null ) {
            installation.getFrameworkProperties().put(START_LEVEL_PROP, "30");
        }

        final Launcher launcher = new FrameworkLauncher();
        launcher.run(installation, createClassLoader(installation));

        config.clear();
    }

    /**
     * Create the class loader.
     * @param installation The launcher configuration
     * @throws Exception If anything goes wrong
     */
    public static ClassLoader createClassLoader(final Installation installation) throws Exception {
        final List<URL> list = new ArrayList<>();
        for(final File f : installation.getAppJars()) {
            try {
                list.add(f.toURI().toURL());
            } catch (IOException e) {
                // ignore
            }
        }
        list.add(Main.class.getProtectionDomain().getCodeSource().getLocation());

        final URL[] urls = list.toArray(new URL[list.size()]);

        if ( Main.LOG().isDebugEnabled() ) {
            Main.LOG().debug("App classpath: ");
            for (int i = 0; i < urls.length; i++) {
                Main.LOG().debug(" - {}", urls[i]);
            }
        }

        // create a paranoid class loader, loading from parent last
        final ClassLoader cl = new URLClassLoader(urls) {
            @Override
            public final Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                // First check if it's already loaded
                Class<?> clazz = findLoadedClass(name);

                if (clazz == null) {

                    try {
                        clazz = findClass(name);
                    } catch (ClassNotFoundException cnfe) {
                        ClassLoader parent = getParent();
                        if (parent != null) {
                            // Ask to parent ClassLoader (can also throw a CNFE).
                            clazz = parent.loadClass(name);
                        } else {
                            // Propagate exception
                            throw cnfe;
                        }
                    }
                }

                if (resolve) {
                    resolveClass(clazz);
                }

                return clazz;
            }

            @Override
            public final URL getResource(final String name) {

                URL resource = findResource(name);
                ClassLoader parent = this.getParent();
                if (resource == null && parent != null) {
                    resource = parent.getResource(name);
                }

                return resource;
            }
        };

        Thread.currentThread().setContextClassLoader(cl);

        return cl;
    }
}
