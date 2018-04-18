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
package org.apache.sling.feature.analyser.main;

import org.apache.sling.feature.Application;
import org.apache.sling.feature.analyser.service.Analyser;
import org.apache.sling.feature.analyser.service.Scanner;
import org.apache.sling.feature.io.ArtifactManagerConfig;
import org.apache.sling.feature.io.json.ApplicationJSONReader;
import org.apache.sling.feature.support.FeatureUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class Main {

    public static void main(final String[] args) {
        // setup logging
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        System.setProperty("org.slf4j.simpleLogger.showThreadName", "false");
        System.setProperty("org.slf4j.simpleLogger.levelInBrackets", "true");
        System.setProperty("org.slf4j.simpleLogger.showLogName", "false");

        final Logger logger = LoggerFactory.getLogger("analyser");
        logger.info("Apache Sling Application Analyser");
        logger.info("");

        if ( args.length == 0 ) {
            logger.error("Required argument missing: application file");
            System.exit(1);
        }
        if ( args.length > 1 ) {
            logger.error("Too many arguments. Only one (application file) is supported");
            System.exit(1);
        }
        final File f = new File(args[0]);
        Application app = null;
        try ( final FileReader r = new FileReader(f)) {
            app = ApplicationJSONReader.read(r);
        } catch ( final IOException ioe) {
            logger.error("Unable to read application: {}", f, ioe);
            System.exit(1);
        }
        if ( app.getFramework() == null ) {
            app.setFramework(FeatureUtil.getFelixFrameworkId(null));
        }

        try {
            final Scanner scanner = new Scanner(new ArtifactManagerConfig());
            final Analyser analyser = new Analyser(scanner);
            analyser.analyse(app);
        } catch ( final Exception e) {
            logger.error("Unable to analyse application: {}", f, e);
            System.exit(1);
        }
    }
}
