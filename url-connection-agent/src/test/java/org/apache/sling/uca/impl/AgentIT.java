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
import static java.util.Objects.requireNonNull;
import static org.apache.sling.uca.impl.HttpClientLauncher.ClientType.HC3;
import static org.apache.sling.uca.impl.HttpClientLauncher.ClientType.HC4;
import static org.apache.sling.uca.impl.HttpClientLauncher.ClientType.JavaNet;
import static org.apache.sling.uca.impl.HttpClientLauncher.ClientType.OkHttp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.sling.uca.impl.HttpClientLauncher.ClientType;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
 */
@ExtendWith(MisbehavingServerExtension.class)
public class AgentIT {
    
    private static final int EXECUTION_TIMEOUT_SECONDS = 5;
    private static final int CONNECT_TIMEOUT_SECONDS = 3;
    private static final int READ_TIMEOUT_SECONDS = 3;
    
    private static final String EXCEPTION_MARKER = "Exception in thread \"main\" ";
    private static final Path STDERR = Paths.get("target", "stderr.txt");
    private static final Path STDOUT = Paths.get("target", "stdout.txt");
    private static final Logger LOG = LoggerFactory.getLogger(AgentIT.class);
    
    private static Map<ClientType, ErrorDescriptor> errorDescriptors = new EnumMap<>(ClientType.class);
    static {
        errorDescriptors.put(JavaNet, new ErrorDescriptor(SocketTimeoutException.class, "connect timed out", "Read timed out"));
        errorDescriptors.put(HC3, new ErrorDescriptor(ConnectTimeoutException.class, "The host did not accept the connection within timeout of 3000 ms", "Read timed out"));
        errorDescriptors.put(HC4, new ErrorDescriptor(org.apache.http.conn.ConnectTimeoutException.class, 
                "Connect to repo1.maven.org:81 \\[.*\\] failed: connect timed out", "Read timed out"));
        errorDescriptors.put(OkHttp, new ErrorDescriptor(SocketTimeoutException.class, "connect timed out", "timeout"));
    }

    /**
     * Creates a matrix of all arguments to use for the read and connect timeout tests
     * 
     * <p>This matrix uses all client types and pairs them with each of the timeouts, for now:
     * 
     * <ol>
     *   <li>default timeouts, which mean the agent-set default timeout will kick in</li>
     *   <li>lower client API timeouts, which mean that the client-enforced timeouts will be applied</li>
     * </ol>
     * 
     * </p>
     * 
     * @return a list of arguments to use for the tests
     */
    static List<Arguments> argumentsMatrix() {
        
        List<Arguments> args = new ArrayList<Arguments>();
        
        TestTimeouts clientLower = new TestTimeouts.Builder()
            .agentTimeouts(Duration.ofMinutes(1), Duration.ofMinutes(1))
            .clientTimeouts(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS), Duration.ofSeconds(READ_TIMEOUT_SECONDS))
            .build();
    
        for ( ClientType client : ClientType.values() )
            for ( TestTimeouts timeout : new TestTimeouts[] { TestTimeouts.DEFAULT, clientLower } )
                args.add(Arguments.of(client, timeout));
        
        return args;
    }

    
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
    @MethodSource("argumentsMatrix")
    public void connectTimeout(ClientType clientType, TestTimeouts timeouts) throws IOException {

        ErrorDescriptor ed =  requireNonNull(errorDescriptors.get(clientType), "Unhandled clientType " + clientType);
        RecordedThrowable error = assertTimeout(ofSeconds(EXECUTION_TIMEOUT_SECONDS),  
            () -> runTest("http://repo1.maven.org:81", clientType, timeouts));
        
        assertEquals(ed.connectTimeoutClass.getName(), error.className);
        assertTrue(error.message.matches(ed.connectTimeoutMessageRegex), 
            "Actual message " + error.message + " did not match regex " + ed.connectTimeoutMessageRegex);
    }

    /**
     * Validates that connecting to a host that delays the response fails with a read timeout
     * 
     * @throws IOException various I/O problems
     */
    @ParameterizedTest
    @MethodSource("argumentsMatrix")
    public void readTimeout(ClientType clientType, TestTimeouts timeouts, MisbehavingServerControl server) throws IOException {
        
        ErrorDescriptor ed =  requireNonNull(errorDescriptors.get(clientType), "Unhandled clientType " + clientType);
        RecordedThrowable error = assertTimeout(ofSeconds(EXECUTION_TIMEOUT_SECONDS),
           () -> runTest("http://localhost:" + server.getLocalPort(), clientType, timeouts));
        
        assertEquals(SocketTimeoutException.class.getName(), error.className);
        assertEquals(ed.readTimeoutMessage, error.message);
    }

    private RecordedThrowable runTest(String urlSpec, ClientType clientType, TestTimeouts timeouts) throws IOException, InterruptedException {

        Process process = runForkedCommandWithAgent(new URL(urlSpec), timeouts, clientType);
        boolean done = process.waitFor(timeouts.executionTimeout.toMillis(), TimeUnit.MILLISECONDS);
        
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
            throw new IllegalStateException("Terminated process since it did not complete within " + timeouts.executionTimeout.toMillis() + " milliseconds");
        }
        int exitCode = process.exitValue();
        LOG.info("Exited with code {}", exitCode);
        
        if ( exitCode == 0 ) {
            throw new RuntimeException("Command terminated successfully. That is unexpected.");
        } else {
            return Files.lines(STDERR)
                .filter( l -> l.startsWith(EXCEPTION_MARKER))
                .map( l -> newRecordedThrowable(l) )
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Exit code was not zero ( " + exitCode + " ) but did not find any exception information in stderr.txt"));
        }
    }
    
    private Process runForkedCommandWithAgent(URL url, TestTimeouts timeouts, ClientType clientType) throws IOException {
        
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
        pb.redirectOutput(STDOUT.toFile());
        pb.redirectError(STDERR.toFile());
        
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

    private RecordedThrowable newRecordedThrowable(String line) {
        
        line = line.replace(EXCEPTION_MARKER, "");

        String className = line.substring(0, line.indexOf(':'));
        String message = line.substring(line.indexOf(':') + 2); // ignore ':' and leading ' '

        return new RecordedThrowable(className, message);
    }
    
    /**
     * Data class for defining specific error messages related to individual {@link ClientType client types}. 
     */
    static class ErrorDescriptor {
        private Class<? extends IOException> connectTimeoutClass;
        private String connectTimeoutMessageRegex;
        private String readTimeoutMessage;

        public ErrorDescriptor(Class<? extends IOException> connectTimeoutClass, String connectTimeoutMessageRegex,
                String readTimeoutMessage) {
            this.connectTimeoutClass = connectTimeoutClass;
            this.connectTimeoutMessageRegex = connectTimeoutMessageRegex;
            this.readTimeoutMessage = readTimeoutMessage;
        }
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
    
    /**
     * Data class for holding information about various timeouts set in the tests
     */
    static class TestTimeouts {
        
        private Duration executionTimeout = Duration.ofSeconds(EXECUTION_TIMEOUT_SECONDS);
        private Duration agentConnectTimeout = Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS);
        private Duration agentReadTimeout = Duration.ofSeconds(READ_TIMEOUT_SECONDS);
        private Duration clientConnectTimeout = Duration.ZERO;
        private Duration clientReadTimeout = Duration.ZERO;
        
        public static TestTimeouts DEFAULT = new TestTimeouts();
        
        static class Builder {
            private TestTimeouts timeouts = new TestTimeouts();
            
            public Builder executionTimeout(Duration duration) {
                timeouts.executionTimeout = Objects.requireNonNull(duration);
                return this;
            }

            public Builder agentTimeouts(Duration connectTimeout, Duration readTimeout) {
                timeouts.agentConnectTimeout = Objects.requireNonNull(connectTimeout);
                timeouts.agentReadTimeout = Objects.requireNonNull(readTimeout);
                return this;
            }
            
            public Builder clientTimeouts(Duration connectTimeout, Duration readTimeout) {
                timeouts.clientConnectTimeout = Objects.requireNonNull(connectTimeout);
                timeouts.clientReadTimeout = Objects.requireNonNull(readTimeout);
                return this;
            }
            
            public TestTimeouts build() {
                return timeouts;
            }
        }
        
        @Override
        public String toString() {
            return getClass().getSimpleName() + ": execution " + executionTimeout + ", agent: " + agentConnectTimeout + "/" + agentReadTimeout + ", client : " + clientConnectTimeout + "/" + clientReadTimeout;
        }
    }
}
