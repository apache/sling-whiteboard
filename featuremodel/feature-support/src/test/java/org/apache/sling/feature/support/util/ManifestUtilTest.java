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
package org.apache.sling.feature.support.util;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.jar.Manifest;

import org.junit.Test;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class ManifestUtilTest {

    private void assertPackageInfo(final String name, final Version version, final PackageInfo info) {
        assertEquals(name, info.getName());
        assertEquals(version, info.getPackageVersion());
    }

    @Test public void testExportPackage() throws Exception {
        final Manifest mf = new Manifest();
        mf.getMainAttributes().putValue(Constants.EXPORT_PACKAGE, "org.apache.sling;version=1.0,org.apache.felix;version=2.0");
        final List<PackageInfo> infos = ManifestUtil.extractExportedPackages(mf);
        assertEquals(2, infos.size());
        assertPackageInfo("org.apache.sling", Version.parseVersion("1.0"), infos.get(0));
        assertPackageInfo("org.apache.felix", Version.parseVersion("2.0"), infos.get(1));
    }
}
