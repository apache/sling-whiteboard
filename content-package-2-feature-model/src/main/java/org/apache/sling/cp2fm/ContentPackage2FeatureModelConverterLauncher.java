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
package org.apache.sling.cp2fm;

import java.io.File;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
    name = "cp2fm",
    description = "Apache Sling Content Package to Sling Feature converter",
    footer = "Copyright(c) 2019 The Apache Software Foundation."
)
public final class ContentPackage2FeatureModelConverterLauncher implements Runnable {

    @Option(names = { "-h", "--help" }, usageHelp = true, description = "Display the usage message.")
    private boolean helpRequested;

    @Option(names = { "-X", "--verbose" }, description = "Produce execution debug output.")
    private boolean debug;

    @Option(names = { "-q", "--quiet" }, description = "Log errors only.")
    private boolean quiet;

    @Option(names = { "-v", "--version" }, description = "Display version information.")
    private boolean printVersion;

    @Option(names = { "-c", "--content-package" }, description = "The content-package input file.", required = true)
    private File contentPackage;

    @Option(names = { "-o", "--output-directory" }, description = "The output directory where the Feature File and the bundles will be deployed.", required = true)
    private File outputDirectory;

    @Override
    public void run() {
        if (quiet) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "error");
        } else if (debug) {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");
        } else {
            System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
        }

        String appName = getClass().getAnnotation(Command.class).description()[0];
        final Logger logger = LoggerFactory.getLogger(appName);

        // Add the Shutdown Hook to the Java virtual machine
        // in order to destroy all the allocated resources
        Runtime.getRuntime().addShutdownHook(new ShutDownHook(logger));

        if (printVersion) {
            printVersion(logger);
        }

        logger.info(appName);
        logger.info("");

        try {
            ContentPackage2FeatureModelConverter converter = new ContentPackage2FeatureModelConverter();
            converter.convert(contentPackage, outputDirectory);

            logger.info( "+-----------------------------------------------------+" );
            logger.info("{} SUCCESS", appName);
        } catch (Throwable t) {
            logger.info( "+-----------------------------------------------------+" );
            logger.info("{} FAILURE", appName);
            logger.info( "+-----------------------------------------------------+" );

            if (debug) {
                logger.error("Unable to convert content-package {}:", contentPackage, t);
            } else {
                logger.error("Unable to convert content-package {}: {}", contentPackage, t.getMessage());
            }

            logger.info( "" );

            System.exit(1);
        }

        logger.info( "+-----------------------------------------------------+" );
    }

    private static void printVersion(final Logger logger) {
        logger.info("{} v{} (built on {})",
                System.getProperty("project.artifactId"),
                System.getProperty("project.version"),
                System.getProperty("build.timestamp"));
        logger.info("Java version: {}, vendor: {}",
                System.getProperty("java.version"),
                System.getProperty("java.vendor"));
        logger.info("Java home: {}", System.getProperty("java.home"));
        logger.info("Default locale: {}_{}, platform encoding: {}",
                System.getProperty("user.language"),
                System.getProperty("user.country"),
                System.getProperty("sun.jnu.encoding"));
        logger.info("Default Time Zone: {}", TimeZone.getDefault().getDisplayName());
        logger.info("OS name: \"{}\", version: \"{}\", arch: \"{}\", family: \"{}\"",
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"),
                getOsFamily());
        logger.info("+-----------------------------------------------------+");
    }

    private static final String getOsFamily() {
        String osName = System.getProperty("os.name").toLowerCase();
        String pathSep = System.getProperty("path.separator");

        if (osName.indexOf("windows") != -1) {
            return "windows";
        } else if (osName.indexOf("os/2") != -1) {
            return "os/2";
        } else if (osName.indexOf("z/os") != -1 || osName.indexOf("os/390") != -1) {
            return "z/os";
        } else if (osName.indexOf("os/400") != -1) {
            return "os/400";
        } else if (pathSep.equals(";")) {
            return "dos";
        } else if (osName.indexOf("mac") != -1) {
            if (osName.endsWith("x")) {
                return "mac"; // MACOSX
            }
            return "unix";
        } else if (osName.indexOf("nonstop_kernel") != -1) {
            return "tandem";
        } else if (osName.indexOf("openvms") != -1) {
            return "openvms";
        } else if (pathSep.equals(":")) {
            return "unix";
        }

        return "undefined";
    }

    public static void main(String[] args) {
        CommandLine.run(new ContentPackage2FeatureModelConverterLauncher(), args);
    }

}
