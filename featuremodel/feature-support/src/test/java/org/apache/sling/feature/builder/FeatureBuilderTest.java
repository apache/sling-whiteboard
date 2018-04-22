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
package org.apache.sling.feature.builder;

import org.apache.felix.utils.resource.CapabilityImpl;
import org.apache.felix.utils.resource.RequirementImpl;
import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Extension;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.Include;
import org.junit.Test;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class FeatureBuilderTest {

    private static final Map<String, Feature> FEATURES = new HashMap<>();

    static {
        final Feature f1 = new Feature(ArtifactId.parse("g/a/1"));

        f1.getFrameworkProperties().put("foo", "2");
        f1.getFrameworkProperties().put("bar", "X");

        f1.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/foo-bar/4.5.6", 3));
        f1.getBundles().add(BuilderUtilTest.createBundle("group/testnewversion_low/2", 5));
        f1.getBundles().add(BuilderUtilTest.createBundle("group/testnewversion_high/2", 5));
        f1.getBundles().add(BuilderUtilTest.createBundle("group/testnewstartlevel/1", 5));
        f1.getBundles().add(BuilderUtilTest.createBundle("group/testnewstartlevelandversion/1", 5));

        final Configuration c1 = new Configuration("org.apache.sling.foo");
        c1.getProperties().put("prop", "value");
        f1.getConfigurations().add(c1);

        FEATURES.put(f1.getId().toMvnId(), f1);
    }

    private final FeatureProvider provider = new FeatureProvider() {

        @Override
        public Feature provide(final ArtifactId id) {
            return FEATURES.get(id.getGroupId() + ":" + id.getArtifactId() + ":" + id.getVersion());
        }
    };

    private List<Map.Entry<Integer, Artifact>> getBundles(final Feature f) {
        final List<Map.Entry<Integer, Artifact>> result = new ArrayList<>();
        for(final Map.Entry<Integer, List<Artifact>> entry : f.getBundles().getBundlesByStartOrder().entrySet()) {
            for(final Artifact artifact : entry.getValue()) {
                result.add(new Map.Entry<Integer, Artifact>() {

                    @Override
                    public Integer getKey() {
                        return entry.getKey();
                    }

                    @Override
                    public Artifact getValue() {
                        return artifact;
                    }

                    @Override
                    public Artifact setValue(Artifact value) {
                        return null;
                    }
                });
            }
        }

        return result;
    }

    private void equals(final Feature expected, final Feature actuals) {
        assertFalse(expected.isAssembled());
        assertTrue(actuals.isAssembled());

        assertEquals(expected.getId(), actuals.getId());
        assertEquals(expected.getTitle(), actuals.getTitle());
        assertEquals(expected.getDescription(), actuals.getDescription());
        assertEquals(expected.getVendor(), actuals.getVendor());
        assertEquals(expected.getLicense(), actuals.getLicense());

        // bundles
        final List<Map.Entry<Integer, Artifact>> expectedBundles = getBundles(expected);
        final List<Map.Entry<Integer, Artifact>> actualsBundles = getBundles(actuals);
        assertEquals(expectedBundles.size(), actualsBundles.size());
        for(final Map.Entry<Integer, Artifact> entry : expectedBundles) {
            boolean found = false;
            for(final Map.Entry<Integer, Artifact> inner : actualsBundles) {
                if ( inner.getValue().getId().equals(entry.getValue().getId()) ) {
                    found = true;
                    assertEquals("Startlevel of bundle " + entry.getValue(), entry.getKey(), inner.getKey());
                    assertEquals("Metadata of bundle " + entry.getValue(), entry.getValue().getMetadata(), inner.getValue().getMetadata());
                    break;
                }
            }
            assertTrue("Bundle " + entry.getValue() + " in level " + entry.getKey(), found);
        }

        // configurations
        assertEquals(expected.getConfigurations().size(), actuals.getConfigurations().size());
        for(final Configuration cfg : expected.getConfigurations()) {
            final Configuration found = (cfg.isFactoryConfiguration() ? actuals.getConfigurations().getFactoryConfiguration(cfg.getFactoryPid(), cfg.getName())
                                                                      : actuals.getConfigurations().getConfiguration(cfg.getPid()));
            assertNotNull("Configuration " + cfg, found);
            assertEquals("Configuration " + cfg, cfg.getProperties(), found.getProperties());
        }

        // frameworkProperties
        assertEquals(expected.getFrameworkProperties(), actuals.getFrameworkProperties());

        // requirements
        assertEquals(expected.getRequirements().size(), actuals.getRequirements().size());
        for(final Requirement r : expected.getRequirements()) {
            boolean found = false;
            for(final Requirement i : actuals.getRequirements()) {
                if ( r.equals(i) ) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }

        // capabilities
        assertEquals(expected.getCapabilities().size(), actuals.getCapabilities().size());
        for(final Capability r : expected.getCapabilities()) {
            boolean found = false;
            for(final Capability i : actuals.getCapabilities()) {
                if ( r.equals(i) ) {
                    found = true;
                    break;
                }
            }
            assertTrue(found);
        }

        // extensions
        assertEquals(expected.getExtensions().size(), actuals.getExtensions().size());
        for(final Extension ext : expected.getExtensions()) {
            final Extension inner = actuals.getExtensions().getByName(ext.getName());
            assertNotNull(inner);
            assertEquals(ext.getType(), inner.getType());
            switch ( ext.getType()) {
                case JSON : assertEquals(ext.getJSON(), inner.getJSON());
                            break;
                case TEXT : assertEquals(ext.getText(), inner.getText());
                            break;
                case ARTIFACTS : assertEquals(ext.getArtifacts().size(), inner.getArtifacts().size());
                                 for(final Artifact art : ext.getArtifacts()) {
                                     boolean found = false;
                                     for(final Artifact i : inner.getArtifacts()) {
                                         if ( art.getId().equals(i.getId()) ) {
                                             found = true;
                                             assertEquals(art.getMetadata(), i.getMetadata());
                                             break;
                                         }
                                     }
                                     assertTrue(found);
                                 }
            }
        }

        // includes should always be empty
        assertTrue(actuals.getIncludes().isEmpty());
    }

    @Test public void testNoIncludesNoUpgrade() throws Exception {
        final Feature base = new Feature(ArtifactId.parse("org.apache.sling/test-feature/1.1"));

        final Requirement r1 = new RequirementImpl(null, "osgi.contract",
                Collections.singletonMap("filter", "(&(osgi.contract=JavaServlet)(version=3.1))"), null);
        base.getRequirements().add(r1);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("osgi.implementation", "osgi.http");
        attrs.put("version:Version", "1.1");
        final Capability c1 = new CapabilityImpl(null, "osgi.implementation",
                Collections.singletonMap("uses", "javax.servlet,javax.servlet.http,org.osgi.service.http.context,org.osgi.service.http.whiteboard"),
                attrs);
        base.getCapabilities().add(c1);
        final Capability c2 = new CapabilityImpl(null, "osgi.service",
                Collections.singletonMap("uses", "org.osgi.service.http.runtime,org.osgi.service.http.runtime.dto"),
                Collections.singletonMap("objectClass:List<String>", "org.osgi.service.http.runtime.HttpServiceRuntime"));
        base.getCapabilities().add(c2);

        base.getFrameworkProperties().put("foo", "1");
        base.getFrameworkProperties().put("brave", "something");
        base.getFrameworkProperties().put("org.apache.felix.scr.directory", "launchpad/scr");

        final Artifact a1 = new Artifact(ArtifactId.parse("org.apache.sling/oak-server/1.0.0"));
        a1.getMetadata().put(Artifact.KEY_START_ORDER, "1");
        a1.getMetadata().put("hash", "4632463464363646436");
        base.getBundles().add(a1);
        base.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/application-bundle/2.0.0", 1));
        base.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/another-bundle/2.1.0", 1));
        base.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/foo-xyz/1.2.3", 2));

        final Configuration co1 = new Configuration("my.pid");
        co1.getProperties().put("foo", 5L);
        co1.getProperties().put("bar", "test");
        co1.getProperties().put("number", 7);
        base.getConfigurations().add(co1);

        final Configuration co2 = new Configuration("my.factory.pid", "name");
        co2.getProperties().put("a.value", "yeah");
        base.getConfigurations().add(co2);

        assertFalse(base.isAssembled());

        final Feature assembled = FeatureBuilder.assemble(base, new BuilderContext(provider));

        equals(base, assembled);
    }

    @Test public void testSingleInclude() throws Exception {
        final Feature base = new Feature(ArtifactId.parse("org.apache.sling/test-feature/1.1"));
        final Include i1 = new Include(ArtifactId.parse("g/a/1"));
        base.getIncludes().add(i1);

        final Requirement r1 = new RequirementImpl(null, "osgi.contract",
                Collections.singletonMap("filter", "(&(osgi.contract=JavaServlet)(version=3.1))"), null);
        base.getRequirements().add(r1);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("osgi.implementation", "osgi.http");
        attrs.put("version:Version", "1.1");
        final Capability c1 = new CapabilityImpl(null, "osgi.implementation",
                Collections.singletonMap("uses", "javax.servlet,javax.servlet.http,org.osgi.service.http.context,org.osgi.service.http.whiteboard"),
                attrs);
        base.getCapabilities().add(c1);

        base.getFrameworkProperties().put("foo", "1");
        base.getFrameworkProperties().put("brave", "something");
        base.getFrameworkProperties().put("org.apache.felix.scr.directory", "launchpad/scr");

        final Artifact a1 = new Artifact(ArtifactId.parse("org.apache.sling/oak-server/1.0.0"));
        a1.getMetadata().put(Artifact.KEY_START_ORDER, "1");
        a1.getMetadata().put("hash", "4632463464363646436");
        base.getBundles().add(a1);
        base.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/application-bundle/2.0.0", 1));
        base.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/another-bundle/2.1.0", 1));
        base.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/foo-xyz/1.2.3", 2));
        base.getBundles().add(BuilderUtilTest.createBundle("group/testnewversion_low/1", 5));
        base.getBundles().add(BuilderUtilTest.createBundle("group/testnewversion_high/5", 5));
        base.getBundles().add(BuilderUtilTest.createBundle("group/testnewstartlevel/1", 10));
        base.getBundles().add(BuilderUtilTest.createBundle("group/testnewstartlevelandversion/2", 10));

        final Configuration co1 = new Configuration("my.pid");
        co1.getProperties().put("foo", 5L);
        co1.getProperties().put("bar", "test");
        co1.getProperties().put("number", 7);
        base.getConfigurations().add(co1);

        final Configuration co2 = new Configuration("my.factory.pid", "name");
        co2.getProperties().put("a.value", "yeah");
        base.getConfigurations().add(co2);

        assertFalse(base.isAssembled());

        // create the expected result
        final Feature result = base.copy();
        result.getIncludes().remove(0);
        result.getFrameworkProperties().put("bar", "X");
        result.getBundles().add(BuilderUtilTest.createBundle("org.apache.sling/foo-bar/4.5.6", 3));
        final Configuration co3 = new Configuration("org.apache.sling.foo");
        co3.getProperties().put("prop", "value");
        result.getConfigurations().add(co3);

        // assemble
        final Feature assembled = FeatureBuilder.assemble(base, new BuilderContext(provider));

        // and test
        equals(result, assembled);
    }
}
