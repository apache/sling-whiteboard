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
package org.apache.sling.scripting.maven.plugin;

import org.junit.Assert;
import org.junit.Test;

public class ScriptingMavenPluginTest
{
    @Test
    public void testScriptNameFullCalculation()
    {
        String scriptPath = "org.apache.foo/1.0.0/POST.hi.xml.jsp";

        ScriptingMavenPlugin.Script script = ScriptingMavenPlugin.getScripts(scriptPath);

        Assert.assertEquals("org.apache.foo", script.rt);
        Assert.assertEquals("1.0.0", script.version);
        Assert.assertEquals("hi", script.name);
        Assert.assertEquals("POST", script.method);
        Assert.assertEquals("xml", script.extension);
        Assert.assertEquals("jsp", script.scriptExtension);
    }

    @Test
    public void testScriptNameMinCalculation()
    {
        String scriptPath = "org.apache.foo/foo";

        ScriptingMavenPlugin.Script script = ScriptingMavenPlugin.getScripts(scriptPath);

        Assert.assertEquals("org.apache.foo", script.rt);
        Assert.assertNull("1.0.0", script.version);
        Assert.assertEquals("foo", script.name);
        Assert.assertNull(script.method);
        Assert.assertEquals("html", script.extension);
        Assert.assertNull(script.scriptExtension);
    }

    @Test
    public void testScriptNameVersionAndMethodCalculation()
    {
        String scriptPath = "org.apache.foo/1.2.0/Post.jsp";

        ScriptingMavenPlugin.Script script = ScriptingMavenPlugin.getScripts(scriptPath);

        Assert.assertEquals("org.apache.foo", script.rt);
        Assert.assertEquals("1.2.0", script.version);
        Assert.assertEquals("", script.name);
        Assert.assertEquals("POST", script.method);
        Assert.assertEquals("html", script.extension);
        Assert.assertEquals("jsp", script.scriptExtension);
    }

    @Test
    public void testScriptNameVersionAndMethodMinCalculation()
    {
        String scriptPath = "org.apache.foo/1.2.0/Post.";

        ScriptingMavenPlugin.Script script = ScriptingMavenPlugin.getScripts(scriptPath);

        Assert.assertEquals("org.apache.foo", script.rt);
        Assert.assertEquals("1.2.0", script.version);
        Assert.assertEquals("",script.name);
        Assert.assertEquals("POST", script.method);
        Assert.assertEquals("html", script.extension);
        Assert.assertNull(script.scriptExtension);
    }
}
