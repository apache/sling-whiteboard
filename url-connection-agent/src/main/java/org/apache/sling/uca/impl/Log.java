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

import static java.util.Objects.requireNonNull;

import java.util.Formatter;

/**
 * Simple logger abstraction
 * 
 * <p>This is an intentionally simple and simplistic logger for agent-internal usage. Since the agent environment
 * is limited and can make no assumption about how it will be used, it only uses the console and no
 * external libraries.</p>
 * 
 * <p>It is required to call {@link #configure(String)} before accessing the log instance using {@link #get()}.</p>
 *
 */
abstract class Log {
    
    private static Log INSTANCE;

    /**
     * Configures the global logger instance
     * 
     * @param spec the logger spec, <tt>v</tt> for a console log, anything else for a no-op log
     */
    public static void configure(String spec) {
        INSTANCE = "v".equals(spec) ? new ConsoleLog() : new NoopLog();
    }
    
    /**
     * Gets the global logger instance, configured with {@link #configure(String)}
     * 
     * @return the global logger instance
     * @throws NullPointerException in case the logger is not configured
     */
    public static Log get() {
        return requireNonNull(INSTANCE, "Log is null, did you foget to call Log.configure() ?");
    }

    private Log() {

    }

    /**
     * Logs a message
     * 
     * <p>The message and the arguments are interpolated using a {@link Formatter}, e.g.
     * 
     * <pre>Logger.get().log("Transforming %s", klazz.getName());</pre>
     * 
     *  </p>
     *  
     *  <p>The line separator <tt>%n</tt> is automatically appended to the message.</p>
     * 
     * @param msg the message
     * @param args the arguments
     */
    public abstract void log(String msg, Object... args);
    
    /**
     * Prints the throwable stack trace and throws a <tt>RuntimeException</tt>
     * 
     * @param message the message to include in the <tt>RuntimeException</tt>
     * @param t the throwable to print the stack trace for 
     */
    public abstract void fatal(String message, Throwable t);

    static class ConsoleLog extends Log {

        private static final String LOG_ENTRY_PREFIX = "[AGENT] ";

        @Override
        public void log(String msg, Object... args) {
            System.out.format(LOG_ENTRY_PREFIX + msg + " %n", args);
        }
        
        @Override
        public void fatal(String msg, Throwable t) {
            t.printStackTrace(); // ensure _something_ is printed
            throw new RuntimeException(LOG_ENTRY_PREFIX + msg, t);
            
        }
    }
    
    static class NoopLog extends Log {

        @Override
        public void log(String msg, Object... args) {
            // empty by design
        }
        
        @Override
        public void fatal(String message, Throwable t) {
            // empty by design
        }
    }
}
