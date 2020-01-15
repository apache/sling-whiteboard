/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.maven.slingstart.feature.run;

import org.apache.maven.plugin.logging.Log;
import org.apache.sling.feature.starter.app.ControlTarget;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.math.BigInteger;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The <code>ControlListener</code> class is a helper class for the {@link ControlTarget}
 * class to support in Sling standalone application process communication. This
 * class implements the client and server sides of a TCP/IP based communication
 * channel to control a running Sling application.
 * <p>
 * The server side listens for commands on a configurable host and port &endash;
 * <code>localhost:63000</code> by default &endash; supporting the following
 * commands:
 * <table>
 * <tr>
 * <th>Command</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td><code>status</code></td>
 * <td>Request status information. Currently only <i>OK</i> is sent back. If no
 * connection can be created to the server the client assumes Sling is not
 * running.</td>
 * </tr>
 * <tr>
 * <td><code>stop</code></td>
 * <td>Requests Sling to shutdown.</td>
 * </tr>
 * </table>
 */
public class ControlClient {

    // command sent by the client to cause Sling to shutdown
    static final String COMMAND_STOP = "stop";

    // command sent by the client to check for the status of the server
    static final String COMMAND_STATUS = "status";

    // command sent by the client to request a thread dump
    static final String COMMAND_THREADS = "threads";

    // the response sent by the server if the command executed successfully
    private static final String RESPONSE_OK = "OK";

    // the status response sent by the server when shutting down
    private static final String RESPONSE_STOPPING = "STOPPING";

    // The default interface to listen on
    private static final String DEFAULT_LISTEN_INTERFACE = "127.0.0.1";

    // The default port to listen on and to connect to - we select it randomly
    private static final int DEFAULT_LISTEN_PORT = 0;

    private final String listenSpec;

    private String secretKey;
    private InetSocketAddress socketAddress;
    private File directory;
    private Log logger;

    /**
     * Creates an instance of this control support class.
     * <p>
     * The host (name or address) and port number of the socket is defined by
     * the <code>listenSpec</code> parameter. This parameter is defined as
     * <code>[ host ":" ] port</code>. If the parameter is empty or
     * <code>null</code> it defaults to <i>localhost:0</i>. If the host name
     * is missing it defaults to <i>localhost</i>.
     *
     * @param listenSpec The specification for the host and port for the socket
     *            connection. See above for the format of this parameter.
     */
    public ControlClient(final String listenSpec, final File directory, Log logger) {
        this.listenSpec = listenSpec; // socketAddress = this.getSocketAddress(listenSpec, selectNewPort);
        this.directory = directory;
        this.logger = logger;
    }

    public int getPort() {
        return socketAddress != null ? socketAddress.getPort() : -1;
    }

    public boolean isStarted() {
        Response response = sendCommand(COMMAND_STATUS);
        return response.getCode() == 0 && "OK".equals(response.getResult());
    }

    /**
     * Implements the client side of the control connection sending the command
     * to shutdown Sling.
     */
    public int shutdownServer() { return sendCommand(COMMAND_STOP).getCode(); }

    /**
     * Implements the client side of the control connection sending the command
     * to check whether Sling is active.
     */
    public int statusServer() {
        return sendCommand(COMMAND_STATUS).getCode();
    }

    /**
     * Implements the client side of the control connection sending the command
     * to retrieve a thread dump.
     */
    public int dumpThreads() {
        return sendCommand(COMMAND_THREADS).getCode();
    }

    /**
     * Sends the given command to the server indicated by the configured
     * socket address and logs the reply.
     *
     * @param command The command to send
     *
     * @return A code indicating success of sending the command.
     */
    private Response sendCommand(final String command) {
        if (configure()) {
            if (this.secretKey == null) {
                logger.info("Missing secret key to protect sending '" + command + "' to " + this.socketAddress);
                return new Response(4);
            }

            Socket socket = null;
            try {
                socket = new Socket();
                socket.connect(this.socketAddress);
                writeLine0(socket, this.secretKey + " " + command);
                final String result = readLine(socket);
                logger.info("Sent '" + command + "' to " + this.socketAddress + ": " + result);
                return new Response(0, result);
            } catch (final ConnectException ce) {
                logger.info("No Apache Sling running at " + this.socketAddress);
                return new Response(3, ce);
            } catch (final IOException ioe) {
                logger.error("Failed sending '" + command + "' to " + this.socketAddress, ioe);
                return new Response(1, ioe);
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }
        logger.info("No socket address to send '" + command + "' to");
        return new Response(4);
    }

    private String readLine(final Socket socket) throws IOException {
        final BufferedReader br = new BufferedReader(new InputStreamReader(
            socket.getInputStream(), "UTF-8"));

        StringBuilder b = new StringBuilder();
        boolean more = true;
        while (more) {
            String s = br.readLine();
            if (s != null && s.startsWith("-")) {
                s = s.substring(1);
            } else {
                more = false;
            }
            if (b.length() > 0) {
                b.append("\r\n");
            }
            b.append(s);
        }

        return b.toString();
    }

//    private void writeLine(final Socket socket, final String line) throws IOException {
//        logger.info(socket.getRemoteSocketAddress() + "<" + line);
//        this.writeLine0(socket, line);
//    }

    private void writeLine0(final Socket socket, final String line) throws IOException {
        final BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
        bw.write(line);
        bw.write("\r\n");
        bw.flush();
    }

    /**
     * Read the port from the config file
     * @return The port or null
     */
    private boolean configure() {
        boolean result = false;
        final File configFile = this.getConfigFile();
        if (configFile.canRead()) {
            try ( final LineNumberReader lnr = new LineNumberReader(new FileReader(configFile))) {
                this.socketAddress = getSocketAddress(lnr.readLine());
                this.secretKey = lnr.readLine();
                result = true;
            } catch (final IOException ignore) {
                // ignore
            }
        }

        return result;
    }

    private File getConfigFile() {
        final File configDir = new File(directory, "conf");
        return new File(configDir, "controlport");
    }

    /**
     * Read the port from the config file
     * @return The port or null
     */
    private boolean configure(final boolean fromConfigFile) {
        this.socketAddress = getSocketAddress(this.listenSpec);
        this.secretKey = generateKey();

        return true;
    }

    private static String generateKey() {
         return new BigInteger(165, new SecureRandom()).toString(32);
    }

    /**
     * Return the control port file
     */
//    private File getConfigFile() {
//        final File configDir = new File(this.controlTarget.getHome(), "conf");
//        return new File(configDir, "controlport");
//    }

    private InetSocketAddress getSocketAddress(String listenSpec) {
        try {

            final String address;
            final int port;
            if (listenSpec == null) {
                address = DEFAULT_LISTEN_INTERFACE;
                port = DEFAULT_LISTEN_PORT;
            } else {
                final int colon = listenSpec.indexOf(':');
                if (colon < 0) {
                    address = DEFAULT_LISTEN_INTERFACE;
                    port = Integer.parseInt(listenSpec);
                } else {
                    address = listenSpec.substring(0, colon);
                    port = Integer.parseInt(listenSpec.substring(colon + 1));
                }
            }

            final InetSocketAddress addr = new InetSocketAddress(address, port);
            if (!addr.isUnresolved()) {
                return addr;
            }

            logger.error("Unknown host in '" + listenSpec);
        } catch (final NumberFormatException nfe) {
            logger.error("Cannot parse port number from '" + listenSpec + "'");
        }

        return null;
    }

    private static class Response {
        private int code;
        private String result;
        private Exception exception;

        public Response(int code) {
            this.code = code;
        }

        public Response(int code, String result) {
            this.code = code;
            this.result = result;
        }

        public Response(int code, Exception exception) {
            this.code = code;
            this.exception = exception;
        }

        public int getCode() {
            return code;
        }

        public String getResult() {
            return result;
        }

        public Exception getException() {
            return exception;
        }
    }
//    private void writePortToConfigFile(final File configFile, final InetSocketAddress socketAddress,
//            final String secretKey) {
//        configFile.getParentFile().mkdirs();
//        FileWriter fw = null;
//        try {
//            fw = new FileWriter(configFile);
//            fw.write(socketAddress.getAddress().getHostAddress());
//            fw.write(':');
//            fw.write(String.valueOf(socketAddress.getPort()));
//            fw.write('\n');
//            fw.write(secretKey);
//            fw.write('\n');
//        } catch (final IOException ignore) {
//            // ignore
//        } finally {
//            if (fw != null) {
//                try {
//                    fw.close();
//                } catch (final IOException ignore) {
//                }
//            }
//        }
//    }
}
