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
package org.apache.sling.feature.cpconverter.cli;

import java.util.Date;
import java.util.Formatter;

import org.slf4j.Logger;

final class ShutDownHook extends Thread {

    private final long start = System.currentTimeMillis();

    private final Logger logger;

    public ShutDownHook(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void run() {
        logger.info("+-----------------------------------------------------+");
        logger.info("");

        // format the uptime string
        Formatter uptimeFormatter = new Formatter();
        uptimeFormatter.format("Total time:");

        long uptime = System.currentTimeMillis() - start;
        if (uptime < 1000) {
            uptimeFormatter.format(" %s millisecond%s", uptime, (uptime > 1 ? "s" : ""));
        } else {
            long uptimeInSeconds = (uptime) / 1000;
            final long hours = uptimeInSeconds / 3600;

            if (hours > 0) {
                uptimeFormatter.format(" %s hour%s", hours, (hours > 1 ? "s" : ""));
            }

            uptimeInSeconds = uptimeInSeconds - (hours * 3600);
            final long minutes = uptimeInSeconds / 60;

            if (minutes > 0) {
                uptimeFormatter.format(" %s minute%s", minutes, (minutes > 1 ? "s" : ""));
            }

            uptimeInSeconds = uptimeInSeconds - (minutes * 60);

            if (uptimeInSeconds > 0) {
                uptimeFormatter.format(" %s second%s", uptimeInSeconds, (uptimeInSeconds > 1 ? "s" : ""));
            }
        }
        logger.info(uptimeFormatter.toString());

        uptimeFormatter.close();

        logger.info("Finished at: {}", new Date());

        final Runtime runtime = Runtime.getRuntime();
        final int megaUnit = 1024 * 1024;

        logger.info("Final Memory: {}M/{}M",
                    (runtime.totalMemory() - runtime.freeMemory()) / megaUnit,
                    runtime.totalMemory() / megaUnit);
    }

}
