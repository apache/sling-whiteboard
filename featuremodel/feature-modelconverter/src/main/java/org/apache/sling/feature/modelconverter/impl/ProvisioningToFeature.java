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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Map.Entry;
import java.util.Set;

/** Converter that converts the provisioning model to the feature model.
 */
public class ProvisioningToFeature {
    private static Logger LOGGER = LoggerFactory.getLogger(ProvisioningToFeature.class);

    public static void convert(File file, String output) {
        Model model = createModel(Collections.singletonList(file), null, false);
        final List<org.apache.sling.feature.Feature> features = buildFeatures(model);
        if (features.size() != 1)
            throw new IllegalStateException("TODO");

        writeFeature(features.get(0), output, 0);
    }

    public static void convert(List<File> files,  String outputFile, String runModes, boolean createApp,
            boolean includeModelInfo, String propsFile) {
        final Model model = createModel(files, runModes, includeModelInfo);

        if ( createApp ) {
            final Application app = buildApplication(model, propsFile);

            writeApplication(app, outputFile);
        } else {
            final List<org.apache.sling.feature.Feature> features = buildFeatures(model);
            int index = 1;
            for(final org.apache.sling.feature.Feature feature : features) {
                writeFeature(feature, outputFile, features.size() > 1 ? index : 0);
                index++;
            }
        }
    }

    /**
     * Read the models and prepare the model
     * @param files The model files
     * @param includeModelInfo
     */
    private static Model createModel(final List<File> files,
            final String runModes, boolean includeModelInfo) {
        LOGGER.info("Assembling model...");
        Model model = null;
        for(final File initFile : files) {
            try {
                model = processModel(model, initFile, includeModelInfo);
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
     * @param includeModelInfo
     * @return The merged model
     * @throws IOException If reading fails
     */
    private static Model processModel(Model model,
            final File modelFile, boolean includeModelInfo) throws IOException {
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
                            model = processModel(model, handler.getFile(), includeModelInfo);

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
    public static Model readProvisioningModel(final File file)
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

    private static Application buildApplication(final Model model, String propsFile) {
        final Application app = new Application();

        for(final Feature feature : model.getFeatures() ) {
            buildFromFeature(feature, app.getVariables(), app.getBundles(), app.getConfigurations(), app.getExtensions(), app.getFrameworkProperties());
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
            final KeyValueMap variables,
            final Bundles bundles,
            final Configurations configurations,
            final Extensions extensions,
            final KeyValueMap properties) {
        for (Iterator<Map.Entry<String, String>> it = feature.getVariables().iterator(); it.hasNext(); ) {
            Entry<String, String> entry = it.next();
            variables.put(entry.getKey(), entry.getValue());
        }
        if (feature.getName().startsWith(":")) {
            variables.put(FeatureToProvisioning.PROVISIONING_MODEL_NAME_VARIABLE, feature.getName());
        }

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

                            // TODO this is probably not correct
                            // newArtifact.getMetadata().put(org.apache.sling.feature.Artifact.KEY_START_ORDER, String.valueOf(startLevel));

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

            buildFromFeature(feature, f.getVariables(), f.getBundles(), f.getConfigurations(), f.getExtensions(), f.getFrameworkProperties());
        }

        return features;
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

        LOGGER.info("to file {}", out);

        final File file = new File(out);
        try ( final FileWriter writer = new FileWriter(file)) {
            FeatureJSONWriter.write(writer, f);
        } catch ( final IOException ioe) {
            LOGGER.error("Unable to write feature to {} : {}", out, ioe.getMessage(), ioe);
            System.exit(1);
        }
    }
}
