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
package org.apache.sling.jcr.repositorymaintenance.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.jcr.RepositoryException;

import org.apache.sling.jcr.repositorymaintenance.VersionCleanupPathConfig;
import org.junit.Test;

public class VersionCleanupPathTest {

    private VersionCleanupPath simpleCreate(String path) {
        return new VersionCleanupPath(new VersionCleanupPathConfig() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public boolean keepVersions() {
                return false;
            }

            @Override
            public int limit() {
                return 5;
            }

            @Override
            public String path() {
                return path;
            }

        });
    }

    @Test
    public void testNotFound() {
        try {
            VersionCleanupPath.getMatchingConfiguration(Collections.emptyList(), "/");
            fail();
        } catch (RepositoryException re) {
            // expected
        }

        try {
            VersionCleanupPath.getMatchingConfiguration(Collections.singletonList(simpleCreate("/subpath")), "/");
            fail();
        } catch (RepositoryException re) {
            // expected
        }
    }

    @Test
    public void testSorting() throws RepositoryException {
        List<VersionCleanupPath> configs = new ArrayList<>();
        configs.add(simpleCreate("/content"));
        configs.add(simpleCreate("/content/content2"));
        Collections.sort(configs);

        assertEquals("/content/content2,/content",
                configs.stream().map(VersionCleanupPath::getPath).collect(Collectors.joining(",")));
        assertEquals("/content/content2",
                VersionCleanupPath.getMatchingConfiguration(configs, "/content/content2/subitem").getPath());
        assertEquals("/content",
                VersionCleanupPath.getMatchingConfiguration(configs, "/content/content3/subitem").getPath());

    }

}