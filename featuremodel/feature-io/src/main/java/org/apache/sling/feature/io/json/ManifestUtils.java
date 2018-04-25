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
package org.apache.sling.feature.io.json;

import org.apache.felix.utils.resource.CapabilityImpl;
import org.osgi.framework.BundleException;
import org.osgi.framework.Version;
import org.osgi.resource.Capability;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

// This class can be picked up from Felix Utils once it has been moved there. At that point
// this class can be removed.
class ManifestUtils {
    public static void unmarshalAttribute(String key, Object value, BiConsumer<String, Object> sink) throws IOException {
        unmarshal(key + "=" + value, Capability::getAttributes, sink);
    }

    public static void unmarshalDirective(String key, Object value, BiConsumer<String, String> sink) throws IOException {
        unmarshal(key + ":=" + value, Capability::getDirectives, sink);
    }

    private static <T> void unmarshal(String header, Function<Capability, Map<String, T>> lookup, BiConsumer<String, T> sink) throws IOException {
        try {
            convertProvideCapabilities(
                    normalizeCapabilityClauses(parseStandardHeader("foo;" + header), "2"))
                    .forEach(capability -> lookup.apply(capability).forEach(sink));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    public static void marshalAttribute(String key, Object value, BiConsumer<String, String> sink) {
        marshal(key, value, sink);
    }

    public static void marshalDirective(String key, Object value, BiConsumer<String, String> sink) {
        marshal(key, value, sink);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void marshal(String key, Object value, BiConsumer<String, String> sink) {
        StringBuilder keyBuilder = new StringBuilder(key);
        if (value instanceof  List) {
            List list = (List) value;
            keyBuilder.append(":List");
            if (!list.isEmpty()) {
                String type = type(list.get(0));
                if (!type.equals("String")) {
                    keyBuilder.append('<').append(type).append('>');
                }
                value = list.stream().map(
                        v -> v.toString().replace(",", "\\,")
                ).collect(Collectors.joining(","));
            }
            else {
                value = "";
            }
        }
        else {
            String type = type(value);
            if (!type.equals("String")) {
                keyBuilder.append(':').append(type);
            }
        }
        sink.accept(keyBuilder.toString(), value.toString());
    }

    private static String type(Object value) {
        if (value instanceof Long) {
            return "Long";
        }
        else if (value instanceof Double)
        {
            return "Double";
        }
        else if (value instanceof Version)
        {
            return "Version";
        }
        else
        {
            return "String";
        }
    }

    public static List<Capability> convertProvideCapabilities(
            List<ParsedHeaderClause> clauses)
            throws BundleException
    {
        List<Capability> capList = new ArrayList<>();
        for (ParsedHeaderClause clause : clauses)
        {
            for (String path : clause.m_paths)
            {
                if (path.startsWith("osgi.wiring."))
                {
                    throw new BundleException("Manifest cannot use Provide-Capability for '"
                            + path
                            + "' namespace.");
                }

                Capability capability = new CapabilityImpl(null, path, clause.m_dirs, clause.m_attrs);
                // Create package capability and add to capability list.
                capList.add(capability);
            }
        }

        return capList;
    }

    public static List<ParsedHeaderClause> normalizeCapabilityClauses(
            List<ParsedHeaderClause> clauses, String mv)
            throws BundleException
    {

        if (!mv.equals("2") && !clauses.isEmpty())
        {
            // Should we error here if we are not an R4 bundle?
        }

        // Convert attributes into specified types.
        for (ParsedHeaderClause clause : clauses)
        {
            for (Entry<String, String> entry : clause.m_types.entrySet())
            {
                String type = entry.getValue();
                if (!type.equals("String"))
                {
                    if (type.equals("Double"))
                    {
                        clause.m_attrs.put(
                                entry.getKey(),
                                new Double(clause.m_attrs.get(entry.getKey()).toString().trim()));
                    }
                    else if (type.equals("Version"))
                    {
                        clause.m_attrs.put(
                                entry.getKey(),
                                new Version(clause.m_attrs.get(entry.getKey()).toString().trim()));
                    }
                    else if (type.equals("Long"))
                    {
                        clause.m_attrs.put(
                                entry.getKey(),
                                new Long(clause.m_attrs.get(entry.getKey()).toString().trim()));
                    }
                    else if (type.startsWith("List"))
                    {
                        int startIdx = type.indexOf('<');
                        int endIdx = type.indexOf('>');
                        if (((startIdx > 0) && (endIdx <= startIdx))
                                || ((startIdx < 0) && (endIdx > 0)))
                        {
                            throw new BundleException(
                                    "Invalid Provide-Capability attribute list type for '"
                                            + entry.getKey()
                                            + "' : "
                                            + type);
                        }

                        String listType = "String";
                        if (endIdx > startIdx)
                        {
                            listType = type.substring(startIdx + 1, endIdx).trim();
                        }

                        List<String> tokens = parseDelimitedString(
                                clause.m_attrs.get(entry.getKey()).toString(), ",", false);
                        List<Object> values = new ArrayList<>(tokens.size());
                        for (String token : tokens)
                        {
                            if (listType.equals("String"))
                            {
                                values.add(token);
                            }
                            else if (listType.equals("Double"))
                            {
                                values.add(new Double(token.trim()));
                            }
                            else if (listType.equals("Version"))
                            {
                                values.add(new Version(token.trim()));
                            }
                            else if (listType.equals("Long"))
                            {
                                values.add(new Long(token.trim()));
                            }
                            else
                            {
                                throw new BundleException(
                                        "Unknown Provide-Capability attribute list type for '"
                                                + entry.getKey()
                                                + "' : "
                                                + type);
                            }
                        }
                        clause.m_attrs.put(
                                entry.getKey(),
                                values);
                    }
                    else
                    {
                        throw new BundleException(
                                "Unknown Provide-Capability attribute type for '"
                                        + entry.getKey()
                                        + "' : "
                                        + type);
                    }
                }
            }
        }

        return clauses;
    }

    private static final char EOF = (char) -1;

    private static char charAt(int pos, String headers, int length)
    {
        if (pos >= length)
        {
            return EOF;
        }
        return headers.charAt(pos);
    }

    private static final int CLAUSE_START = 0;
    private static final int PARAMETER_START = 1;
    private static final int KEY = 2;
    private static final int DIRECTIVE_OR_TYPEDATTRIBUTE = 4;
    private static final int ARGUMENT = 8;
    private static final int VALUE = 16;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static List<ParsedHeaderClause> parseStandardHeader(String header)
    {
        List<ParsedHeaderClause> clauses = new ArrayList<>();
        if (header == null)
        {
            return clauses;
        }
        ParsedHeaderClause clause = null;
        String key = null;
        Map targetMap = null;
        int state = CLAUSE_START;
        int currentPosition = 0;
        int startPosition = 0;
        int length = header.length();
        boolean quoted = false;
        boolean escaped = false;

        char currentChar = EOF;
        do
        {
            currentChar = charAt(currentPosition, header, length);
            switch (state)
            {
                case CLAUSE_START:
                    clause = new ParsedHeaderClause(
                            new ArrayList<>(),
                            new HashMap<>(),
                            new HashMap<>(),
                            new HashMap<>());
                    clauses.add(clause);
                    state = PARAMETER_START;
                case PARAMETER_START:
                    startPosition = currentPosition;
                    state = KEY;
                case KEY:
                    switch (currentChar)
                    {
                        case ':':
                        case '=':
                            key = header.substring(startPosition, currentPosition).trim();
                            startPosition = currentPosition + 1;
                            targetMap = clause.m_attrs;
                            state = currentChar == ':' ? DIRECTIVE_OR_TYPEDATTRIBUTE : ARGUMENT;
                            break;
                        case EOF:
                        case ',':
                        case ';':
                            clause.m_paths.add(header.substring(startPosition, currentPosition).trim());
                            state = currentChar == ',' ? CLAUSE_START : PARAMETER_START;
                            break;
                        default:
                            break;
                    }
                    currentPosition++;
                    break;
                case DIRECTIVE_OR_TYPEDATTRIBUTE:
                    switch(currentChar)
                    {
                        case '=':
                            if (startPosition != currentPosition)
                            {
                                clause.m_types.put(key, header.substring(startPosition, currentPosition).trim());
                            }
                            else
                            {
                                targetMap = clause.m_dirs;
                            }
                            state = ARGUMENT;
                            startPosition = currentPosition + 1;
                            break;
                        default:
                            break;
                    }
                    currentPosition++;
                    break;
                case ARGUMENT:
                    if (currentChar == '\"')
                    {
                        quoted = true;
                        currentPosition++;
                    }
                    else
                    {
                        quoted = false;
                    }
                    if (!Character.isWhitespace(currentChar)) {
                        state = VALUE;
                    }
                    else {
                        currentPosition++;
                    }
                    break;
                case VALUE:
                    if (escaped)
                    {
                        escaped = false;
                    }
                    else
                    {
                        if (currentChar == '\\' )
                        {
                            escaped = true;
                        }
                        else if (quoted && currentChar == '\"')
                        {
                            quoted = false;
                        }
                        else if (!quoted)
                        {
                            String value = null;
                            switch(currentChar)
                            {
                                case EOF:
                                case ';':
                                case ',':
                                    value = header.substring(startPosition, currentPosition).trim();
                                    if (value.startsWith("\"") && value.endsWith("\""))
                                    {
                                        value = value.substring(1, value.length() - 1);
                                    }
                                    if (targetMap.put(key, value) != null)
                                    {
                                        throw new IllegalArgumentException(
                                                "Duplicate '" + key + "' in: " + header);
                                    }
                                    state = currentChar == ';' ? PARAMETER_START : CLAUSE_START;
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                    currentPosition++;
                    break;
                default:
                    break;
            }
        } while ( currentChar != EOF);

        if (state > PARAMETER_START)
        {
            throw new IllegalArgumentException("Unable to parse header: " + header);
        }
        return clauses;
    }

    /**
     * Parses delimited string and returns an array containing the tokens. This
     * parser obeys quotes, so the delimiter character will be ignored if it is
     * inside of a quote. This method assumes that the quote character is not
     * included in the set of delimiter characters.
     * @param value the delimited string to parse.
     * @param delim the characters delimiting the tokens.
     * @return a list of string or an empty list if there are none.
     **/
    public static List<String> parseDelimitedString(String value, String delim, boolean trim)
    {
        if (value == null)
        {
            value = "";
        }

        List<String> list = new ArrayList<>();

        int CHAR = 1;
        int DELIMITER = 2;
        int STARTQUOTE = 4;
        int ENDQUOTE = 8;

        StringBuffer sb = new StringBuffer();

        int expecting = (CHAR | DELIMITER | STARTQUOTE);

        boolean isEscaped = false;
        for (int i = 0; i < value.length(); i++)
        {
            char c = value.charAt(i);

            boolean isDelimiter = (delim.indexOf(c) >= 0);

            if (!isEscaped && (c == '\\'))
            {
                isEscaped = true;
                continue;
            }

            if (isEscaped)
            {
                sb.append(c);
            }
            else if (isDelimiter && ((expecting & DELIMITER) > 0))
            {
                if (trim)
                {
                    list.add(sb.toString().trim());
                }
                else
                {
                    list.add(sb.toString());
                }
                sb.delete(0, sb.length());
                expecting = (CHAR | DELIMITER | STARTQUOTE);
            }
            else if ((c == '"') && ((expecting & STARTQUOTE) > 0))
            {
                sb.append(c);
                expecting = CHAR | ENDQUOTE;
            }
            else if ((c == '"') && ((expecting & ENDQUOTE) > 0))
            {
                sb.append(c);
                expecting = (CHAR | STARTQUOTE | DELIMITER);
            }
            else if ((expecting & CHAR) > 0)
            {
                sb.append(c);
            }
            else
            {
                throw new IllegalArgumentException("Invalid delimited string: " + value);
            }

            isEscaped = false;
        }

        if (sb.length() > 0)
        {
            if (trim)
            {
                list.add(sb.toString().trim());
            }
            else
            {
                list.add(sb.toString());
            }
        }

        return list;
    }

    static class ParsedHeaderClause
    {
        public final List<String> m_paths;
        public final Map<String, String> m_dirs;
        public final Map<String, Object> m_attrs;
        public final Map<String, String> m_types;

        public ParsedHeaderClause(
                List<String> paths, Map<String, String> dirs, Map<String, Object> attrs,
                Map<String, String> types)
        {
            m_paths = paths;
            m_dirs = dirs;
            m_attrs = attrs;
            m_types = types;
        }
    }
}
