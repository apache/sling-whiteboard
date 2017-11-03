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
package org.apache.sling.feature.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.Include;

public class FeatureBuilder {

    /**
     * Assemble the full feature by processing all includes.
     *
     * @param feature The feature to start
     * @param context The builder context
     * @return The assembled feature.
     * @throws IllegalArgumentException If feature or context is {@code null}
     * @throws IllegalStateException If an included feature can't be provided or merged.
     */
    public static Feature assemble(final Feature feature,
            final BuilderContext context) {
        if ( feature == null || context == null ) {
            throw new IllegalArgumentException("Feature and/or context must not be null");
        }
        return internalAssemble(new ArrayList<>(), feature, context);
    }

    /**
     * Assemble the final feature and apply upgrades
     *
     * If the list of upgrades contains upgrade features not intended for the
     * provided feature, this is not considered an error situation. But the
     * provided upgrade is ignored.
     *
     * @param feature The feature to start
     * @param upgrades The list of upgrades. If this is {@code null} or empty, this method
     *     behaves like {@link #assemble(Feature, FeatureProvider)}.
     * @param context The builder context
     * @return The assembled feature.
     * @throws IllegalArgumentException If feature or context is {@code null}
     * @throws IllegalStateException If an included feature can't be provided
     */
    public static Feature assemble(final Feature feature,
            final List<Feature> upgrades,
            final BuilderContext context) {
        if ( feature == null || context == null ) {
            throw new IllegalArgumentException("Feature and/or context must not be null");
        }

        // check upgrades
        List<Feature> useUpdates = null;
        if ( upgrades != null && !upgrades.isEmpty() ) {
            useUpdates = new ArrayList<>();
            for(final Feature uf : upgrades) {
                if ( !feature.getId().equals(uf.getUpgradeOf()) ) {
                    continue;
                }
                boolean found = false;
                for(final Feature i : useUpdates) {
                    if ( i.getId().isSame(uf.getId()) ) {
                        if ( uf.getId().getOSGiVersion().compareTo(i.getId().getOSGiVersion()) > 0 ) {
                            useUpdates.remove(i);
                        } else {
                            found = true;
                        }
                        break;
                    }
                }
                if ( !found ) {
                    // we add a copy as we manipulate the upgrade below
                    useUpdates.add(uf.copy());
                }
            }
            Collections.sort(useUpdates);
            if ( useUpdates.isEmpty() ) {
                useUpdates = null;
            }
        }

        // assemble feature without upgrades
        final Feature assembledFeature = internalAssemble(new ArrayList<>(), feature, context);

        // handle upgrades
        if ( useUpdates != null ) {
            for(final Feature uf : useUpdates) {
                Include found = null;
                for(final Include inc : uf.getIncludes() ) {
                    if ( inc.getId().equals(assembledFeature.getId()) ) {
                        found = inc;
                        break;
                    }
                }
                if ( found != null ) {
                    uf.getIncludes().remove(found);

                    // process include instructions
                    include(assembledFeature, found);
                }

                // now assemble upgrade, but without considering the base
                uf.setUpgradeOf(null);
                assembledFeature.getUpgrades().add(uf.getId());
                final Feature auf = assemble(uf, context);

                // merge
                merge(assembledFeature, auf, context);
            }
        }

        return assembledFeature;
    }

    private static Feature internalAssemble(final List<String> processedFeatures,
            final Feature feature,
            final BuilderContext context) {
        if ( feature.isAssembled() ) {
            return feature;
        }
        if ( processedFeatures.contains(feature.getId().toMvnId()) ) {
            throw new IllegalStateException("Recursive inclusion of " + feature.getId().toMvnId() + " via " + processedFeatures);
        }
        processedFeatures.add(feature.getId().toMvnId());

        // we copy the feature as we set the assembled flag on the result
        final Feature result;

        if ( feature.getUpgradeOf() != null ) {
            Include found = null;
            for(final Include inc : feature.getIncludes()) {
                if ( inc.getId().equals(feature.getUpgradeOf()) ) {
                    found = inc;
                    break;
                }
            }

            result = feature.copy(feature.getUpgradeOf());

            // add base as the first include
            if ( found == null ) {
                result.getIncludes().add(0, new Include(feature.getUpgradeOf()));
            } else {
                result.getIncludes().remove(found);
                result.getIncludes().add(0, found);
            }
            result.getUpgrades().add(feature.getId());
        } else {
            result = feature.copy();
        }

        if ( !result.getIncludes().isEmpty() ) {

            final List<Include> includes = new ArrayList<>(result.getIncludes());

            // clear everything in the result, will be added in the process
            result.getBundles().clear();
            result.getFrameworkProperties().clear();
            result.getConfigurations().clear();
            result.getRequirements().clear();
            result.getCapabilities().clear();
            result.getIncludes().clear();
            result.getExtensions().clear();

            for(final Include i : includes) {
                final Feature f = context.getFeatureProvider().provide(i.getId());
                if ( f == null ) {
                    throw new IllegalStateException("Unable to find included feature " + i.getId());
                }
                final Feature af = internalAssemble(processedFeatures, f, context);

                // process include instructions
                include(af, i);

                // and now merge
                merge(result, af, context);
            }
            merge(result, feature, context);
        }
        processedFeatures.remove(feature.getId().toMvnId());

        result.setAssembled(true);
        return result;
    }

    private static void merge(final Feature target,
            final Feature source,
            final BuilderContext context) {
        BuilderUtil.mergeBundles(target.getBundles(), source.getBundles(), BuilderUtil.ArtifactMerge.LATEST);
        BuilderUtil.mergeConfigurations(target.getConfigurations(), source.getConfigurations());
        BuilderUtil.mergeFrameworkProperties(target.getFrameworkProperties(), source.getFrameworkProperties());
        BuilderUtil.mergeRequirements(target.getRequirements(), source.getRequirements());
        BuilderUtil.mergeCapabilities(target.getCapabilities(), source.getCapabilities());
        BuilderUtil.mergeExtensions(target,
                source,
                BuilderUtil.ArtifactMerge.LATEST,
                context);
    }

    private static void include(final Feature base, final Include i) {
        // process removals
        // bundles
        for(final ArtifactId a : i.getBundleRemovals()) {
            base.getBundles().removeExact(a);
            final Iterator<Configuration> iter = base.getConfigurations().iterator();
            while ( iter.hasNext() ) {
                final Configuration cfg = iter.next();
                final String bundleId = (String)cfg.getProperties().get(Configuration.PROP_ARTIFACT);
                if ( a.toMvnId().equals(bundleId) ) {
                    iter.remove();
                }
            }
        }
        // configurations
        for(final String c : i.getConfigurationRemovals()) {
            final int attrPos = c.indexOf('@');
            final String val = (attrPos == -1 ? c : c.substring(0, attrPos));
            final String attr = (attrPos == -1 ? null : c.substring(attrPos + 1));

            final int sepPos = val.indexOf('~');
            Configuration found = null;
            if ( sepPos == -1 ) {
                found = base.getConfigurations().getConfiguration(val);

            } else {
                final String factoryPid = val.substring(0, sepPos);
                final String name = val.substring(sepPos + 1);

                found = base.getConfigurations().getFactoryConfiguration(factoryPid, name);
            }
            if ( found != null ) {
                if ( attr == null ) {
                    base.getConfigurations().remove(found);
                } else {
                    found.getProperties().remove(attr);
                }
            }
        }

        // framework properties
        for(final String p : i.getFrameworkPropertiesRemovals()) {
            base.getFrameworkProperties().remove(p);
        }

        // extensions
        for(final String name : i.getExtensionRemovals()) {
            for(final Extension ext : base.getExtensions()) {
                if ( ext.getName().equals(name) ) {
                    base.getExtensions().remove(ext);
                    break;
                }
            }
        }
        for(final Map.Entry<String, List<ArtifactId>> entry : i.getArtifactExtensionRemovals().entrySet()) {
            for(final Extension ext : base.getExtensions()) {
                if ( ext.getName().equals(entry.getKey()) ) {
                    for(final ArtifactId id : entry.getValue() ) {
                        for(final Artifact a : ext.getArtifacts()) {
                            if ( a.getId().equals(id) ) {
                                ext.getArtifacts().remove(a);
                                break;
                            }
                        }
                    }
                    break;
                }
            }
        }
    }
}
