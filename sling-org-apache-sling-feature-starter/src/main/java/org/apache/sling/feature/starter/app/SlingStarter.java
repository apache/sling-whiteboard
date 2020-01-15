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
package org.apache.sling.feature.starter.app;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.feature.launcher.impl.Main;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;


@Command(
    name = "java -jar <Sling Feature Starter JAR File>",
    description = "Apache Sling Feature Starter",
    footer = "Copyright(c) 2019 The Apache Software Foundation."
)
public class SlingStarter implements Runnable, ControlTarget {

    @Option(names = { "-s", "--mainFeature" }, description = "main feature file (file path or URL) replacing the provided Sling Feature File", required = false)
    private String mainFeatureFile;

    @Option(names = { "-af", "--additionalFeature" }, description = "additional feature files", required = false)
    private List<String> additionalFeatureFile;

    @Option(names = { "-j", "--control" }, description = "host and port to use for control connection in the format '[host:]port' (default 127.0.0.1:0)", required = false)
    private String controlAddress;

    @Option(names = { "-l", "--logLevel" }, description = "the initial loglevel (0..4, FATAL, ERROR, WARN, INFO, DEBUG)", required = false)
    private String logLevel;

    @Option(names = { "-f", "--logFile" }, description = "the log file, \"-\" for stdout (default logs/error.log)", required = false)
    private String logFile;

    @Option(names = { "-c", "--slingHome" }, description = "the sling context directory (default sling)", required = false)
    private String slingHome;

    //AS TODO: does this still apply here
    @Option(names = { "-i", "--launcherHome" }, description = "the launcher home directory (default launcher)", required = false)
    private String launcherHome;

    @Option(names = { "-a", "--address" }, description = "the interface to bind to (use 0.0.0.0 for any)", required = false)
    private String address;

    @Option(names = { "-p", "--port" }, description = "the port to listen to (default 8080)", required = false)
    private String port;

    @Option(names = { "-r", "--context" }, description = "the root servlet context path for the http service (default is /)", required = false)
    private String contextPath;

    @Option(names = { "-n", "--noShutdownHook" }, description = "don't install the shutdown hook")
    private boolean noShutdownHook;

    @Option(names = { "-v", "--verbose" }, description = "the feature launcher is verbose on launch", required = false)
    private boolean verbose;

    @Option(names = {"-D", "--define"}, description = "sets property n to value v. Make sure to use this option *after* the jar filename. " +
        "The JVM also has a -D option which has a different meaning", required = false)
    private Map<String, String> properties = new HashMap<>();

    @Option(names = { "-h", "--help" }, usageHelp = true, description = "Display the usage message.")
    private boolean helpRequested;

    @Parameters(paramLabel = "COMMAND", description = "Optional Command for Server Instance Interaction, can be one of: 'start', 'stop', 'status' or 'threads'", arity = "0..1")
    private String command;

    // The name of the environment variable to consult to find out
    // about sling.home
    private static final String ENV_SLING_HOME = "SLING_HOME";

//    /**
//     * The name of the configuration property indicating the
//     * {@link ControlAction} to be taken in the {@link #doControlAction()}
//     * method.
//     */
//    protected static final String PROP_CONTROL_ACTION = "sling.control.action";

    /**
     * The name of the configuration property indicating the socket to use for
     * the control connection. The value of this property is either just a port
     * number (in which case the host is assumed to be <code>localhost</code>)
     * or a host name (or IP address) and port number separated by a colon.
     */
    protected static final String PROP_CONTROL_SOCKET = "sling.control.socket";

    /** The Sling configuration property name setting the initial log level */
    private static final String PROP_LOG_LEVEL = "org.apache.sling.commons.log.level";

    /** The Sling configuration property name setting the initial log file */
    private static final String PROP_LOG_FILE = "org.apache.sling.commons.log.file";

    /**
     * The configuration property setting the port on which the HTTP service
     * listens
     */
    private static final String PROP_PORT = "org.osgi.service.http.port";

    /**
     * The configuration property setting the context path where the HTTP service
     * mounts itself.
     */
    private static final String PROP_CONTEXT_PATH = "org.apache.felix.http.context_path";

    /**
     * Host name or IP Address of the interface to listen on.
     */
    private static final String PROP_HOST = "org.apache.felix.http.host";

    /**
     * Name of the configuration property (or system property) indicating
     * whether the shutdown hook should be installed or not. If this property is
     * not set or set to {@code true} (case insensitive), the shutdown hook
     * properly shutting down the framework is installed on startup. Otherwise,
     * if this property is set to any value other than {@code true} (case
     * insensitive) the shutdown hook is not installed.
     * <p>
     * The respective command line option is {@code -n}.
     */
    private static final String PROP_SHUTDOWN_HOOK = "sling.shutdown.hook";

    private boolean started = false;

    @Override
    public void run() {
        try {
            URL mainFeatureURL = checkFeatureFile(mainFeatureFile);
            if(mainFeatureURL == null) {
                mainFeatureURL = getClass().getResource("/feature-sling12.json");
            }
            List<String> argumentList = new ArrayList<>();
            argumentList.add("-f");
            argumentList.add(mainFeatureURL.toString());
            if(additionalFeatureFile != null) {
                for (String additional : additionalFeatureFile) {
                    URL additionalURL = checkFeatureFile(additional);
                    if (additionalURL != null) {
                        argumentList.add("-f");
                        argumentList.add(additionalURL.toString());
                    }
                }
            }
            //TODO: Remove because this is handled here so we do not need to pass it to the Feature Launcher
//            if(StringUtils.isNotEmpty(controlAddress)) {
//                addArgument(argumentList, PROP_CONTROL_SOCKET, controlAddress);
//            }
            if(StringUtils.isNotEmpty(logLevel)) {
                addArgument(argumentList, PROP_LOG_LEVEL, logLevel);
            }
            if(StringUtils.isNotEmpty(logFile)) {
                addArgument(argumentList, PROP_LOG_FILE, logFile);
            }
            if(StringUtils.isNotEmpty(port)) {
                addArgument(argumentList, PROP_PORT, port);
            }
            if(StringUtils.isNotEmpty(address)) {
                addArgument(argumentList, PROP_HOST, address);
            }
            if(StringUtils.isNotEmpty(contextPath)) {
                addArgument(argumentList, PROP_CONTEXT_PATH, contextPath);
            }
            if(verbose) {
                argumentList.add("-v");
            }
            System.out.println("Before Launching Feature Launcher, arguments: " + argumentList);
            // Now we have to handle any Start Option
            ControlAction controlAction = getControlAction(command);
            int answer = doControlAction(controlAction, controlAddress);
            if (answer >= 0) {
                doTerminateVM(answer);
                return;
            }

            // finally start Sling
            if (!doStart(argumentList)) {
                error("Failed to start Sling; terminating", null);
                doTerminateVM(1);
                return;
            }
        } catch(Throwable t) {
            System.out.println("Caught an Exception: " + t.getLocalizedMessage());
            t.printStackTrace();
        }
    }

    private void addArgument(List<String> list, String key, String value) {
        list.add("-D");
        list.add(key + "=" + value);
    }

    private URL checkFeatureFile(String featureFile) {
        URL answer = null;
        if(featureFile != null && !featureFile.isEmpty()) {
            try {
                URL check = new URL(featureFile);
                check.toURI();
                answer = check;
            } catch (MalformedURLException | URISyntaxException e) {
                // Try it as a file
                File check = new File(mainFeatureFile);
                if (!check.exists() || !check.canRead()) {
                    throw new RuntimeException("Given Feature File is not a valid URL or File: '" + mainFeatureFile + "'", e);
                }
                try {
                    answer = check.toURI().toURL();
                } catch (MalformedURLException ex) {
                    throw new RuntimeException("Given Feature File cannot be converted to an URL: '" + mainFeatureFile + "'", e);
                }
            }
        }
        return answer;
    }

    public static void main(String[] args) {
        CommandLine.run(new SlingStarter(), args);
    }

    private int doControlAction(ControlAction controlAction, String controlAddress) {
//        if(controlAction != ControlAction.FOREGROUND) {
            final ControlListener sl = new ControlListener(
                this,
                controlAddress
            );
            switch (controlAction) {
                case FOREGROUND:
                    if (!sl.listen()) {
                        return -1;
                    }
                    break;
                case START:
                    if (!sl.listen()) {
                        // assume service already running
                        return 0;
                    }
                    break;
                case STOP:
                    return sl.shutdownServer();
                case STATUS:
                    return sl.statusServer();
                case THREADS:
                    return sl.dumpThreads();
            }
//        }
        return -1;
    }

    private boolean doStart(List<String> argumentList) {
        // prevent duplicate start
        if ( this.started) {
            info("Apache Sling has already been started", new Exception("Where did this come from"));
            return true;
        }

        info("Starting Apache Sling in " + slingHome, null);
        this.started = true;
        System.out.println("Start Command: '" + command + "'");
        try {
            Main.main(argumentList.toArray(new String[]{}));
        } catch(Error | RuntimeException e) {
            error("Launching Sling Feature failed", e);
            return false;
        }
        return true;
    }

    private ControlAction getControlAction(String command) {
        ControlAction answer = ControlAction.FOREGROUND;
        try {
            answer = ControlAction.valueOf(command.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Given Control Action is not valid: '" + command.toUpperCase() + "'");
        } catch (NullPointerException e) {
            // Ignore as we set the default to FOREGROUND anyhow
        }
        return answer;
    }

    @Override
    public String getHome() {
        return slingHome;
    }

    @Override
    public void doStop() {
        info("Stop Application", null);
        System.exit(0);
    }

    @Override
    public void doTerminateVM(int status) {
        info("Terminate VM, status: " + status, null);
        System.exit(status);
    }

    @Override
    public void info(String message, Throwable t) {
        System.out.println(message);
        if(t != null) {
            t.printStackTrace();
        }
    }

    @Override
    public void error(String message, Throwable t) {
        System.err.println(message);
        if(t != null) {
            t.printStackTrace(System.err);
        }
    }
}
