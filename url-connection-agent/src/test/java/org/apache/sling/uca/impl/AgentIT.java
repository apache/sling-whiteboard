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

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.sling.uca.impl.Main.ClientType;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates that accessing URLs that exhibit connection problems results in a timeouts being fired when the agent is used
 * 
 * <p>This test validates that the agent works when statically loaded, i.e. with a <tt>-javaagent:</tt> flag
 * passed to the JVM. As such it requires launching a new JVM instance each time, otherwise the results are
 * not valid.</p>
 * 
 * <p>It does so by reusing the same JVM as the one running the test. Validation is done by looking for a
 * Throwable information in the stderr and recording the exception class name and the message.</p>
 *
 */
@ExtendWith(MisbehavingServerExtension.class)
public class AgentIT {
    
    private static final Path STDERR = Paths.get("target", "stderr.txt");
    private static final Path STDOUT = Paths.get("target", "stdout.txt");
    private static final Logger LOG = LoggerFactory.getLogger(AgentIT.class);

    /**
     * Validates that connecting to a unaccessible port on an existing port fails with a connect 
     * timeout exception
     * 
     * <p>It is surprisingly hard to simulate a connnection timeout. The most reliable way seems to
     * be to get a firewall to drop packets, but this is very hard to do portably and safely
     * in a unit test. The least bad possible solution is to access an URL that we know will timeout
     * and that is able to sustain additional traffic. Maven Central is a good candidate for that.</p>
     * 
     * @throws IOException various I/O problems 
     */
    @ParameterizedTest
    @EnumSource(Main.ClientType.class)
    public void connectTimeout(ClientType clientType) throws IOException {

        RecordedThrowable error = assertTimeout(ofSeconds(5),  () -> runTest("http://repo1.maven.org:81", clientType));
        
        Class<?> expectedClass = clientType == ClientType.HC3 ? ConnectTimeoutException.class : SocketTimeoutException.class;
        String expectedMessage = clientType == ClientType.HC3 ? "The host did not accept the connection within timeout of 3000 ms" : "connect timed out";
        
        assertEquals(expectedClass.getName(), error.className);
        assertEquals(expectedMessage, error.message);
    }

    /**
     * Validates that connecting to a host that delays the response fails with a read timeout
     * 
     * @throws IOException various I/O problems
     */
    @ParameterizedTest
    @EnumSource(Main.ClientType.class)
    public void readTimeout(ClientType clientType, MisbehavingServerControl server) throws IOException {
        
        RecordedThrowable error = assertTimeout(ofSeconds(5),  () -> runTest("http://localhost:" + server.getLocalPort(), clientType));
        assertEquals(SocketTimeoutException.class.getName(), error.className);
        assertEquals("Read timed out", error.message);
    }
    

    private RecordedThrowable runTest(String urlSpec, ClientType clientType) throws IOException, InterruptedException {

        Process process = runForkedCommandWithAgent(new URL(urlSpec), 3, 3, clientType);
        boolean done = process.waitFor(30, TimeUnit.SECONDS);
        
        LOG.info("Dump of stdout: ");
        Files
            .lines(STDOUT)
            .forEach(LOG::info);

        LOG.info("Dump of stderr: ");
        Files
            .lines(STDERR)
            .forEach(LOG::info);

        if ( !done ) {
            process.destroy();
            throw new IllegalStateException("Terminated process since it did not exit in a reasonable amount of time.");
        }
        int exitCode = process.exitValue();
        LOG.info("Exited with code {}", exitCode);
        
        if ( exitCode != 0 ) {
            return Files.lines(STDERR)
                .filter( l -> l.startsWith("Exception in thread \"main\""))
                .map( l -> newRecordedThrowable(l) )
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Exit code was zero but did not find any exception information in stderr.txt"));
        }
        
        return null;
    }
    
    private Process runForkedCommandWithAgent(URL url, int connectTimeoutSeconds, int readTimeoutSeconds, ClientType clientType) throws IOException {
        
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
            "-javaagent:" + jar +"=" + TimeUnit.SECONDS.toMillis(connectTimeoutSeconds) +"," + TimeUnit.SECONDS.toMillis(readTimeoutSeconds),
            "-cp",
            classPath,
            "org.apache.sling.uca.impl.Main",
            url.toString(),
            clientType.toString()
        );
        
        pb.redirectInput(Redirect.INHERIT);
        pb.redirectOutput(STDOUT.toFile());
        pb.redirectError(STDERR.toFile());
        
        return pb.start();
    }
    
    private String buildClassPath() throws IOException {
        
        List<String> elements = new ArrayList<>();
        elements.add(Paths.get("target", "test-classes").toString());
        
        Files.list(Paths.get("target", "it-dependencies"))
            .filter( p -> p.getFileName().toString().equals("commons-httpclient.jar") 
                    || p.getFileName().toString().equals("commons-codec.jar")
                    || p.getFileName().toString().equals("slf4j-simple.jar")
                    || p.getFileName().toString().equals("slf4j-api.jar")
                    || p.getFileName().toString().equals("jcl-over-slf4j.jar"))
            .forEach( p -> elements.add(p.toString()));
        
        return String.join(File.pathSeparator, elements);
    }

    private RecordedThrowable newRecordedThrowable(String string) {
     
        string = string.replace("Exception in thread \"main\"", "");
        String[] parts = string.split(":");

        return new RecordedThrowable(parts[0].trim(), parts[1].trim());
    }
    
    /**
     * Basic information about a {@link Throwable} that was recorded in a file
     */
    static class RecordedThrowable {
        String className;
        String message;

        public RecordedThrowable(String className, String message) {
            this.className = className;
            this.message = message;
        }
        
        
    }
}
