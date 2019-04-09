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
package org.apache.sling.feature.apiregions;

import static java.util.Objects.requireNonNull;

import java.util.Formatter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public final class ApiRegion implements Iterable<String> {

    private static final Pattern PACKAGE_NAME_VALIDATION =
            Pattern.compile("^[a-z]+(\\.[a-zA-Z_][a-zA-Z0-9_]*)*$");

    private static final String PACKAGE_DELIM = ".";

    private static final Set<String> KEYWORDS = new HashSet<>();

    static {
        KEYWORDS.add("abstract");
        KEYWORDS.add("continue");
        KEYWORDS.add("for");
        KEYWORDS.add("new");
        KEYWORDS.add("switch");
        KEYWORDS.add("assert");
        KEYWORDS.add("default");
        KEYWORDS.add("package");
        KEYWORDS.add("synchronized");
        KEYWORDS.add("boolean");
        KEYWORDS.add("do");
        KEYWORDS.add("if");
        KEYWORDS.add("private");
        KEYWORDS.add("this");
        KEYWORDS.add("break");
        KEYWORDS.add("double");
        KEYWORDS.add("implements");
        KEYWORDS.add("protected");
        KEYWORDS.add("throw");
        KEYWORDS.add("byte");
        KEYWORDS.add("else");
        KEYWORDS.add("import");
        KEYWORDS.add("public");
        KEYWORDS.add("throws");
        KEYWORDS.add("case");
        KEYWORDS.add("enum");
        KEYWORDS.add("instanceof");
        KEYWORDS.add("return");
        KEYWORDS.add("transient");
        KEYWORDS.add("catch");
        KEYWORDS.add("extends");
        KEYWORDS.add("int");
        KEYWORDS.add("short");
        KEYWORDS.add("try");
        KEYWORDS.add("char");
        KEYWORDS.add("final");
        KEYWORDS.add("interface");
        KEYWORDS.add("static");
        KEYWORDS.add("void");
        KEYWORDS.add("class");
        KEYWORDS.add("finally");
        KEYWORDS.add("long");
        KEYWORDS.add("strictfp");
        KEYWORDS.add("volatile");
        KEYWORDS.add("float");
        KEYWORDS.add("native");
        KEYWORDS.add("super");
        KEYWORDS.add("while");
    }

    private final Set<String> apis = new HashSet<>();

    private final String name;

    private final ApiRegion parent;

    protected ApiRegion(String name, ApiRegion parent) {
        this.name = name;
        this.parent = parent;
    }

    public String getName() {
        return name;
    }

    public ApiRegion getParent() {
        return parent;
    }

    public void addAll(Iterable<String> apis) {
        requireNonNull(apis, "Impossible to import null APIs");

        for (String api : apis) {
            add(api);
        }
    }

    public boolean add(String api) {
        // ignore null, empty package and non well-formed packages names, i.e. javax.jms.doc-files
        if (isEmpty(api) || !PACKAGE_NAME_VALIDATION.matcher(api).matches()) {
            // ignore it
            return false;
        }

        // ignore packages with reserved keywords, i.e. org.apache.commons.lang.enum
        StringTokenizer tokenizer = new StringTokenizer(api, PACKAGE_DELIM);
        while (tokenizer.hasMoreTokens()) {
            String apiPart = tokenizer.nextToken();
            if (KEYWORDS.contains(apiPart)) {
                return false;
            }
        }

        if (contains(api)) {
            return false;
        }

        return apis.add(api);
    }

    public boolean contains(String api) {
        if (isEmpty(api)) {
            return false;
        }

        if (apis.contains(api)) {
            return true;
        }

        if (parent != null) {
            return parent.contains(api);
        }

        return false;
    }

    public boolean isEmpty() {
        if (!apis.isEmpty()) {
            return false;
        }

        if (parent != null) {
            return parent.isEmpty();
        }

        return true;
    }

    public boolean remove(String api) {
        if (isEmpty(api)) {
            return false;
        }

        if (apis.remove(api)) {
            return true;
        }

        if (parent != null) {
            return parent.remove(api);
        }

        return false;
    }

    @Override
    public Iterator<String> iterator() {
        List<Iterable<String>> iterators = new LinkedList<>();

        ApiRegion region = this;
        while (region != null) {
            iterators.add(region.apis);
            region = region.getParent();
        }

        return new JoinedIterator<String>(iterators.iterator());
    }

    public Iterable<String> getExports() {
        return apis;
    }

    @Override
    public String toString() {
        Formatter formatter = new Formatter();
        formatter.format("Region '%s'", name);

        if (parent != null) {
            formatter.format(" inherits from %n")
                     .format(parent.toString());
        }

        for (String api : apis) {
            formatter.format("%n * %s", api);
        }

        String toString = formatter.toString();
        formatter.close();

        return toString;
    }

    private static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

}
