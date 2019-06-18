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
package org.apache.sling.uca.impl;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.sling.uca.impl.HttpClientLauncher.ClientType;

/**
 * Launches the {@link HttpClientLauncher} as a separate process with the timeout agent enabled
 *
 */
class AgentLauncher {
    private final URL url;
    private final TestTimeouts timeouts;
    private final ClientType clientType;
    private Path stdout;
    private Path stderr;

    public AgentLauncher(URL url, TestTimeouts timeouts, ClientType clientType, Path stdout, Path stderr) {
        this.url = url;
        this.timeouts = timeouts;
        this.clientType = clientType;
        this.stdout = stdout;
        this.stderr = stderr;
    }
    
    public Process launch() throws IOException {
        
        Path jar = Files.list(Paths.get("target"))
            .filter( p -> p.getFileName().toString().endsWith("-jar-with-dependencies.jar"))
            .findFirst()
            .orElseThrow( () -> new IllegalStateException("Did not find the agent jar. Did you run mvn package first?"));
        
        String classPath = buildClassPath();

        String javaHome = System.getProperty("java.home");
        Path javaExe = Paths.get(javaHome, "bin", "java");
        ProcessBuilder pb = new ProcessBuilder(
            javaExe.toString(),
            "-showversion",
            "-javaagent:" + jar +"=" + timeouts.agentConnectTimeout.toMillis() +"," + timeouts.agentReadTimeout.toMillis()+",v",
            "-cp",
            classPath,
            HttpClientLauncher.class.getName(),
            url.toString(),
            clientType.toString(),
            String.valueOf(timeouts.clientConnectTimeout.toMillis()),
            String.valueOf(timeouts.clientReadTimeout.toMillis())
        );
        
        pb.redirectInput(Redirect.INHERIT);
        pb.redirectOutput(stdout.toFile());
        pb.redirectError(stderr.toFile());
        
        return pb.start();
    }
    
    private String buildClassPath() throws IOException {
        
        List<String> elements = new ArrayList<>();
        elements.add(Paths.get("target", "test-classes").toString());
        
        Set<String> dependencies = new HashSet<>(Arrays.asList(new String[] {
            "commons-httpclient.jar",
            "commons-codec.jar",
            "slf4j-simple.jar",
            "slf4j-api.jar",
            "jcl-over-slf4j.jar",
            "httpclient.jar",
            "httpcore.jar",
            "okhttp.jar",
            "okio.jar"
        }));
        
        Files.list(Paths.get("target", "it-dependencies"))
            .filter( p -> dependencies.contains(p.getFileName().toString()) )
            .forEach( p -> elements.add(p.toString()));
        
        return String.join(File.pathSeparator, elements);
    }
}