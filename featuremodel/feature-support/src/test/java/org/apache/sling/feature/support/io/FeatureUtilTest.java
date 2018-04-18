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
package org.apache.sling.feature.support.io;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class FeatureUtilTest {

    @Test public void testFileSort() {
        final String[] files = new String[] {
            "/different/path/app.json",
            "/path/to/base.json",
            "/path/to/feature.json",
            "/path/to/amode/feature.json",
            "/path/to/later/feature.json",
            "http://sling.apache.org/features/one.json",
            "http://sling.apache.org/features/two.json",
            "http://sling.apache.org/features/amode/feature.json"
        };

        final List<String> l = new ArrayList<>(Arrays.asList(files));
        Collections.sort(l, FileUtils.FEATURE_PATH_COMP);
        for(int i=0; i<files.length; i++) {
            assertEquals(files[i], l.get(i));
        }
    }

}
