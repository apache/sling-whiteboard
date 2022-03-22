///usr/bin/env jbang "$0" "$@" ; exit $? 

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

//DEPS org.apache.sling:org.apache.sling.feature.launcher:1.2.0
//DEPS commons-cli:commons-cli:1.4
//DEPS org.apache.commons:commons-lang3:3.12.0
//DEPS org.apache.commons:commons-text:1.9
//DEPS org.apache.felix:org.apache.felix.cm.json:1.0.6
//DEPS org.apache.felix:org.apache.felix.converter:1.0.18
//DEPS org.apache.sling:org.apache.sling.commons.johnzon:1.2.14
//DEPS org.apache.sling:org.apache.sling.feature:1.2.30
//DEPS org.apache.sling:org.apache.sling.feature.launcher:1.2.0
//DEPS org.osgi:org.osgi.annotation.versioning:1.1.1
//DEPS org.osgi:org.osgi.util.function:1.0.0
//DEPS org.osgi:osgi.core:7.0.0
//DEPS org.slf4j:slf4j-api:1.7.25
//DEPS org.slf4j:slf4j-simple:1.7.25

import org.apache.sling.feature.launcher.impl.Main;

/** Thanks to the JBang catalog found at https://github.com/apache/sling-whiteboard/blob/master/jbang-catalog.json
 *  this can be started using
 * 
 *     jbang start@apache/sling-whiteboard
 * 
 *  After installing JBang from https://www.jbang.dev/
 *
 */
class SlingStarter {

    public static void main(String[] args) {

        // Starter 12 is the only version that we distribute like
        // this, so there's no other version to switch to. Not yet.
        final int defaultVersion = 12;
        final int version = args == null || args.length == 0 ? defaultVersion : Integer.valueOf(args[0]);
        final String farURL = String.format(
            "https://repo1.maven.org/maven2/org/apache/sling/org.apache.sling.starter/%d/org.apache.sling.starter-%d-oak_tar_far.far",
            version,
            version
        );
        
        System.err.println(String.format("Starting Apache Sling %d using the Feature Launcher", version));
        System.err.println("The first run of this script is slower, as it downloads the Sling starter feature file and dependencies");
        System.err.println("Starter feature file: " + farURL);
        System.err.println("TODO: need to handle arguments such as Sling server port number etc.");

        final String [] starterArgs = {
            "-f",
            farURL
        };
        Main.main(starterArgs);
    }
}