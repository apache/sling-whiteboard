/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
