/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.cp2fm.handlers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.Archive.Entry;
import org.apache.sling.cp2fm.ContentPackage2FeatureModelConverter;
import org.apache.sling.cp2fm.spi.EntryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DefaultEntryHandler implements EntryHandler {

    public static final String TMP_DEFLATED = "tmp-deflated";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public boolean matches(String path) {
        return true;
    }

    @Override
    public void handle(String path, Archive archive, Entry entry, ContentPackage2FeatureModelConverter converter) throws Exception {
        File deflatedDir = new File(converter.getOutputDirectory(), TMP_DEFLATED);
        File target = new File(deflatedDir, path);

        target.getParentFile().mkdirs();

        try (InputStream input = archive.openInputStream(entry);
                OutputStream output = new FileOutputStream(target)) {
            logger.info("Copying {} archived resource to {}...", path, target);

            IOUtils.copy(input, output);
        }
    }

}
