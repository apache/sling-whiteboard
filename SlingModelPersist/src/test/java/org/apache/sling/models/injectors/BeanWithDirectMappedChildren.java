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
 */package org.apache.sling.models.injectors;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.persistor.annotations.DirectDescendants;

/**
 * Expresses a sling model which has child nodes as a map
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class BeanWithDirectMappedChildren {

    public transient String path;

    @DirectDescendants
    @Inject
    public Map<String, Person> people = new HashMap<>();

    public void addPerson(String firstName, String lastName) {
        Person p = new Person();
        String name = lastName + '-' + firstName;
        p.firstName = firstName;
        p.lastName = lastName;
        p.path = "./" + name;
        people.put(name, p);
    }

    @Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
    public static class Person {

        transient String path;

        @Inject
        String firstName;

        @Inject
        String lastName;
    }
}
