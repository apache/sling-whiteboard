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
package org.apache.sling.maven.kickstart.run;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.utils.StringUtils;
import org.apache.sling.maven.kickstart.launcher.Main;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.ProcessBuilder.Redirect;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * A callable for launchpad an instance
 */
public class LauncherCallable implements Callable<ProcessDescription> {

    private final LaunchpadEnvironment environment;
    private final ServerConfiguration configuration;
    private final Log logger;

    public LauncherCallable(final Log logger,
                                  final ServerConfiguration configuration,
                                  final LaunchpadEnvironment environment) {
        this.logger = logger;
        this.configuration = configuration;
        this.environment = environment;
    }

    /**
     * @see Callable#call()
     */
    @Override
    public ProcessDescription call() throws Exception {
        logger.info("call() started");
        // fail if launchpad with this id is already started
        if (!ProcessDescriptionProvider.getInstance().isRunConfigurationAvailable(configuration.getId())) {
            throw new Exception("Launchpad with id " + configuration.getId() + " is not available");
        }

        // get the launchpad jar
        final File launchpad = this.environment.prepare(this.configuration.getFolder());

        // Lock the launchpad id
        final String launchpadKey = ProcessDescriptionProvider.getInstance().getId(configuration.getId());

        // start launchpad
        ProcessDescription cfg = this.start(launchpad);

        // Add thread hook to shutdown launchpad
        if (environment.isShutdownOnExit()) {
            cfg.installShutdownHook();
        }

        // Add configuration to the config provider
        ProcessDescriptionProvider.getInstance().addRunConfiguration(cfg, launchpadKey);

        logger.info("Before Check if started");
        boolean started = false;
        try {
            final long endTime = System.currentTimeMillis() + this.environment.getReadyTimeOutSec() * 1000;
            boolean finished = false;
            while ( !started && !finished && System.currentTimeMillis() < endTime ) {
                Thread.sleep(5000);
                logger.info("Ask Control Listener: " + cfg.getControlClient());
                started = cfg.getControlClient().isStarted();
                logger.info("Is Started: " + started);
                try {
                    // if we get an exit value, the process has stopped
                    cfg.getProcess().exitValue();
                    finished = true;
                } catch ( final IllegalThreadStateException itse) {
                    // everything as expected
                }
                
            }
            logger.info("Check Done, started: " + started + ", finihsed: " + finished);

            if ( finished ) {
                throw new Exception("Launchpad did exit unexpectedly.");
            }
            if ( !started ) {
                throw new Exception("Launchpad did not start successfully in " + this.environment.getReadyTimeOutSec() + " seconds.");
            }
            // now check for the availability of the HTTP port
            boolean httpAvailable = isLocalhostPortAvailable(Integer.valueOf(this.configuration.getPort()));
            // repeat until http service is up as well
            while ( !httpAvailable && System.currentTimeMillis() < endTime ) {
                Thread.sleep(1000);
                httpAvailable = isLocalhostPortAvailable(Integer.valueOf(this.configuration.getPort()));
            }
            if ( !httpAvailable ) {
                throw new Exception("Launchpad did not start http service on port " + this.configuration.getPort() + " successfully in " + this.environment.getReadyTimeOutSec() + " seconds.");
            }
            this.logger.info("Started Launchpad '" + configuration.getId() +
                    "' at port " + configuration.getPort()+ " [run modes: " + configuration.getRunmode()+ "]");
        } finally {
//            // stop control port
//            cfg.getControlClient().shutdownServer();

            // call launchpad stop routine if not properly started
            if (!started) {
                stop(this.logger, cfg);
                ProcessDescriptionProvider.getInstance().removeRunConfiguration(cfg.getId());
                cfg = null;
            }
        }

        return cfg;
    }

    private boolean isLocalhostPortAvailable(int port) throws IOException {
        // https://stackoverflow.com/questions/46436813/difference-between-a-connection-refused-exception-and-a-timeout-in-httpclient
        Socket clientSocket = new Socket();
        try {
            clientSocket.connect(new InetSocketAddress("127.0.0.1", port), 500);
            // without that, read() call on the InputStream associated with this Socket is infinite
            this.logger.debug("Successfully connected to localhost, port " + port);
            clientSocket.close();
            return true;
        } catch (SocketTimeoutException e) {
            // we ran into a timeout (port most probably blocked by firewall)
            this.logger.debug("Ran into a timeout while connecting to localhost, port " + port, e);
            return false;
        } catch (ConnectException e) {
            // port not bound
            this.logger.debug("Could not connect to localhost, port " + port, e);
            return false;
        } finally {
            clientSocket.close();
        }
    }

    public boolean isRunning() {
        return getControlPortFile(this.configuration.getFolder()).exists();
    }

    private void add(final List<String> args, final String value) {
        if ( value != null ) {
            final String[] single = value.trim().split(" ");
            for(final String v : single) {
                if ( v.trim().length() > 0 ) {
                    args.add(v.trim());
                }
            }
        }
    }

    private ProcessDescription start(final File jar) throws Exception {
        final ProcessDescription cfg = new ProcessDescription(
            this.configuration.getId(),
            this.configuration.getFolder(),
            this.configuration.getServer() + ":" + this.configuration.getControlPort(),
            logger
        );

        final ProcessBuilder builder = new ProcessBuilder();
        final List<String> args = new ArrayList<String>();

        String javaHome = System.getenv("JAVA_HOME");
        String javaCmd = javaHome != null ? Paths.get(javaHome, "bin", "java").toString() : "java";

        args.add(javaCmd);
        add(args, this.configuration.getVmOpts());
        add(args, this.configuration.getVmDebugOpts(this.environment.getDebug()));

        args.add("-cp");
        args.add("bin");
        args.add(Main.class.getName());
        // first three arguments: jar, listener port, verbose
        args.add(jar.getPath());
        args.add(String.valueOf(cfg.getControlClient().getPort()));
        args.add("true");

        // from here on launchpad properties
        add(args, this.configuration.getOpts());

        if(this.configuration.getAdditionalFeatureFile() != null) {
            args.add("-af");
            args.add(this.configuration.getAdditionalFeatureFile().getAbsolutePath());
        }

        final String contextPath = this.configuration.getContextPath();
        if ( contextPath != null && contextPath.length() > 0 && !contextPath.equals("/") ) {
            args.add("-r");
            args.add(contextPath);
        }

        if ( this.configuration.getPort() != null ) {
            args.add("-p");
            args.add(this.configuration.getPort());
        }

        if ( this.configuration.getControlPort() != null ) {
            args.add("-j");
            args.add(this.configuration.getControlPort());
        }
        if ( this.configuration.getRunmode() != null && this.configuration.getRunmode().length() > 0 ) {
            args.add("-Dsling.run.modes=" + this.configuration.getRunmode());
        }
        logger.info("Is Shutdown On Exit: " + this.environment.isShutdownOnExit());
        if ( !this.environment.isShutdownOnExit() ) {
            args.add("start");
        }

        builder.command(args.toArray(new String[args.size()]));
        builder.directory(this.configuration.getFolder());
        builder.redirectErrorStream(true);
        logger.info("Starting Launchpad " + this.configuration.getId() +  "...");
        logger.info("Starting Launchpad, arguments: " + args);
        String stdOutFile = this.configuration.getStdOutFile();
        if (StringUtils.isNotBlank(stdOutFile)) {
            File absoluteStdOutFile = new File(builder.directory(), stdOutFile);
            // make sure to create the parent directories (if they do not exist yet)
            absoluteStdOutFile.getParentFile().mkdirs();
            builder.redirectOutput(absoluteStdOutFile);
            logger.info("Redirecting stdout and stderr to " + absoluteStdOutFile);
        } else {
            builder.redirectOutput(Redirect.INHERIT);
        }

        logger.debug("Launchpad cmd: " + builder.command());
        logger.debug("Launchpad dir: " + builder.directory());

        try {
            logger.info("Before Builder start()");
            cfg.setProcess(builder.start());
            logger.info("After Builder start(), cfg: " + cfg);
        } catch (final IOException e) {
            if (cfg.getProcess() != null) {
                cfg.getProcess().destroy();
                cfg.setProcess(null);
            }
            throw new Exception("Could not start the Launchpad", e);
        }

        return cfg;
    }

    public static void stop(final Log LOG, final ProcessDescription cfg) throws Exception {
        boolean isNew = false;

        if (cfg.getProcess() != null || isNew ) {
            LOG.info("Stopping Launchpad '" + cfg.getId() + "'");
            boolean destroy = true;
            final int twoMinutes = 2 * 60 * 1000;
            final File controlPortFile = getControlPortFile(cfg.getDirectory());
            LOG.debug("Control port file " + controlPortFile + " exists: " + controlPortFile.exists());
            if ( controlPortFile.exists() ) {
                // reading control port
                int controlPort = -1;
                String secretKey = null;
                LineNumberReader lnr = null;
                String serverName = null;
                try {
                    lnr = new LineNumberReader(new FileReader(controlPortFile));
                    final String portLine = lnr.readLine();
                    final int pos = portLine.indexOf(':');
                    controlPort = Integer.parseInt(portLine.substring(pos + 1));
                    if ( pos > 0 ) {
                        serverName = portLine.substring(0, pos);
                    }
                    secretKey = lnr.readLine();
                } catch ( final NumberFormatException ignore) {
                    // we ignore this
                    LOG.debug("Error reading control port file " + controlPortFile, ignore);
                } catch ( final IOException ignore) {
                    // we ignore this
                    LOG.debug("Error reading control port file " + controlPortFile, ignore);
                } finally {
                    IOUtils.closeQuietly(lnr);
                }

                if ( controlPort != -1 ) {
                    final List<String> hosts = new ArrayList<String>();
                    if ( serverName != null ) {
                        hosts.add(serverName);
                    }
                    hosts.add("localhost");
                    hosts.add("127.0.0.1");
                    LOG.debug("Found control port " + controlPort);
                    int index = 0;
                    while ( destroy && index < hosts.size() ) {
                        final String hostName = hosts.get(index);

                        Socket clientSocket = null;
                        DataOutputStream out = null;
                        BufferedReader in = null;
                        try {
                            LOG.debug("Trying to connect to " + hostName + ":" + controlPort);
                            clientSocket = new Socket();
                            // set a socket timeout
                            clientSocket.connect(new InetSocketAddress(hostName, controlPort), twoMinutes);
                            // without that, read() call on the InputStream associated with this Socket is infinite
                            clientSocket.setSoTimeout(twoMinutes);

                            LOG.debug(hostName + ":" + controlPort + " connection estabilished, sending the 'stop' command...");

                            out = new DataOutputStream(clientSocket.getOutputStream());
                            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                            if (secretKey != null) {
                                out.writeBytes(secretKey);
                                out.write(' ');
                            }
                            out.writeBytes("stop\n");
                            in.readLine();
                            destroy = false;
                            LOG.debug("'stop' command sent to " + hostName + ":" + controlPort);
                        } catch (final Throwable ignore) {
                            // catch Throwable because InetSocketAddress and Socket#connect throws unchecked exceptions
                            // we ignore this for now
                            LOG.debug("Error sending 'stop' command to " + hostName + ":" + controlPort + " due to: " + ignore.getMessage());
                        } finally {
                            IOUtils.closeQuietly(in);
                            IOUtils.closeQuietly(out);
                            IOUtils.closeQuietly(clientSocket);
                        }
                        index++;
                    }
                }
            }
            if ( cfg.getProcess() != null ) {
                final Process process = cfg.getProcess();

                if (!destroy) {
                    LOG.debug("Waiting for process to stop...");
                    process.waitFor(twoMinutes, TimeUnit.MILLISECONDS);
                    if (process.isAlive()) {
                        LOG.debug("Process timeout out after 2 minutes");
                        destroy = true;
                    } else {
                        LOG.debug("Process stopped");
                    }
                }

                if (destroy) {
                    LOG.debug("Destroying process...");
                    process.destroy();
                    process.waitFor(twoMinutes, TimeUnit.MILLISECONDS);
                    LOG.debug("Process destroyed");
                }

                cfg.setProcess(null);
            }
        } else {
            LOG.warn("Launchpad already stopped");
        }
    }

    private static File getControlPortFile(final File directory) {
        final File confDir = new File(directory, "conf");
        final File controlPortFile = new File(confDir, "controlport");
        return controlPortFile;
    }
}
