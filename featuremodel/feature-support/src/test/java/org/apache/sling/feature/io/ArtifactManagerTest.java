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
package org.apache.sling.feature.io;

import org.apache.sling.feature.io.ArtifactHandler;
import org.apache.sling.feature.io.ArtifactManager;
import org.apache.sling.feature.io.ArtifactManagerConfig;
import org.apache.sling.feature.io.spi.ArtifactProvider;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ArtifactManagerTest {

    private static final String METADATA = "<metadata modelVersion=\"1.1.0\">\n" +
            "<groupId>org.apache.sling.samples</groupId>\n" +
            "<artifactId>slingshot</artifactId>\n" +
            "<version>0-DEFAULT-SNAPSHOT</version>\n" +
            "<versioning>\n" +
                "<snapshot>\n" +
                    "<timestamp>20160321.103951</timestamp>\n" +
                    "<buildNumber>1</buildNumber>\n" +
                "</snapshot>\n" +
                "<lastUpdated>20160321103951</lastUpdated>\n" +
                "<snapshotVersions>\n" +
                    "<snapshotVersion>\n" +
                        "<extension>txt</extension>\n" +
                        "<value>0-DEFAULT-20160321.103951-1</value>\n" +
                        "<updated>20160321103951</updated>\n" +
                    "</snapshotVersion>\n" +
                    "<snapshotVersion>\n" +
                        "<extension>pom</extension>\n" +
                        "<value>0-DEFAULT-20160321.103951-1</value>\n" +
                        "<updated>20160321103951</updated>\n" +
                    "</snapshotVersion>\n" +
                "</snapshotVersions>\n" +
            "</versioning></metadata>";

    @Test public void testMetadataParsing() {
        final String version = ArtifactManager.getLatestSnapshot(METADATA);
        assertEquals("20160321.103951-1", version);
    }

    @Test public void testSnapshotHandling() throws IOException {
        final String REPO = "http://org.apache.sling";
        final ArtifactManagerConfig config = mock(ArtifactManagerConfig.class);
        when(config.getRepositoryUrls()).thenReturn(new String[] {REPO});

        final File metadataFile = mock(File.class);
        when(metadataFile.exists()).thenReturn(true);
        when(metadataFile.getPath()).thenReturn("/maven-metadata.xml");

        final File artifactFile = mock(File.class);
        when(artifactFile.exists()).thenReturn(true);

        final ArtifactProvider provider = mock(ArtifactProvider.class);
        when(provider.getArtifact(REPO + "/group/artifact/1.0.0-SNAPSHOT/artifact-1.0.0-SNAPSHOT.txt", "group/artifact/1.0.0-SNAPSHOT/artifact-1.0.0-SNAPSHOT.txt")).thenReturn(null);
        when(provider.getArtifact(REPO + "/group/artifact/1.0.0-SNAPSHOT/maven-metadata.xml", "org.apache.sling/group/artifact/1.0.0-SNAPSHOT/maven-metadata.xml")).thenReturn(metadataFile);
        when(provider.getArtifact(REPO + "/group/artifact/1.0.0-SNAPSHOT/artifact-1.0.0-20160321.103951-1.txt", "group/artifact/1.0.0-SNAPSHOT/artifact-1.0.0-SNAPSHOT.txt")).thenReturn(artifactFile);

        final Map<String, ArtifactProvider> providers = new HashMap<>();
        providers.put("*", provider);

        final ArtifactManager mgr = new ArtifactManager(config, providers) {

            @Override
            protected String getFileContents(final ArtifactHandler handler) throws IOException {
                final String path = handler.getFile().getPath();
                if ( "/maven-metadata.xml".equals(path) ) {
                    return METADATA;
                }
                return super.getFileContents(handler);
            }
        };

        final ArtifactHandler handler = mgr.getArtifactHandler("mvn:group/artifact/1.0.0-SNAPSHOT/txt");
        assertNotNull(handler);
        assertEquals(artifactFile, handler.getFile());
    }
}
