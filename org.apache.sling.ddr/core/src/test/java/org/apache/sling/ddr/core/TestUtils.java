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
package org.apache.sling.ddr.core;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TestUtils {

    /**
     * Gets the List of All Resources from the given Resource Provider of the given Parent
     *
     * @param provider Resource Provider from which the list of children is obtained from
     * @param resolveContext Resolve Context to be used
     * @param parent Parent Resource from which children are returned
     * @return List of Children or empty if none are found
     */
    public static List<Resource> getResourcesFromProvider(
        ResourceProvider provider, ResolveContext resolveContext, Resource parent
    ) {
        List<Resource> answer = new ArrayList<>();
        // List all the children and make sure that only one is returned
        Iterator<Resource> i = provider.listChildren(
            resolveContext, parent
        );
        if(i != null) {
            while (i.hasNext()) {
                answer.add(i.next());
            }
        }
        return answer;
    }

    /**
     * Filters out matching or not matching resources from the given list
     *
     * @param resources List of resources to be filtered
     * @param matching If true only matching resources are returned otherwise only not matching resources
     * @param names All the names that we want to filter out or remove
     * @return Returns a list of all resources that are either in the list of names or not. This is a copy of the
     *         given list of resources
     */
    public static List<Resource> filterResourceByName(List<Resource> resources, boolean matching, String ... names) {
        List<Resource> answer = new ArrayList<>();
        for(Resource resource: resources) {
            if(names != null) {
                boolean found = false;
                for (String childName : names) {
                    if (resource.getName().equals(childName)) {
                        found = true;
                    }
                }
                if(matching == found) {
                    answer.add(resource);
                }
            }
        }
        return answer;
    }
}
