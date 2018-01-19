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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.sling.feature.Application;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Bundles;
import org.apache.sling.feature.Configurations;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.ExtensionType;
import org.apache.sling.feature.Extensions;
import org.apache.sling.feature.KeyValueMap;
import org.apache.sling.feature.support.ArtifactHandler;
import org.apache.sling.feature.support.ArtifactManager;
import org.apache.sling.feature.support.ArtifactManagerConfig;
import org.apache.sling.feature.support.FeatureUtil;
import org.apache.sling.feature.support.json.ApplicationJSONReader;
import org.apache.sling.feature.support.json.ApplicationJSONWriter;
import org.apache.sling.feature.support.json.FeatureJSONWriter;
import org.apache.sling.provisioning.model.Artifact;
import org.apache.sling.provisioning.model.ArtifactGroup;
import org.apache.sling.provisioning.model.Configuration;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.MergeUtility;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelConstants;
import org.apache.sling.provisioning.model.ModelUtility;
import org.apache.sling.provisioning.model.ModelUtility.ResolverOptions;
import org.apache.sling.provisioning.model.ModelUtility.VariableResolver;
import org.apache.sling.provisioning.model.RunMode;
import org.apache.sling.provisioning.model.Section;
import org.apache.sling.provisioning.model.Traceable;
import org.apache.sling.provisioning.model.io.ModelReader;
import org.apache.sling.provisioning.model.io.ModelWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            final Model model = createModel(files, runModes);

            if ( createApp ) {
                final Application app = buildApplication(model);

                writeApplication(app, output);
            } else {
                final List<org.apache.sling.feature.Feature> features = buildFeatures(model);
                int index = 1;
                for(final org.apache.sling.feature.Feature feature : features) {
                    writeFeature(feature, output, features.size() > 1 ? index : 0);
                    index++;
                }
            }
        } else {
            if ( output == null ) {
                output = createApp ? "application.txt" : "feature.txt";
            }
            try {
                if ( createApp ) {
                    // each file is an application
                    int index = 1;
                    for(final File appFile : files ) {
                        try ( final FileReader r = new FileReader(appFile) ) {
                            final Application app = ApplicationJSONReader.read(r);
                            convert(app, files.size() > 1 ? index : 0);
                        }
                        index++;
                    }
                } else {
                    final Application app = FeatureUtil.assembleApplication(null, am, files.stream()
                            .map(File::getAbsolutePath)
                            .toArray(String[]::new));
                    convert(app, 0);
                }
            } catch ( final IOException ioe) {
                LOGGER.error("Unable to read feature/application files " + ioe.getMessage(), ioe);
                System.exit(1);
            }
        }
    }

    private static List<org.apache.sling.feature.Feature> buildFeatures(final Model model) {
        final List<org.apache.sling.feature.Feature> features = new ArrayList<>();

        for(final Feature feature : model.getFeatures() ) {
            final String idString;
            // use a default name if not present or not usable as a Maven artifactId ( starts with ':')
            if ( feature.getName() != null && !feature.isSpecial() ) {
                if ( feature.getVersion() != null ) {
                    idString = "generated/" + feature.getName() + "/" + feature.getVersion();
                } else {
                    idString = "generated/" + feature.getName() + "/1.0.0";
                }
            } else {
                idString = "generated/feature/1.0.0";
            }
            final org.apache.sling.feature.Feature f = new org.apache.sling.feature.Feature(ArtifactId.parse(idString));
            features.add(f);

            buildFromFeature(feature, f.getBundles(), f.getConfigurations(), f.getExtensions(), f.getFrameworkProperties());
        }

        return features;
    }

    private static Application buildApplication(final Model model) {
        final Application app = new Application();

        for(final Feature feature : model.getFeatures() ) {
            buildFromFeature(feature, app.getBundles(), app.getConfigurations(), app.getExtensions(), app.getFrameworkProperties());
        }

        // hard coded dependency to launchpad api
        final org.apache.sling.feature.Artifact a = new org.apache.sling.feature.Artifact(ArtifactId.parse("org.apache.sling/org.apache.sling.launchpad.api/1.2.0"));
        a.getMetadata().put(org.apache.sling.feature.Artifact.KEY_START_ORDER, "1");
        // sling.properties (TODO)
        if ( propsFile == null ) {
            app.getFrameworkProperties().put("org.osgi.framework.bootdelegation", "sun.*,com.sun.*");
        } else {

        }
        // felix framework hard coded for now
        app.setFramework(FeatureUtil.getFelixFrameworkId(null));
        return app;
    }

    private static void buildFromFeature(final Feature feature,
            final Bundles bundles,
            final Configurations configurations,
            final Extensions extensions,
            final KeyValueMap properties) {
        Extension cpExtension = extensions.getByName(Extension.NAME_CONTENT_PACKAGES);
        for(final RunMode runMode : feature.getRunModes() ) {
            if ( !ModelConstants.FEATURE_LAUNCHPAD.equals(feature.getName()) ) {
                for(final ArtifactGroup group : runMode.getArtifactGroups()) {
                    for(final Artifact artifact : group) {
                        final ArtifactId id = ArtifactId.fromMvnUrl(artifact.toMvnUrl());
                        final org.apache.sling.feature.Artifact newArtifact = new org.apache.sling.feature.Artifact(id);

                        for(final Map.Entry<String, String> entry : artifact.getMetadata().entrySet()) {
                            newArtifact.getMetadata().put(entry.getKey(), entry.getValue());
                        }

                        if ( newArtifact.getId().getType().equals("zip") ) {
                            if ( cpExtension == null ) {
                                cpExtension = new Extension(ExtensionType.ARTIFACTS, Extension.NAME_CONTENT_PACKAGES, true);
                                extensions.add(cpExtension);
                            }
                            cpExtension.getArtifacts().add(newArtifact);
                        } else {
                            int startLevel = group.getStartLevel();
                            if ( ModelConstants.FEATURE_BOOT.equals(feature.getName()) ) {
                                startLevel = 1;
                            } else if ( startLevel == 0 ) {
                                startLevel = 20;
                            }
                            newArtifact.getMetadata().put(org.apache.sling.feature.Artifact.KEY_START_ORDER, String.valueOf(startLevel));
                            bundles.add(newArtifact);
                        }
                    }
                }
            }

            for(final Configuration cfg : runMode.getConfigurations()) {
                final org.apache.sling.feature.Configuration newCfg;
                if ( cfg.getFactoryPid() != null ) {
                    newCfg = new org.apache.sling.feature.Configuration(cfg.getFactoryPid(), cfg.getPid());
                } else {
                    newCfg = new org.apache.sling.feature.Configuration(cfg.getPid());
                }
                final Enumeration<String> keys = cfg.getProperties().keys();
                while ( keys.hasMoreElements() ) {
                    final String key = keys.nextElement();
                    newCfg.getProperties().put(key, cfg.getProperties().get(key));
                }
                configurations.add(newCfg);
            }

            for(final Map.Entry<String, String> prop : runMode.getSettings()) {
                properties.put(prop.getKey(), prop.getValue());
            }
        }
        Extension repoExtension = extensions.getByName(Extension.NAME_REPOINIT);
        for(final Section sect : feature.getAdditionalSections("repoinit")) {
            final String text = sect.getContents();
            if ( repoExtension == null ) {
                repoExtension = new Extension(ExtensionType.TEXT, Extension.NAME_REPOINIT, true);
                extensions.add(repoExtension);
                repoExtension.setText(text);
            } else {
                repoExtension.setText(repoExtension.getText() + "\n\n" + text);
            }
        }
    }

    private static void writeApplication(final Application app, final String out) {
        LOGGER.info("Writing application...");
        final File file = new File(out);
        try ( final FileWriter writer = new FileWriter(file)) {
            ApplicationJSONWriter.write(writer, app);
        } catch ( final IOException ioe) {
            LOGGER.error("Unable to write application to {} : {}", out, ioe.getMessage(), ioe);
            System.exit(1);
        }
    }

    private static void writeFeature(final org.apache.sling.feature.Feature f, String out, final int index) {
        LOGGER.info("Writing feature...");
        if ( index > 0 ) {
            final int lastDot = out.lastIndexOf('.');
            if ( lastDot == -1 ) {
                out = out + "_" + String.valueOf(index);
            } else {
                out = out.substring(0, lastDot) + "_" + String.valueOf(index) + out.substring(lastDot);
            }
        }
        final File file = new File(out);
        try ( final FileWriter writer = new FileWriter(file)) {
            FeatureJSONWriter.write(writer, f);
        } catch ( final IOException ioe) {
            LOGGER.error("Unable to write feature to {} : {}", out, ioe.getMessage(), ioe);
            System.exit(1);
        }
    }

    /**
     * Read the models and prepare the model
     * @param files The model files
     */
    private static Model createModel(final List<File> files,
            final String runModes) {
        LOGGER.info("Assembling model...");
        Model model = null;
        for(final File initFile : files) {
            try {
                model = processModel(model, initFile);
            } catch ( final IOException iae) {
                LOGGER.error("Unable to read provisioning model {} : {}", initFile, iae.getMessage(), iae);
                System.exit(1);
            }
        }

        final Model effectiveModel = ModelUtility.getEffectiveModel(model, new ResolverOptions().variableResolver(new VariableResolver() {

            @Override
            public String resolve(Feature feature, String name) {
                if ( "sling.home".equals(name) ) {
                    return "${sling.home}";
                }
                return feature.getVariables().get(name);
            }
        }));
        final Map<Traceable, String> errors = ModelUtility.validate(effectiveModel);
        if ( errors != null ) {
            LOGGER.error("Invalid assembled provisioning model.");
            for(final Map.Entry<Traceable, String> entry : errors.entrySet()) {
                LOGGER.error("- {} : {}", entry.getKey().getLocation(), entry.getValue());
            }
            System.exit(1);
        }
        final Set<String> modes = calculateRunModes(effectiveModel, runModes);

        removeInactiveFeaturesAndRunModes(effectiveModel, modes);

        return effectiveModel;
    }

    /**
     * Process the given model and merge it into the provided model
     * @param model The already read model
     * @param modelFile The model file
     * @return The merged model
     * @throws IOException If reading fails
     */
    private static Model processModel(Model model,
            final File modelFile) throws IOException {
        LOGGER.info("- reading model {}", modelFile);

        final Model nextModel = readProvisioningModel(modelFile);
        // resolve references to other models
        final ResolverOptions options = new ResolverOptions().variableResolver(new VariableResolver() {

            @Override
            public String resolve(final Feature feature, final String name) {
                return name;
            }
        });


        final Model effectiveModel = ModelUtility.getEffectiveModel(nextModel, options);
        for(final Feature feature : effectiveModel.getFeatures()) {
            for(final RunMode runMode : feature.getRunModes()) {
                for(final ArtifactGroup group : runMode.getArtifactGroups()) {
                    final List<org.apache.sling.provisioning.model.Artifact> removeList = new ArrayList<>();
                    for(final org.apache.sling.provisioning.model.Artifact a : group) {
                        if ( "slingstart".equals(a.getType())
                             || "slingfeature".equals(a.getType())) {

                            final ArtifactManagerConfig cfg = new ArtifactManagerConfig();
                            final ArtifactManager mgr = ArtifactManager.getArtifactManager(cfg);

                            final ArtifactId correctedId = new ArtifactId(a.getGroupId(),
                                    a.getArtifactId(),
                                    a.getVersion(),
                                    "slingstart".equals(a.getType()) ? "slingfeature" : a.getClassifier(),
                                    "txt");

                            final ArtifactHandler handler = mgr.getArtifactHandler(correctedId.toMvnUrl());
                            model = processModel(model, handler.getFile());

                            removeList.add(a);
                        } else {
                            final org.apache.sling.provisioning.model.Artifact realArtifact = nextModel.getFeature(feature.getName()).getRunMode(runMode.getNames()).getArtifactGroup(group.getStartLevel()).search(a);

                            if ( includeModelInfo ) {
                                realArtifact.getMetadata().put("model-filename", modelFile.getName());
                            }
                            if ( runMode.getNames() != null ) {
                                realArtifact.getMetadata().put("runmodes", String.join(",", runMode.getNames()));
                            }
                        }
                    }
                    for(final org.apache.sling.provisioning.model.Artifact r : removeList) {
                        nextModel.getFeature(feature.getName()).getRunMode(runMode.getNames()).getArtifactGroup(group.getStartLevel()).remove(r);
                    }
                }
            }
        }

        if ( model == null ) {
            model = nextModel;
        } else {
            MergeUtility.merge(model, nextModel);
        }
        return model;
    }

    /**
     * Read the provisioning model
     */
    private static Model readProvisioningModel(final File file)
    throws IOException {
        try (final FileReader is = new FileReader(file)) {
            final Model m = ModelReader.read(is, file.getAbsolutePath());
            return m;
        }
    }

    private static void removeInactiveFeaturesAndRunModes(final Model m,
            final Set<String> activeRunModes) {
        final String[] requiredFeatures = new String[] {ModelConstants.FEATURE_LAUNCHPAD, ModelConstants.FEATURE_BOOT};
        // first pass:
        // - remove special features except boot required ones
        // - remove special run modes and inactive run modes
        // - remove special configurations (TODO)
        final Iterator<Feature> i = m.getFeatures().iterator();
        while ( i.hasNext() ) {
            final Feature feature = i.next();
            if ( feature.isSpecial() ) {
                boolean remove = true;
                if ( requiredFeatures != null ) {
                    for(final String name : requiredFeatures) {
                        if ( feature.getName().equals(name) ) {
                            remove = false;
                            break;
                        }
                    }
                }
                if ( remove ) {
                    i.remove();
                    continue;
                }
            }
            feature.setComment(null);
            final Iterator<RunMode> rmI = feature.getRunModes().iterator();
            while ( rmI.hasNext() ) {
                final RunMode rm = rmI.next();
                if ( rm.isActive(activeRunModes) || rm.isRunMode(ModelConstants.RUN_MODE_STANDALONE) ) {
                    final Iterator<Configuration> cI = rm.getConfigurations().iterator();
                    while ( cI.hasNext() ) {
                        final Configuration config = cI.next();
                        if ( config.isSpecial() ) {
                            cI.remove();
                            continue;
                        }
                        config.setComment(null);
                    }
                } else {
                    rmI.remove();
                    continue;
                }
            }
        }

        // second pass: aggregate the settings and add them to the first required feature
        final Feature requiredFeature = m.getFeature(requiredFeatures[0]);
        if ( requiredFeature != null ) {
            for(final Feature f : m.getFeatures()) {
                if ( f.getName().equals(requiredFeature.getName()) ) {
                    continue;
                }
                copyAndClearSettings(requiredFeature, f.getRunMode(new String[] {ModelConstants.RUN_MODE_STANDALONE}));
                copyAndClearSettings(requiredFeature, f.getRunMode());
            }
        }
    }

    private static void copyAndClearSettings(final Feature requiredFeature, final RunMode rm) {
        if ( rm != null && !rm.getSettings().isEmpty() ) {
            final RunMode requiredRunMode = requiredFeature.getOrCreateRunMode(null);
            final Set<String> keys = new HashSet<>();
            for(final Map.Entry<String, String> entry : rm.getSettings()) {
                requiredRunMode.getSettings().put(entry.getKey(), entry.getValue());
                keys.add(entry.getKey());
            }

            for(final String key : keys) {
                rm.getSettings().remove(key);
            }
        }
    }

    private static Set<String> calculateRunModes(final Model model, final String runModes) {
        final Set<String> modesSet = new HashSet<>();

        // check configuration property first
        if (runModes != null && runModes.trim().length() > 0) {
            final String[] modes = runModes.split(",");
            for(int i=0; i < modes.length; i++) {
                modesSet.add(modes[i].trim());
            }
        }

        //  handle configured options
        final Feature feature = model.getFeature(ModelConstants.FEATURE_BOOT);
        if ( feature != null ) {
            handleOptions(modesSet, feature.getRunMode().getSettings().get("sling.run.mode.options"));
            handleOptions(modesSet, feature.getRunMode().getSettings().get("sling.run.mode.install.options"));
        }

        return modesSet;
    }

    private static void handleOptions(final Set<String> modesSet, final String propOptions) {
        if ( propOptions != null && propOptions.trim().length() > 0 ) {

            final String[] options = propOptions.trim().split("\\|");
            for(final String opt : options) {
                String selected = null;
                final String[] modes = opt.trim().split(",");
                for(int i=0; i<modes.length; i++) {
                    modes[i] = modes[i].trim();
                    if ( selected != null ) {
                        modesSet.remove(modes[i]);
                    } else {
                        if ( modesSet.contains(modes[i]) ) {
                            selected = modes[i];
                        }
                    }
                }
                if ( selected == null ) {
                    selected = modes[0];
                    modesSet.add(modes[0]);
                }
            }
        }
    }

    private static void convert(final Application app, final int index) {
        final Feature f = new Feature("application");

        // bundles
        for(final org.apache.sling.feature.Artifact bundle : app.getBundles()) {
            final ArtifactId id = bundle.getId();
            final Artifact newBundle = new Artifact(id.getGroupId(), id.getArtifactId(), id.getVersion(), id.getClassifier(), id.getType());
            for(final Map.Entry<String, String> prop : bundle.getMetadata()) {
                newBundle.getMetadata().put(prop.getKey(), prop.getValue());
            }
            int startLevel = bundle.getStartOrder();
            if ( startLevel == 0 ) {
                startLevel = 20;
            }
            f.getOrCreateRunMode(null).getOrCreateArtifactGroup(startLevel).add(newBundle);
        }

        // configurations
        for(final org.apache.sling.feature.Configuration cfg : app.getConfigurations()) {
            final Configuration c;
            if ( cfg.isFactoryConfiguration() ) {
                c = new Configuration(cfg.getName(), cfg.getFactoryPid());
            } else {
                c = new Configuration(cfg.getPid(), null);
            }
            final Enumeration<String> keys = cfg.getProperties().keys();
            while ( keys.hasMoreElements() ) {
                final String key = keys.nextElement();
                c.getProperties().put(key, cfg.getProperties().get(key));
            }
            f.getOrCreateRunMode(null).getConfigurations().add(c);
        }

        // framework properties
        for(final Map.Entry<String, String> prop : app.getFrameworkProperties()) {
            f.getOrCreateRunMode(null).getSettings().put(prop.getKey(), prop.getValue());
        }

        // extensions: content packages and repoinit
        for(final Extension ext : app.getExtensions()) {
            if ( Extension.NAME_CONTENT_PACKAGES.equals(ext.getName()) ) {
                for(final org.apache.sling.feature.Artifact cp : ext.getArtifacts() ) {
                    final ArtifactId id = cp.getId();
                    final Artifact newCP = new Artifact(id.getGroupId(), id.getArtifactId(), id.getVersion(), id.getClassifier(), id.getType());
                    for(final Map.Entry<String, String> prop : cp.getMetadata()) {
                        newCP.getMetadata().put(prop.getKey(), prop.getValue());
                    }
                    f.getOrCreateRunMode(null).getOrCreateArtifactGroup(0).add(newCP);
                }

            } else if ( Extension.NAME_REPOINIT.equals(ext.getName()) ) {
                final Section section = new Section("repoinit");
                section.setContents(ext.getText());
                f.getAdditionalSections().add(section);
            } else if ( ext.isRequired() ) {
                LOGGER.error("Unable to convert required extension {}", ext.getName());
                System.exit(1);
            }
        }

        LOGGER.info("Writing feature...");
        String out = output;
        if ( index > 0 ) {
            final int lastDot = out.lastIndexOf('.');
            if ( lastDot == -1 ) {
                out = out + "_" + String.valueOf(index);
            } else {
                out = out.substring(0, lastDot) + "_" + String.valueOf(index) + out.substring(lastDot);
            }
        }
        final File file = new File(out);
        final Model m = new Model();
        m.getFeatures().add(f);
        try ( final FileWriter writer = new FileWriter(file)) {
            ModelWriter.write(writer, m);
        } catch ( final IOException ioe) {
            LOGGER.error("Unable to write feature to {} : {}", out, ioe.getMessage(), ioe);
            System.exit(1);
        }
    }
}
