/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.feature.analyser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import org.apache.sling.feature.Application;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.AnalyserTaskContext;
import org.apache.sling.feature.scanner.ApplicationDescriptor;
import org.apache.sling.feature.scanner.Scanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Analyser {

    private final AnalyserTask[] tasks;

    private final Scanner scanner;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public Analyser(final Scanner scanner,
            final AnalyserTask...tasks)
    throws IOException {
        this.tasks = tasks;
        this.scanner = scanner;
    }

    public Analyser(final Scanner scanner,
            final String... taskIds)
    throws IOException {
        this(scanner, getTasks(taskIds));
        if ( this.tasks.length != taskIds.length ) {
            throw new IOException("Couldn't find all tasks " + taskIds);
        }
    }

    public Analyser(final Scanner scanner)
    throws IOException {
        this(scanner, getTasks((String[])null));
    }

    private static AnalyserTask[] getTasks(final String... taskIds) {
        final Set<String> ids = taskIds == null ? null : new HashSet<>(Arrays.asList(taskIds));
        final ServiceLoader<AnalyserTask> loader = ServiceLoader.load(AnalyserTask.class);
        final List<AnalyserTask> list = new ArrayList<>();
        for(final AnalyserTask task : loader) {
            if ( ids == null || ids.contains(task.getId()) ) {
                list.add(task);
            }
        }
        return list.toArray(new AnalyserTask[list.size()]);
    }

    public void analyse(final Application app)
    throws Exception {
        logger.info("Starting application analyzer...");

        final ApplicationDescriptor appDesc = scanner.scan(app);

        final List<String> warnings = new ArrayList<>();
        final List<String> errors = new ArrayList<>();

        // execute analyser tasks
        for(final AnalyserTask task : tasks) {
            logger.info("- Executing {}...", task.getName());
            task.execute(new AnalyserTaskContext() {

                @Override
                public Application getApplication() {
                    return app;
                }

                @Override
                public ApplicationDescriptor getDescriptor() {
                    return appDesc;
                }

                @Override
                public void reportWarning(final String message) {
                    warnings.add(message);
                }

                @Override
                public void reportError(final String message) {
                    errors.add(message);
                }
            });
        }

        for(final String msg : warnings) {
            logger.warn(msg);
        }
        for(final String msg : errors) {
            logger.error(msg);
        }

        if ( !errors.isEmpty() ) {
            throw new Exception("Analyser detected errors. See log output for error messages.");
        }

        logger.info("Provisioning model analyzer finished");
    }
}
