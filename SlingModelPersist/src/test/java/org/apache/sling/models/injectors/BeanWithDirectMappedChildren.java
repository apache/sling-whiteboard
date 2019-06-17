/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.sling.models.injectors;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.persist.annotations.DirectDescendants;

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
