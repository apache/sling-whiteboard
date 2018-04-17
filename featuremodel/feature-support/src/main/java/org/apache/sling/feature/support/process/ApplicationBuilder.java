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
package org.apache.sling.feature.support.process;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.feature.Application;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;

/**
 * Build an application based on features.
 */
public class ApplicationBuilder {

    /**
     * Assemble an application based on the provided feature Ids.
     * The features are processed in the order they are provided.
     *
     * @param app The optional application to use as a base.
     * @param context The builder context
     * @param featureIds The feature ids
     * @return The application
     * throws IllegalArgumentException If context or featureIds is {@code null}
     * throws IllegalStateException If the provided ids are invalid, or the feature can't be provided
     */
    public static Application assemble(final Application app,
            final BuilderContext context,
            final String... featureIds) {
        if ( featureIds == null || context == null ) {
            throw new IllegalArgumentException("Features and/or context must not be null");
        }

        final Feature[] features = new Feature[featureIds.length];
        int index = 0;
        for(final String id : featureIds) {
            features[index] = context.getFeatureProvider().provide(ArtifactId.parse(id));
            if ( features[index] == null ) {
                throw new IllegalStateException("Unable to find included feature " + id);
            }
            index++;
        }
        return assemble(app, context, features);
    }

    /**
     * Assemble an application based on the provided features.
     *
     * The features are processed in the order they are provided.
     * If the same feature is included more than once only the feature with
     * the highest version is used. The others are ignored.
     *
     * @param app The optional application to use as a base.
     * @param context The builder context
     * @param features The features
     * @return The application
     * throws IllegalArgumentException If context or featureIds is {@code null}
     * throws IllegalStateException If a feature can't be provided
     */
    public static Application assemble(
            Application app,
            final BuilderContext context,
            final Feature... features) {
        if ( features == null || context == null ) {
            throw new IllegalArgumentException("Features and/or context must not be null");
        }

        if ( app == null ) {
            app = new Application();
        }

        // Remove duplicate features by selecting the one with the highest version
        final List<Feature> featureList = new ArrayList<>();
        for(final Feature f : features) {
            Feature found = null;
            for(final Feature s : featureList) {
                if ( s.getId().isSame(f.getId()) ) {
                    found = s;
                    break;
                }
            }
            boolean add = true;
            // feature with different version found
            if ( found != null ) {
                if ( f.getId().getOSGiVersion().compareTo(found.getId().getOSGiVersion()) <= 0 ) {
                    // higher version already included
                    add = false;
                } else {
                    // remove lower version, higher version will be added
                    featureList.remove(found);
                }
            }
            if ( add ) {
                featureList.add(f);
            }
        }

        // assemble
        int featureStartOrder = 5; // begin with start order a little higher than 0
        for(final Feature f : featureList) {
            app.getFeatureIds().add(f.getId());
            final Feature assembled = FeatureBuilder.assemble(f, context.clone(new FeatureProvider() {

                @Override
                public Feature provide(final ArtifactId id) {
                    for(final Feature f : features) {
                        if ( f.getId().equals(id) ) {
                            return f;
                        }
                    }
                    return context.getFeatureProvider().provide(id);
                }
            }));

            int globalStartOrder = featureStartOrder;
            for (Artifact a : assembled.getBundles()) {
                int so = a.getStartOrder() + featureStartOrder;
                if (so > globalStartOrder)
                    globalStartOrder = so;
                a.setStartOrder(so);
            }
            // Next feature will have a higher start order than the previous
            featureStartOrder = globalStartOrder + 1;

            merge(app, assembled);
        }

        return app;
    }

    private static void merge(final Application target, final Feature source) {
        BuilderUtil.mergeBundles(target.getBundles(), source.getBundles(), BuilderUtil.ArtifactMerge.HIGHEST);
        BuilderUtil.mergeConfigurations(target.getConfigurations(), source.getConfigurations());
        BuilderUtil.mergeFrameworkProperties(target.getFrameworkProperties(), source.getFrameworkProperties());
        BuilderUtil.mergeExtensions(target, source, BuilderUtil.ArtifactMerge.HIGHEST);
    }
}
