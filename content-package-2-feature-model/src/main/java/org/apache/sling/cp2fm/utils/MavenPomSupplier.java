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
package org.apache.sling.cp2fm.utils;

import java.io.IOException;
import java.io.StringWriter;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MavenPomSupplier {

    private static final Logger LOGGER = LoggerFactory.getLogger(MavenPomSupplier.class);

    /**
     * Hidden constructor, this class can not be directly instantiated.
     */
    private MavenPomSupplier() {
        // do nothing
    }

    public static String generatePom(String groupId,
                                     String artifactId,
                                     String version,
                                     String type) throws IOException {
        LOGGER.info("Creating synthetic POM file for bundle [{}:{}:{}:{}]",
                    groupId,
                    artifactId,
                    version,
                    type);

        Model model = new Model();
        model.setGroupId(groupId);
        model.setArtifactId(artifactId);
        model.setVersion(version);
        model.setPackaging(type);

        try (StringWriter stringWriter = new StringWriter()) {
            new MavenXpp3Writer().write(stringWriter, model);
            return stringWriter.toString();
        }
    }

}
