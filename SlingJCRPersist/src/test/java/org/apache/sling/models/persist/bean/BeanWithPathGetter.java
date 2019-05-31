/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.sling.models.persist.bean;

import javax.inject.Inject;

/**
 * Example of bean with getPath method that renders path of the bean [possible] dynamically.
 */
public class BeanWithPathGetter {
    @Inject
    public String prop1 = "testValue";
    
    // This provides the path of the bean, which could also be some kind of dynamic business logic.
    public String getPath() {
        return "/test/dynamic-path";
    }
}
