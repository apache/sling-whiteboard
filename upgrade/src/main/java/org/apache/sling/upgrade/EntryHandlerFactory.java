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
package org.apache.sling.upgrade;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarEntry;

/**
 * A service factory for Entry handlers to implement. These services are called
 * to process entries to upgrade an Apache Sling instance.
 */
public interface EntryHandlerFactory<E extends UpgradeEntry> {

    /**
     * Returns true if the entry matches the requirements for this entry provider
     * and should be loaded.
     * 
     * @param entry the JarEntry to check
     * @return true if matches, false otherwise
     */
    boolean matches(JarEntry entry);

    /**
     * Load an upgrade entry from the Specified Jar entry.
     * 
     * @param entry the entry to to load the upgrade entry from
     * @param is    the input stream to load the entry contents from
     * @return the upgrade entry
     * @throws IOException
     */
    E loadEntry(JarEntry entry, InputStream is) throws IOException;

    /**
     * Process a single upgrade entry and return the result of processing the entry.
     * 
     * @param entry the entry to process
     * @return the result of processing the entry
     */
    UpgradeResultEntry<E> processEntry(E entry);
}
