/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.scripting.resolver.internal;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.scripting.resolver.BundledScriptFinder;
import org.apache.sling.scripting.resolver.Script;
import org.osgi.framework.Bundle;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(
        service = BundledScriptFinder.class
)
public class BundledScriptFinderImpl implements BundledScriptFinder {

    private List<String> scriptEngineExtensions;

    @Reference
    private ScriptEngineManager scriptEngineManager;

    @Activate
    private void activate() {
        List<String> _scriptEngineExtensions = new ArrayList<>();
        for (ScriptEngineFactory factory : scriptEngineManager.getEngineFactories()) {
            _scriptEngineExtensions.addAll(factory.getExtensions());
        }
        Collections.reverse(_scriptEngineExtensions);
        scriptEngineExtensions = Collections.unmodifiableList(_scriptEngineExtensions);
    }

    @Override
    public Script getScript(SlingHttpServletRequest request, Bundle bundle) throws IOException {
        String resourceType = request.getResource().getResourceType();
        String type;
        String version = null;
        if (resourceType.contains("/") && StringUtils.countMatches(resourceType, "/") == 1) {
            type = resourceType.substring(0, resourceType.indexOf("/"));
            version = resourceType.substring(resourceType.indexOf("/") + 1, resourceType.length());
        } else {
            type = resourceType;
        }
        String scriptName = version == null ? resourceType.substring(resourceType.lastIndexOf('.') + 1) : type.substring(type.lastIndexOf
                ('.') + 1);
        String scriptPath = type + (version == null ? "/" : "/" + version + "/") + scriptName;
        for (String extension : scriptEngineExtensions) {
            URL bundledScriptURL = bundle.getEntry(BundledScriptTracker.NS_JAVAX_SCRIPT_CAPABILITY + "/" + scriptPath + "." + extension);
            if (bundledScriptURL != null) {
                return new ScriptImpl(scriptEngineManager.getEngineByExtension(extension),
                        IOUtils.toString(bundledScriptURL.openStream(), StandardCharsets.UTF_8));
            }
        }
        return null;
    }
}
