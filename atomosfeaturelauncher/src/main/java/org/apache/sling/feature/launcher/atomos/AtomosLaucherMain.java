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
package org.apache.sling.feature.launcher.atomos;


import org.apache.sling.feature.launcher.impl.Main;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AtomosLaucherMain {
    public static void main(String[] args) {
        List<String> launcherArgs = new ArrayList<>(Arrays.asList(args));

        if (args.length > 0) {
            URL feature = AtomosLaucherMain.class.getResource("/META-INF/features/feature-" + args[0] + ".json");
            if (feature != null) {
                launcherArgs.remove(0);
                launcherArgs.add(0, feature.toString());
                launcherArgs.add(0, "-f");
            }
        } else {
            URL feature = AtomosLaucherMain.class.getResource("/META-INF/features/feature.json");
            if (feature != null) {
                launcherArgs.addAll(Arrays.asList("-f", feature.toString()));
            }
        }
        Main.main(launcherArgs.toArray(new String[0]));
    }
}
