/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.sling.models.persist.bean;

import javax.inject.Inject;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.persist.annotations.ChildType;

/**
 * Example of bean with getPath method that stores the path in a field
 */
@Model(adaptables = Resource.class, resourceType = "test/testBean")
@ChildType("test/testBean/field-path")
public class BeanWithPathField {
    @Inject
    public String prop1 = "testValue";
    
    public String path = "/test/field-path";
}
