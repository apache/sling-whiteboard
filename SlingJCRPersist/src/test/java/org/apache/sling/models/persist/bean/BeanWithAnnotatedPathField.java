/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.sling.models.persist.bean;

import javax.inject.Inject;
import org.apache.sling.models.annotations.Path;

/**
 * Example of bean with getPath method that stores the path in a field using an annotation marker
 */
public class BeanWithAnnotatedPathField {
    @Inject
    public String prop1 = "testValue";
    
    public String path = "/test/WRONG-path";

    @Path
    public String correctPath = "/test/annotated-field-path";
    
    public String getPath() {
        return path;
    }
}
