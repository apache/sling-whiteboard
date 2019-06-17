/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.sling.models.persist.bean;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

/**
 * Bean with children arranged in maps (enumeration map and also string keys)
 */
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class MappedChildren {
    public static enum KEYS{ONE,TWO,THREE};

    public String name;

    public Map<String, Child> stringKeys = new HashMap<>();

    public EnumMap<KEYS, Child> enumKeys = new EnumMap<>(KEYS.class);

    @Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
    public static class Child {
        public String name;
        public String testValue;
    }

    public MappedChildren() {
    }

    public MappedChildren(Resource resource) {
        if (resource != null) {
            name = resource.getName();
        }
    }


}
