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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;

import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.io.ModelReader;
import org.apache.sling.repoinit.parser.RepoInitParser;
import org.apache.sling.repoinit.parser.RepoInitParsingException;
import org.apache.sling.repoinit.parser.operations.Operation;

/**
 * Represents a repoinit file.
 */
public class RepoInitEntry extends UpgradeEntry {

    private final Model model;
    private final Map<String,List<Operation>> repoinits = new HashMap<>();

    public RepoInitEntry(JarEntry entry, InputStream is, RepoInitParser parser)
            throws IOException, RepoInitParsingException {
        super(entry, is);
        model = ModelReader.read(new InputStreamReader(new ByteArrayInputStream(super.getContents())), null);
        model.getFeatures().forEach(f -> f.getAdditionalSections("repoinit").forEach(r -> {
            try {
                repoinits.put(f.getName(), parser.parse(new StringReader(r.getContents())));
            } catch (RepoInitParsingException e) {
                throw new RuntimeException("Failed to parse repoinit file",e);
            }
        }));

    }
    
    @Override
    public int compareTo(UpgradeEntry o) {
        if (o instanceof RepoInitEntry) {
            return this.getOriginalName().compareTo(o.getOriginalName());
        } else {
            return getClass().getName().compareTo(o.getClass().getName());
        }
    }

    public Model getModel() {
        return model;
    }

    public Map<String,List<Operation>> getRepoInits(){
        return repoinits;
    }

}
