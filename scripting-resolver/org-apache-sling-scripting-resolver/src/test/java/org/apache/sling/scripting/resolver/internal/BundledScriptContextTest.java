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

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.SimpleBindings;

import org.apache.sling.api.scripting.SlingScriptConstants;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class BundledScriptContextTest {

    @Test
    public void testSetAndGetBindings() {
        BundledScriptContext bundledScriptContext = new BundledScriptContext();
        Bindings engineScope = new SimpleBindings();
        Bindings globalScope = new SimpleBindings();
        Bindings slingScope = new SimpleBindings();

        bundledScriptContext.setBindings(slingScope, SlingScriptConstants.SLING_SCOPE);
        bundledScriptContext.setBindings(globalScope, ScriptContext.GLOBAL_SCOPE);
        bundledScriptContext.setBindings(engineScope, ScriptContext.ENGINE_SCOPE);

        assertEquals(engineScope, bundledScriptContext.getBindings(ScriptContext.ENGINE_SCOPE));
        assertEquals(globalScope, bundledScriptContext.getBindings(ScriptContext.GLOBAL_SCOPE));
        assertEquals(slingScope, bundledScriptContext.getBindings(SlingScriptConstants.SLING_SCOPE));

        boolean invalidScopeThrowsException;
        try {
            bundledScriptContext.getBindings(Integer.MIN_VALUE);
            invalidScopeThrowsException = false;
        } catch (Exception e) {
            invalidScopeThrowsException = true;
        }
        assertTrue(BundledScriptContext.class.getName() + " should throw exceptions for invalid scopes.", invalidScopeThrowsException);

        boolean acceptsAnyScope;
        try {
            bundledScriptContext.setBindings(new SimpleBindings(), Integer.MIN_VALUE);
            acceptsAnyScope = true;
        } catch (Exception e) {
            acceptsAnyScope = false;
        }
        assertFalse(BundledScriptContext.class.getName() + " should not accept bindings for arbitrary scopes.", acceptsAnyScope);

        boolean acceptsNullBindings;
        try {
            bundledScriptContext.setBindings(null, ScriptContext.ENGINE_SCOPE);
            acceptsNullBindings = true;
        } catch (Exception e) {
            acceptsNullBindings = false;
        }
        assertFalse(BundledScriptContext.class.getName() + " should not accept null bindings.", acceptsNullBindings);
    }

    @Test
    public void testSetAndGetAttribute() {
        BundledScriptContext bundledScriptContext = new BundledScriptContext();
        boolean acceptsSettingNullAttributeNames;
        try {
            bundledScriptContext.setAttribute(null, 0, ScriptContext.ENGINE_SCOPE);
            acceptsSettingNullAttributeNames = true;
        } catch (Exception e) {
            acceptsSettingNullAttributeNames = false;
        }
        assertFalse(BundledScriptContext.class.getName() + " should not accept null attribute names.", acceptsSettingNullAttributeNames);

        boolean acceptsRetrievingNullAttributeNamesFromScope;
        try {
            bundledScriptContext.getAttribute(null, ScriptContext.ENGINE_SCOPE);
            acceptsRetrievingNullAttributeNamesFromScope = true;
        } catch (Exception e) {
            acceptsRetrievingNullAttributeNamesFromScope = false;
        }
        assertFalse(BundledScriptContext.class.getName() + " should not accept null attribute names.", acceptsRetrievingNullAttributeNamesFromScope);

        boolean acceptsRetrievingNullAttributeNames;
        try {
            bundledScriptContext.getAttribute(null);
            acceptsRetrievingNullAttributeNames = true;
        } catch (Exception e) {
            acceptsRetrievingNullAttributeNames = false;
        }
        assertFalse(BundledScriptContext.class.getName() + " should not accept null attribute names.", acceptsRetrievingNullAttributeNames);

        bundledScriptContext.setAttribute("nothing", 0, ScriptContext.ENGINE_SCOPE);
        assertEquals(0, bundledScriptContext.getAttribute("nothing"));
        assertEquals(0, bundledScriptContext.getAttribute("nothing", ScriptContext.ENGINE_SCOPE));
        assertNull(bundledScriptContext.getAttribute("nothing", ScriptContext.GLOBAL_SCOPE));
    }

    @Test
    public void testRemoveAttribute() {
        BundledScriptContext bundledScriptContext = new BundledScriptContext();
        boolean acceptsNullAttributeNames;
        try {
            bundledScriptContext.removeAttribute(null, ScriptContext.ENGINE_SCOPE);
            acceptsNullAttributeNames = true;
        } catch (Exception e) {
            acceptsNullAttributeNames = false;
        }
        assertFalse(BundledScriptContext.class.getName() + " should not accept null attribute names.", acceptsNullAttributeNames);

        bundledScriptContext.setAttribute("nothing", 0, ScriptContext.ENGINE_SCOPE);
        assertEquals(0, bundledScriptContext.removeAttribute("nothing", ScriptContext.ENGINE_SCOPE));
        Bindings engineScope = bundledScriptContext.getBindings(ScriptContext.ENGINE_SCOPE);
        assertTrue( engineScope != null && engineScope.size() == 0);
        assertNull(bundledScriptContext.removeAttribute("nothing", ScriptContext.ENGINE_SCOPE));
        assertNull(bundledScriptContext.removeAttribute("nothing", ScriptContext.GLOBAL_SCOPE));
    }

    @Test
    public void testGetAttributesScope() {
        BundledScriptContext bundledScriptContext = new BundledScriptContext();
        boolean acceptsNullAttributeNames;
        try {
            bundledScriptContext.getAttributesScope(null);
            acceptsNullAttributeNames = true;
        } catch (Exception e) {
            acceptsNullAttributeNames = false;
        }
        assertFalse(BundledScriptContext.class.getName() + " should not accept null attribute names.", acceptsNullAttributeNames);

        bundledScriptContext.setAttribute("nothing", 0, SlingScriptConstants.SLING_SCOPE);
        assertEquals(SlingScriptConstants.SLING_SCOPE, bundledScriptContext.getAttributesScope("nothing"));
        assertEquals(-1, bundledScriptContext.getAttributesScope("nothing here"));
    }

}
