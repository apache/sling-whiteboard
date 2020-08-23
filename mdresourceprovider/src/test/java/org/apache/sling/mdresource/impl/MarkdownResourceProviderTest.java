/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.mdresource.impl;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.junit.SlingContextBuilder;
import org.junit.Rule;
import org.junit.Test;

public class MarkdownResourceProviderTest {

    @Rule
    public SlingContext context = 
        new SlingContextBuilder(ResourceResolverType.JCR_MOCK)
            .plugin(new RegisterMarkdownResourcePlugin()).build();

    @Test
    public void loadSingleResourceFromRoot() {

        Resource resource = context.resourceResolver().getResource("/md-test");
        assertThat("resource", resource, notNullValue());
        assertThat("resource.getValueMap()", resource.getValueMap(), notNullValue());
        assertThat("resource.getMetadata()", resource.getResourceMetadata(), notNullValue());
        assertThat("resource.getMetadata().getPath()", resource.getResourceMetadata().getResolutionPath(), equalTo("/md-test2"));
        
        assertThat("valueMap[jcr:title]", resource.getValueMap().get("jcr:title", String.class), equalTo("Simple markdown file"));
        assertThat("valueMap[jcr:description]", resource.getValueMap().get("jcr:description", String.class),
            equalTo("<p>This is an example of a simple markdown file</p>\n"));
        assertThat("valueMap[sling:resourceType]", resource.getValueMap().get("sling:resourceType", String.class),
            equalTo("sling/markdown/file"));
        assertThat("valueMap[author]", resource.getValueMap().get("author", String.class), equalTo("John Doe"));
        assertThat("valueMap[keywords]", resource.getValueMap().get("keywords", String[].class), equalTo(new String[] {"news", "simple"}));

        ValueMap adapted = resource.adaptTo(ValueMap.class);
        assertThat("adapted ValueMap", adapted, notNullValue());
        assertThat("adapted Map", resource.adaptTo(Map.class), notNullValue());
        
        // TODO - more valueMap tests
    }
    
    @Test
    public void childResourceIsListed() {
        
        Resource root = context.resourceResolver().getResource("/");
        Iterator<Resource> children = context.resourceResolver().listChildren(root);
        List<String> paths = new ArrayList<>();
        while ( children.hasNext() ) {
            paths.add(children.next().getPath());
        }

        assertThat(paths, hasItem("/md-test"));
    }
    
    @Test
    public void listChildren() {
        
        Resource resource = context.resourceResolver().getResource("/md-test/child-listing");
        
        List<Resource> children = new ArrayList<>();
        resource.getChildren().forEach( children::add );
        
        assertThat("children", children, hasSize(3));
        assertThat("children.paths", 
                children.stream().map( r -> r.getPath()).collect(Collectors.toList()), 
                hasItems("/md-test/child-listing/news", "/md-test/child-listing/spam", "/md-test/child-listing/comments"));

        Resource newsResource = children.stream()
                .filter( r -> r.getPath().equals("/md-test/child-listing/spam") )
                .findFirst()
                .get();
        
        assertThat("newsResource.valueMap[jcr:title]", newsResource.getValueMap().get("jcr:title", String.class), equalTo("Spam (correct)"));
        
    }
    
    // TODO - more tests
    // - file with just a title
    // - empty file
    // - missing file
    // - file with just a description
    // - listing children ( files and folders )
    // - listing children ( folders overrides file ( log warning ?)
}
