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
package org.apache.sling.graphql.schema.aggregator.impl;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.input.BoundedReader;

/** Reader for the partials format, which parses a partial file and
 *  provides access to its sections.
 *  See the example.partial.txt and the tests for a description of
 *  the format.
  */
class PartialReader implements Partial {
    private static final Pattern SECTION_LINE = Pattern.compile("([A-Z]+) *:(.*)");
    private static final int EOL = '\n';

    private final Map<String, Section> sections = new HashMap<>();
    private final String name;

    /** The PARTIAL section is the only required one */
    public static final String PARTIAL_SECTION = "PARTIAL";

    static class SyntaxException extends IOException {
        SyntaxException(String reason) {
            super(reason);
        }
    }

    static class ParsedSection implements Partial.Section {
        private final Supplier<Reader> sectionSource;
        private final String name;
        private final String description;
        private final int startCharIndex;
        private final int endCharIndex;

        ParsedSection(Supplier<Reader> sectionSource, String name, String description, int start, int end) {
            this.sectionSource = sectionSource;
            this.name = name;
            this.description = description;
            this.startCharIndex = start;
            this.endCharIndex = end;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public Reader getContent() throws IOException {
            final Reader r = sectionSource.get();
            r.skip(startCharIndex);
            return new BoundedReader(r, endCharIndex - startCharIndex);
        }
    }
    
    PartialReader(String name, Supplier<Reader> source) throws IOException {
        this.name = name;
        parse(source);
    }

    /* Detect lines that start with a <SECTION>: name
     *  in the input, and save them as sections
     */
    private void parse(Supplier<Reader> source) throws IOException {
        final Reader input = source.get();
        StringBuilder line = new StringBuilder();
        int c;
        int charCount = 0;
        int lastSectionStart = 0;
        String sectionName = null;
        String sectionDescription = "";
        while((c = input.read()) != -1) {
            if(c == EOL) {
                final Matcher m = SECTION_LINE.matcher(line);
                if(m.matches()) {
                    // Add previous section
                    addSectionIfNameIsSet(source, sectionName, sectionDescription, lastSectionStart, charCount - line.length());
                    // And setup for the new section
                    sectionName = m.group(1).trim();
                    sectionDescription = m.group(2).trim();
                    lastSectionStart = charCount + 1;
                }
                line = new StringBuilder();
            } else {
                line.append((char)c);
            }
            charCount++;
        }

        // Add last section
        addSectionIfNameIsSet(source, sectionName, sectionDescription, lastSectionStart, Integer.MAX_VALUE);

        // And validate
        if(!sections.containsKey(PARTIAL_SECTION)) {
            throw new SyntaxException(String.format("Missing required %s section", PARTIAL_SECTION));
        }
        
    }

    private void addSectionIfNameIsSet(Supplier<Reader> sectionSource, String name, String description, int start, int end) throws SyntaxException {
        if(name != null) {
            if(sections.containsKey(name)) {
                throw new SyntaxException(String.format("Duplicate section %s", name));
            }
            sections.put(name, new ParsedSection(sectionSource, name, description, start, end));
        }
    }

    @Override
    public Optional<Section> getSection(String name) {
        final Section s = sections.get(name);
        return s == null ? Optional.empty() : Optional.of(s);
    }

    @Override
    public String getName() {
        return name;
    }
}