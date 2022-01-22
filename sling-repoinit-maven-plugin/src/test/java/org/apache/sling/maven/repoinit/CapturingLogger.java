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
package org.apache.sling.maven.repoinit;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.codehaus.plexus.logging.AbstractLogger;
import org.codehaus.plexus.logging.Logger;

public class CapturingLogger extends AbstractLogger {

    private List<String> lines = new ArrayList<>();
    private boolean hadErrors = false;

    public CapturingLogger() {
        super(LEVEL_DEBUG, "test");
    }

    @Override
    public void debug(String message, Throwable throwable) {
        log("[debug] ", message, throwable);

    }

    @Override
    public void info(String message, Throwable throwable) {
        log("[info] ", message, throwable);
    }

    @Override
    public void warn(String message, Throwable throwable) {
        log("[warn] ", message, throwable);
    }

    @Override
    public void error(String message, Throwable throwable) {
        hadErrors = true;
        log("[error] ", message, throwable);
    }

    @Override
    public void fatalError(String message, Throwable throwable) {
        hadErrors = true;
        log("[fatal] ", message, throwable);
    }

    public boolean hadErrors() {
        return hadErrors;
    }

    public void reset() {
        lines.clear();
    }

    public List<String> getLines() {
        return lines;
    }

    public String getLogs() {
        return lines.stream().collect(Collectors.joining("\n"));
    }

    private void log(String levelStr, String message, Throwable throwable) {
        lines.add(levelStr + message);
        if (throwable != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            lines.add(sw.toString());
        }
    }

    @Override
    public Logger getChildLogger(String name) {
        return null;
    }

}
