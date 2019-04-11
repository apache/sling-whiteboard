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
package org.apache.sling.feature.diff.io.json;

import static org.apache.sling.feature.diff.FeatureDiff.compareFeatures;
import static org.junit.Assert.assertEquals;

import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import org.apache.sling.feature.Artifact;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Configuration;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.apiregions.model.ApiRegion;
import org.apache.sling.feature.apiregions.model.ApiRegions;
import org.apache.sling.feature.apiregions.model.io.json.ApiRegionsJSONSerializer;
import org.apache.sling.feature.diff.FeatureDiff;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;

public class FeatureDiffJSONSerializerTest {

    @Test(expected = NullPointerException.class)
    public void nullOutputStreamNotAccepted() {
        FeatureDiffJSONSerializer.serializeFeatureDiff(null, (OutputStream) null);
    }

    @Test(expected = NullPointerException.class)
    public void nullFeatureDiffNotAccepted() {
        FeatureDiffJSONSerializer.serializeFeatureDiff(null, (Writer) null);
    }

    @Test(expected = NullPointerException.class)
    public void nullWriterNotAccepted() {
        Feature previous = new Feature(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:0.9.0"));
        Feature current = new Feature(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:1.0.0"));
        FeatureDiff featureDiff = compareFeatures(previous, current);

        FeatureDiffJSONSerializer.serializeFeatureDiff(featureDiff , (Writer) null);
    }

    @Test
    public void serialization() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        // the expected result

        JsonNode expectedNode = objectMapper.readTree(getClass().getResourceAsStream("expectedDiff.json"));

        // define the previous Feature

        Feature previous = new Feature(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:0.9.0"));
        previous.getFrameworkProperties().put("env", "staging");
        previous.getFrameworkProperties().put("sling.framework.install.incremental", "true");
        previous.getBundles().add(new Artifact(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:removed:1.0.0")));
        previous.getBundles().add(new Artifact(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:updated:1.0.0")));
        previous.getConfigurations().add(new Configuration("org.apache.sling.feature.diff.config.removed"));

        Configuration previousConfiguration = new Configuration("org.apache.sling.feature.diff.config.updated");
        previousConfiguration.getProperties().put("it.will.appear.in.the.removed.section", 123);
        previousConfiguration.getProperties().put("it.will.appear.in.the.updated.section", new String[] { "/log" });
        previous.getConfigurations().add(previousConfiguration);

        ApiRegions previousRegions = new ApiRegions();
        ApiRegion base = previousRegions.addNew("base");
        base.add("org.apache.felix.inventory");
        base.add("org.apache.felix.metatype");

        ApiRegionsJSONSerializer.serializeApiRegions(previousRegions, previous);

        // define the current Feature

        Feature current = new Feature(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:1.0.0"));
        current.getFrameworkProperties().put("env", "prod");
        current.getFrameworkProperties().put("sling.framework.install.startlevel", "1");
        current.getBundles().add(new Artifact(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:added:1.0.0")));
        current.getBundles().add(new Artifact(ArtifactId.fromMvnId("org.apache.sling:org.apache.sling.feature.diff:updated:2.0.0")));
        current.getConfigurations().add(new Configuration("org.apache.sling.feature.diff.config.added"));

        Configuration currentConfiguration = new Configuration("org.apache.sling.feature.diff.config.updated");
        currentConfiguration.getProperties().put("it.will.appear.in.the.updated.section", new String[] { "/log", "/etc" });
        currentConfiguration.getProperties().put("it.will.appear.in.the.added.section", true);
        current.getConfigurations().add(currentConfiguration);

        ApiRegions currentRegions = new ApiRegions();
        base = currentRegions.addNew("base");
        base.add("org.apache.felix.inventory");
        base.add("org.apache.felix.scr.component");

        ApiRegionsJSONSerializer.serializeApiRegions(currentRegions, current);

        // now compare

        FeatureDiff featureDiff = compareFeatures(previous, current);

        StringWriter stringWriter = new StringWriter();
        FeatureDiffJSONSerializer.serializeFeatureDiff(featureDiff , stringWriter);

        JsonNode actualNode = objectMapper.readTree(stringWriter.toString());

        // assert the differences
        JsonNode patchNode = JsonDiff.asJson(expectedNode, actualNode);
        // expected 1 node as diff, i.e. [{"op":"replace","path":"/generatedOn","value":"2019-04-04T12:38:46 +0200"}]
        assertEquals(patchNode.toString(), 1, patchNode.size());
    }

}
