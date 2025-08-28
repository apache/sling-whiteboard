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
package org.apache.sling.microsling.request.helpers;

import javax.jcr.Item;

import junit.framework.TestCase;

import org.apache.sling.microsling.api.Resource;
import org.apache.sling.microsling.api.ResourceMetadata;
import org.apache.sling.microsling.api.SlingRequestPathInfo;

/** Test the MicroslingRequestPathInfo */
public class MicroslingRequestPathInfoTest extends TestCase {

    static class MockResource implements Resource {
        
        private final ResourceMetadata metadata;
        
        MockResource(String resolutionPath) {
            metadata = new ResourceMetadata();
            metadata.put(ResourceMetadata.RESOLUTION_PATH, resolutionPath);
        }

        public Object getData() {
            throw new Error("MockResource does not implement this method");
        }

        public Item getItem() {
            throw new Error("MockResource does not implement this method");
        }

        public String getResourceType() {
            throw new Error("MockResource does not implement this method");
        }

        public String getURI() {
            throw new Error("MockResource does not implement this method");
        }
        
        public ResourceMetadata getMetadata() {
            return metadata;
        }
        
    }
    
    public void testSimplePath() {
        SlingRequestPathInfo p = new MicroslingRequestPathInfo(new MockResource(""),"/some/path.print.a4.html/some/suffix");
        assertEquals("/some/path",p.getUnusedContentPath());
        assertEquals("",p.getResourcePath());
    }
    
    public void testNullResource() {
        SlingRequestPathInfo p = new MicroslingRequestPathInfo(null,"/some/path.print.a4.html/some/suffix");
        assertEquals("/some/path",p.getUnusedContentPath());
        assertEquals(null,p.getResourcePath());
    }
    
    public void testSimpleSuffix() {
        SlingRequestPathInfo p = new MicroslingRequestPathInfo(new MockResource(""),"/some/path.print.a4.html/some/suffix");
        assertEquals("/some/suffix",p.getSuffix());
    }
    
    public void testSimpleSelectorString() {
        SlingRequestPathInfo p = new MicroslingRequestPathInfo(new MockResource(""),"/some/path.print.a4.html/some/suffix");
        assertEquals("print.a4",p.getSelectorString());
    }
    
    public void testSimpleExtension() {
        SlingRequestPathInfo p = new MicroslingRequestPathInfo(new MockResource(""),"/some/path.print.a4.html/some/suffix");
        assertEquals("html",p.getExtension());
    }
    
    public void testAllOptions() {
        SlingRequestPathInfo p = new MicroslingRequestPathInfo(new MockResource(""),"/some/path.print.a4.html/some/suffix");
        assertEquals("/some/path",p.getUnusedContentPath());
        assertEquals("print.a4",p.getSelectorString());
        assertEquals("html",p.getExtension());
        assertEquals("/some/suffix",p.getSuffix());
    }
    
    public void testAllEmpty() {
        SlingRequestPathInfo p = new MicroslingRequestPathInfo(new MockResource(""),null);
        assertEquals("",p.getUnusedContentPath());
        assertEquals("",p.getSelectorString());
        assertEquals("",p.getExtension());
        assertEquals("",p.getSuffix());
    }
    
    public void testPathOnly() {
        SlingRequestPathInfo p = new MicroslingRequestPathInfo(new MockResource(""),"/some/path/here");
        assertEquals("/some/path/here",p.getUnusedContentPath());
        assertEquals("",p.getSelectorString());
        assertEquals("",p.getExtension());
        assertEquals("",p.getSuffix());
    }
    
    public void testPathAndExtensionOnly() {
        SlingRequestPathInfo p = new MicroslingRequestPathInfo(new MockResource(""),"/some/path/here.html");
        assertEquals("/some/path/here",p.getUnusedContentPath());
        assertEquals("",p.getSelectorString());
        assertEquals("html",p.getExtension());
        assertEquals("",p.getSuffix());
    }
    
    public void testPathAndOneSelectorOnly() {
        SlingRequestPathInfo p = new MicroslingRequestPathInfo(new MockResource(""),"/some/path/here.print.html");
        assertEquals("/some/path/here",p.getUnusedContentPath());
        assertEquals("print",p.getSelectorString());
        assertEquals("html",p.getExtension());
        assertEquals("",p.getSuffix());
    }
    
    public void testPathExtAndSuffix() {
        SlingRequestPathInfo p = new MicroslingRequestPathInfo(new MockResource(""),"/some/path/here.html/something");
        assertEquals("/some/path/here",p.getUnusedContentPath());
        assertEquals("",p.getSelectorString());
        assertEquals("html",p.getExtension());
        assertEquals("/something",p.getSuffix());
    }
    
    public void testSelectorsSplit() {
        SlingRequestPathInfo p = new MicroslingRequestPathInfo(new MockResource(""),"/some/path.print.a4.html/some/suffix");
        assertEquals(2,p.getSelectors().length);
        assertEquals("print",p.getSelector(0));
        assertEquals("a4",p.getSelector(1));
    }
    
    public void testPartialResolutionA() {
        SlingRequestPathInfo p = new MicroslingRequestPathInfo(new MockResource("/some"),"/some/path.print.a4.html/some/suffix");
        assertEquals("/some",p.getResourcePath());
        assertEquals("print.a4",p.getSelectorString());
        assertEquals("html",p.getExtension());
        assertEquals("/some/suffix",p.getSuffix());
        assertEquals("/path",p.getUnusedContentPath());
    }
    
    public void testPartialResolutionB() {
        SlingRequestPathInfo p = new MicroslingRequestPathInfo(new MockResource("/some/path"),"/some/path.print.a4.html/some/suffix");
        assertEquals("print.a4",p.getSelectorString());
        assertEquals("html",p.getExtension());
        assertEquals("/some/suffix",p.getSuffix());
        assertEquals("",p.getUnusedContentPath());
    }
    
    public void testPartialResolutionC() {
        SlingRequestPathInfo p = new MicroslingRequestPathInfo(new MockResource("/some/path.print"),"/some/path.print.a4.html/some/suffix");
        assertEquals("a4",p.getSelectorString());
        assertEquals("html",p.getExtension());
        assertEquals("/some/suffix",p.getSuffix());
        assertEquals("",p.getUnusedContentPath());
    }
    
    public void testPartialResolutionD() {
        SlingRequestPathInfo p = new MicroslingRequestPathInfo(new MockResource("/some/path.print.a"),"/some/path.print.a4.html/some/suffix");
        assertEquals("",p.getSelectorString());
        assertEquals("html",p.getExtension());
        assertEquals("/some/suffix",p.getSuffix());
        assertEquals("4",p.getUnusedContentPath());
    }
    
    public void testPartialResolutionE() {
        SlingRequestPathInfo p = new MicroslingRequestPathInfo(new MockResource("/some/path.print.a4.html"),"/some/path.print.a4.html/some/suffix");
        assertEquals("",p.getSelectorString());
        assertEquals("",p.getExtension());
        assertEquals("",p.getSuffix());
        assertEquals("/some/suffix",p.getUnusedContentPath());
    }
    
}
