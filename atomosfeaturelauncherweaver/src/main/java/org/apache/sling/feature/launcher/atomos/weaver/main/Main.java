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
package org.apache.sling.feature.launcher.atomos.weaver.main;

import org.apache.sling.feature.launcher.atomos.weaver.AtomosWeaver;
import org.apache.sling.feature.launcher.atomos.weaver.impl.AtomosWeaverVisitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ServiceLoader;

public class Main {

    public static void main(String[] args) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        AtomosWeaver weaver = ServiceLoader.load(AtomosWeaver.class).iterator().next();

        byte[] bytes = weaver.weave(
            new FileInputStream(args[0].replace('.', File.separatorChar) + ".class").readAllBytes(),
            Main.class.getName(), "getClassLoaderFix", "getResourceFix", "getStreamFix",
            AtomosWeaverVisitor.class.getClassLoader()
        );
        try (FileOutputStream output = new FileOutputStream("dump.class")) {
            output.write(bytes);
        }
        new ClassLoader(){
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (name.equals(args[0])) {
                    return defineClass(name, bytes, 0, bytes.length);
                } else {
                    return AtomosWeaverVisitor.class.getClassLoader().loadClass(name);
                }
            }
        }.loadClass(args[0]).newInstance();
    }

    public static ClassLoader getClassLoaderFix(Class c) {
        System.out.println("CALLED! " + c.getName());
        return c.getClassLoader();
    }

    public static URL getResourceFix(Class c, String r) {
        System.out.println("CALLED!! " + c.getName() + " " + r);
        return c.getResource(r);
    }

    public static InputStream getStreamFix(Class c, String r) {
        System.out.println("CALLED!!! " + c.getName() + " " + r);
        return c.getResourceAsStream(r);
    }
}
