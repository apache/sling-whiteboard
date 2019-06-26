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
package org.apache.sling.models.persist.bean;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

/**
 * Example of a model bean with an object graph of depth 4
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ComplexBean {
    public ComplexBean() {

    }

    public ComplexBean(Resource resource) {
        if (resource != null) {
            name = resource.getName();
        }
    }

    public String name = "change-me";

    public String getPath() {
        return "/test/complex-beans/" + name;
    }

    // --- Serializable properties
    @Inject
    @Named("array-of-strings")
    public String[] arrayOfStrings = {"one", "two", "three", "four"};

    @Inject
    public Date now = new Date();

    @Inject
    public long nowLong = now.getTime();

    @Inject
    public Level2Bean level2 = new Level2Bean();

    @Model(adaptables = Resource.class)
    public static class Level2Bean {
        @Inject
        public String name = "level2";

        @Inject
        public List<Level3Bean> level3 = new ArrayList<>();
    }

    @Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
    public static class Level3Bean {
    public Level3Bean() {

    }

    public Level3Bean(Resource resource) {
        if (resource != null) {
            path = resource.getPath();
        }
    }

    public String path;

        @Inject
        public String value1 = "val1";

        @Inject
        public int value2 = -1;

        @Inject
        public String[] valueList = {};
    }
}
