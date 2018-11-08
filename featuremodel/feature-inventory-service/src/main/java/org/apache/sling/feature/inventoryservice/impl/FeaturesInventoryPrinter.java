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
package org.apache.sling.feature.inventoryservice.impl;

import org.apache.felix.inventory.Format;
import org.apache.felix.inventory.InventoryPrinter;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component(
property = {InventoryPrinter.NAME + "=launch_features",
        InventoryPrinter.TITLE + "=Launch Features",
        InventoryPrinter.FORMAT + "=JSON"})
public class FeaturesInventoryPrinter implements InventoryPrinter
{
    @Activate
    BundleContext bc;

    @Override
    public void print(PrintWriter printWriter, Format format, boolean isZip) {
        try {
            Path path = Paths.get(new URI(bc.getProperty("sling.feature")));
            byte[] bytes = Files.readAllBytes(path);
            printWriter.print(new String(bytes));
        } catch (Exception e) {
            e.printStackTrace(printWriter);
        }
        printWriter.println();
    }
}
