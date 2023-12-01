/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.feature.launcher.atomos.weaver;

import org.apache.sling.feature.launcher.atomos.weaver.impl.AtomosWeaverVisitor;

import java.util.stream.Stream;

public class Test {
    private static final Class t = AtomosWeaverVisitor.class;
    private static final ClassLoader tt = AtomosWeaverVisitor.class.getClassLoader();

    public Test() {
        System.out.println("S" + Test.class.getClassLoader());
        System.out.println("I" + getClass().getClassLoader());
        System.out.println("SS" + Test.class.getResource("/" + Test.class.getName().replace('.', '/') + ".class"));
        System.out.println("II" + getClass().getResource("/" + Test.class.getName().replace('.', '/') + ".class"));

        System.out.println("SSS" + Test.class.getResourceAsStream("/" +Test.class.getName().replace('.', '/') + ".class"));

        System.out.println("III" + getClass().getResourceAsStream("/" +Test.class.getName().replace('.', '/') + ".class"));

        foo(Test.class);
        bar(Test.class.getClassLoader());
        la();
    }

    private void foo(Class cl) {
        System.out.println("FOO: " + cl.getClassLoader());
        ClassLoader loader = cl.getClassLoader();

        Stream.of(cl).map(l -> l.getClassLoader()).forEach(ll -> {
            System.out.println("ll. " + ll);
        });

        System.out.println("fl: " + loader);
    }

    private void bar(ClassLoader cl) {
        System.out.println("BAR: " + cl);
        System.out.println("t: " + t.getClassLoader());
        System.out.println("tt: " + tt);
    }

    private static void la() {
        System.out.println(Test.class.getClassLoader());
    }
}
