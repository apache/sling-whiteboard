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
package org.apache.sling.feature.support.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.Manifest;

import org.apache.sling.feature.Capability;
import org.apache.sling.feature.Requirement;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.BundleNamespace;
import org.osgi.framework.namespace.ExecutionEnvironmentNamespace;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleRevision;

public class ManifestParser
{
    private static final String BUNDLE_LICENSE_HEADER = "Bundle-License"; // No constant defined by OSGi...

    private final Manifest m_headerMap;
    private volatile String m_bundleSymbolicName;
    private volatile Version m_bundleVersion;
    private volatile List<Capability> m_capabilities;
    private volatile List<Requirement> m_requirements;

    public ManifestParser(Manifest m)
            throws BundleException
    {
        m_headerMap = m;

        // Verify that only manifest version 2 is specified.
        String manifestVersion = getManifestVersion(m_headerMap);
        if ((manifestVersion != null) && !manifestVersion.equals("2"))
        {
            throw new BundleException(
                    "Unknown 'Bundle-ManifestVersion' value: " + manifestVersion);
        }

        List<Capability> capList = new ArrayList<>();

        //
        // Parse bundle version.
        //

        m_bundleVersion = Version.emptyVersion;
        if (m_headerMap.getMainAttributes().getValue(Constants.BUNDLE_VERSION) != null)
        {
            try
            {
                m_bundleVersion = Version.parseVersion(m_headerMap.getMainAttributes().getValue(Constants.BUNDLE_VERSION));
            }
            catch (RuntimeException ex)
            {
                // R4 bundle versions must parse, R3 bundle version may not.
                if (getManifestVersion().equals("2"))
                {
                    throw ex;
                }
                m_bundleVersion = Version.emptyVersion;
            }
        }

        //
        // Parse bundle symbolic name.
        //

        Capability bundleCap = parseBundleSymbolicName(m_headerMap);
        if (bundleCap != null)
        {
            m_bundleSymbolicName = (String)
                    bundleCap.getAttributes().get(BundleRevision.BUNDLE_NAMESPACE);

            // Add a bundle capability and a host capability to all
            // non-fragment bundles. A host capability is the same
            // as a require capability, but with a different capability
            // namespace. Bundle capabilities resolve required-bundle
            // dependencies, while host capabilities resolve fragment-host
            // dependencies.
            if (m_headerMap.getMainAttributes().getValue(Constants.FRAGMENT_HOST) == null)
            {
                // All non-fragment bundles have host capabilities.
                capList.add(bundleCap);
                // A non-fragment bundle can choose to not have a host capability.
                String attachment =
                        (String) bundleCap.getDirectives().get(Constants.FRAGMENT_ATTACHMENT_DIRECTIVE);
                attachment = (attachment == null)
                        ? Constants.FRAGMENT_ATTACHMENT_RESOLVETIME
                        : attachment;
                if (!attachment.equalsIgnoreCase(Constants.FRAGMENT_ATTACHMENT_NEVER))
                {
                    Map<String, Object> hostAttrs =
                            new HashMap<>(bundleCap.getAttributes());
                    Object value = hostAttrs.remove(BundleRevision.BUNDLE_NAMESPACE);
                    hostAttrs.put(BundleRevision.HOST_NAMESPACE, value);
                    Capability cap = new Capability(BundleRevision.HOST_NAMESPACE);
                    cap.getAttributes().putAll(hostAttrs);
                    cap.getDirectives().putAll(bundleCap.getDirectives());
                    capList.add(cap);
                }
            }

            //
            // Add the osgi.identity capability.
            //
            capList.add(addIdentityCapability(m_headerMap, bundleCap));
        }

        // Verify that bundle symbolic name is specified.
        if (getManifestVersion().equals("2") && (m_bundleSymbolicName == null))
        {
            throw new BundleException(
                    "R4 bundle manifests must include bundle symbolic name.");
        }

        //
        // Parse Fragment-Host.
        //

        List<Requirement> hostReqs = parseFragmentHost(m_headerMap);

        //
        // Parse Require-Bundle
        //

        List<ParsedHeaderClause> rbClauses =
                parseStandardHeader(m_headerMap.getMainAttributes().getValue(Constants.REQUIRE_BUNDLE));
        rbClauses = normalizeRequireClauses(rbClauses, getManifestVersion());
        List<Requirement> rbReqs = convertRequires(rbClauses);


        //
        // Parse Require-Capability.
        //

        List<ParsedHeaderClause> requireClauses =
                parseStandardHeader(m_headerMap.getMainAttributes().getValue(Constants.REQUIRE_CAPABILITY));
        List<Requirement> requireReqs = convertRequireCapabilities(normalizeCapabilityClauses( requireClauses, getManifestVersion()));

        //
        // Parse Provide-Capability.
        //

        List<ParsedHeaderClause> provideClauses =
                parseStandardHeader(m_headerMap.getMainAttributes().getValue(Constants.PROVIDE_CAPABILITY));

        List<Capability> provideCaps = convertProvideCapabilities(normalizeCapabilityClauses(provideClauses, getManifestVersion()));

        // Combine all requirements.
        m_requirements = new ArrayList<>();
        m_requirements.addAll(hostReqs);
        m_requirements.addAll(rbReqs);
        m_requirements.addAll(requireReqs);

        // Combine all capabilities.
        m_capabilities = new ArrayList<>();
        m_capabilities.addAll(capList);
        m_capabilities.addAll(provideCaps);
    }


    public static List<Requirement> convertRequireCapabilities(
            List<ParsedHeaderClause> clauses)
            throws BundleException
    {
        // Now convert generic header clauses into requirements.
        List<Requirement> reqList = new ArrayList<>();
        for (ParsedHeaderClause clause : clauses)
        {
            for (String path : clause.m_paths)
            {
                if (path.startsWith("osgi.wiring."))
                {
                    throw new BundleException("Manifest cannot use Require-Capability for '"
                            + path
                            + "' namespace.");
                }

                Requirement req = new Requirement(path);
                req.getAttributes().putAll(clause.m_attrs);
                req.getDirectives().putAll(clause.m_dirs);
                // Create requirement and add to requirement list.
                reqList.add(req);
            }
        }

        return reqList;
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

                Capability capability = new Capability(path);
                capability.getAttributes().putAll(clause.m_attrs);
                capability.getDirectives().putAll(clause.m_dirs);
                // Create package capability and add to capability list.
                capList.add(capability);
            }
        }

        return capList;
    }


    public String getManifestVersion()
    {
        String manifestVersion = getManifestVersion(m_headerMap);
        return (manifestVersion == null) ? "1" : manifestVersion;
    }

    private static String getManifestVersion(Manifest headerMap)
    {
        String manifestVersion = headerMap.getMainAttributes().getValue(Constants.BUNDLE_MANIFESTVERSION);
        return (manifestVersion == null) ? null : manifestVersion.trim();
    }

    public String getSymbolicName()
    {
        return m_bundleSymbolicName;
    }

    public Version getBundleVersion()
    {
        return m_bundleVersion;
    }

    public List<Capability> getCapabilities()
    {
        return m_capabilities;
    }

    public List<Requirement> getRequirements()
    {
        return m_requirements;
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

    private static Capability parseBundleSymbolicName(
            Manifest headerMap)
            throws BundleException
    {
        List<ParsedHeaderClause> clauses = parseStandardHeader(headerMap.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME));
        if (clauses.size() > 0)
        {
            if (clauses.size() > 1)
            {
                throw new BundleException(
                        "Cannot have multiple symbolic names: "
                                + headerMap.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME));
            }
            else if (clauses.get(0).m_paths.size() > 1)
            {
                throw new BundleException(
                        "Cannot have multiple symbolic names: "
                                + headerMap.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME));
            }

            // Get bundle version.
            Version bundleVersion = Version.emptyVersion;
            if (headerMap.getMainAttributes().getValue(Constants.BUNDLE_VERSION) != null)
            {
                try
                {
                    bundleVersion = Version.parseVersion(
                            headerMap.getMainAttributes().getValue(Constants.BUNDLE_VERSION));
                }
                catch (RuntimeException ex)
                {
                    // R4 bundle versions must parse, R3 bundle version may not.
                    String mv = getManifestVersion(headerMap);
                    if (mv != null)
                    {
                        throw ex;
                    }
                    bundleVersion = Version.emptyVersion;
                }
            }

            // Create a require capability and return it.
            String symName = clauses.get(0).m_paths.get(0);
            clauses.get(0).m_attrs.put(BundleRevision.BUNDLE_NAMESPACE, symName);
            clauses.get(0).m_attrs.put(Constants.BUNDLE_VERSION_ATTRIBUTE, bundleVersion);
            Capability cap = new Capability(BundleRevision.BUNDLE_NAMESPACE);
            cap.getAttributes().putAll(clauses.get(0).m_attrs);
            cap.getAttributes().putAll(clauses.get(0).m_dirs);

            return cap;
        }

        return null;
    }

    private static Capability addIdentityCapability(Manifest headerMap, Capability bundleCap)
    {
        Map<String, Object> attrs = new HashMap<>();

        attrs.put(IdentityNamespace.IDENTITY_NAMESPACE,
                bundleCap.getAttributes().get(BundleNamespace.BUNDLE_NAMESPACE));
        attrs.put(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE,
                headerMap.getMainAttributes().getValue(Constants.FRAGMENT_HOST) == null
                        ? IdentityNamespace.TYPE_BUNDLE
                        : IdentityNamespace.TYPE_FRAGMENT);
        attrs.put(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE,
                bundleCap.getAttributes().get(Constants.BUNDLE_VERSION_ATTRIBUTE));

        if (headerMap.getMainAttributes().getValue(Constants.BUNDLE_COPYRIGHT) != null)
        {
            attrs.put(IdentityNamespace.CAPABILITY_COPYRIGHT_ATTRIBUTE,
                    headerMap.getMainAttributes().getValue(Constants.BUNDLE_COPYRIGHT));
        }

        if (headerMap.getMainAttributes().getValue(Constants.BUNDLE_DESCRIPTION) != null)
        {
            attrs.put(IdentityNamespace.CAPABILITY_DESCRIPTION_ATTRIBUTE,
                    headerMap.getMainAttributes().getValue(Constants.BUNDLE_DESCRIPTION));
        }
        if (headerMap.getMainAttributes().getValue(Constants.BUNDLE_DOCURL) != null)
        {
            attrs.put(IdentityNamespace.CAPABILITY_DOCUMENTATION_ATTRIBUTE,
                    headerMap.getMainAttributes().getValue(Constants.BUNDLE_DOCURL));
        }
        if (headerMap.getMainAttributes().getValue(BUNDLE_LICENSE_HEADER) != null)
        {
            attrs.put(IdentityNamespace.CAPABILITY_LICENSE_ATTRIBUTE,
                    headerMap.getMainAttributes().getValue(BUNDLE_LICENSE_HEADER));
        }

        Map<String, String> dirs;
        if (bundleCap.getDirectives().get(Constants.SINGLETON_DIRECTIVE) != null)
        {
            dirs = Collections.singletonMap(IdentityNamespace.CAPABILITY_SINGLETON_DIRECTIVE,
                    (String) bundleCap.getDirectives().get(Constants.SINGLETON_DIRECTIVE));
        }
        else
        {
            dirs = Collections.emptyMap();
        }
        Capability cap = new Capability(IdentityNamespace.IDENTITY_NAMESPACE);
        cap.getAttributes().putAll(attrs);
        cap.getDirectives().putAll(dirs);
        return cap;
    }

    private static List<Requirement> parseFragmentHost(
            Manifest headerMap)
            throws BundleException
    {
        List<Requirement> reqs = new ArrayList<>();

        String mv = getManifestVersion(headerMap);
        if ((mv != null) && mv.equals("2"))
        {
            List<ParsedHeaderClause> clauses = parseStandardHeader(
                    headerMap.getMainAttributes().getValue(Constants.FRAGMENT_HOST));
            if (clauses.size() > 0)
            {
                // Make sure that only one fragment host symbolic name is specified.
                if (clauses.size() > 1)
                {
                    throw new BundleException(
                            "Fragments cannot have multiple hosts: "
                                    + headerMap.getMainAttributes().getValue(Constants.FRAGMENT_HOST));
                }
                else if (clauses.get(0).m_paths.size() > 1)
                {
                    throw new BundleException(
                            "Fragments cannot have multiple hosts: "
                                    + headerMap.getMainAttributes().getValue(Constants.FRAGMENT_HOST));
                }

                // If the bundle-version attribute is specified, then convert
                // it to the proper type.
                Object value = clauses.get(0).m_attrs.get(Constants.BUNDLE_VERSION_ATTRIBUTE);
                value = (value == null) ? "0.0.0" : value;
                if (value != null)
                {
                    clauses.get(0).m_attrs.put(
                            Constants.BUNDLE_VERSION_ATTRIBUTE,
                            VersionRange.parse(value.toString()));
                }

                // Note that we use a linked hash map here to ensure the
                // host symbolic name is first, which will make indexing
                // more efficient.
// TODO: OSGi R4.3 - This is ordering is kind of hacky.
                // Prepend the host symbolic name to the map of attributes.
                Map<String, Object> attrs = clauses.get(0).m_attrs;
                Map<String, Object> newAttrs = new LinkedHashMap<>(attrs.size() + 1);
                // We want this first from an indexing perspective.
                newAttrs.put(
                        BundleRevision.HOST_NAMESPACE,
                        clauses.get(0).m_paths.get(0));
                newAttrs.putAll(attrs);
                // But we need to put it again to make sure it wasn't overwritten.
                newAttrs.put(
                        BundleRevision.HOST_NAMESPACE,
                        clauses.get(0).m_paths.get(0));

                // Create filter now so we can inject filter directive.
                SimpleFilter sf = SimpleFilter.convert(newAttrs);

                // Inject filter directive.
// TODO: OSGi R4.3 - Can we insert this on demand somehow?
                Map<String, String> dirs = clauses.get(0).m_dirs;
                Map<String, String> newDirs = new HashMap<>(dirs.size() + 1);
                newDirs.putAll(dirs);
                newDirs.put(
                        Constants.FILTER_DIRECTIVE,
                        sf.toString());

                Requirement req = new Requirement(BundleRevision.HOST_NAMESPACE);
                req.getAttributes().putAll(newAttrs);
                req.getDirectives().putAll(newDirs);
                reqs.add(req);
            }
        }
        else if (headerMap.getMainAttributes().getValue(Constants.FRAGMENT_HOST) != null)
        {
            String s = headerMap.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
            s = (s == null) ? headerMap.getMainAttributes().getValue(Constants.BUNDLE_NAME) : s;
            s = (s == null) ? headerMap.toString() : s;
        }

        return reqs;
    }


    private static List<Requirement> parseBreeHeader(String header)
    {
        List<String> filters = new ArrayList<>();
        for (String entry : parseDelimitedString(header, ","))
        {
            List<String> names = parseDelimitedString(entry, "/");
            List<String> left = parseDelimitedString(names.get(0), "-");

            String lName = left.get(0);
            Version lVer;
            try
            {
                lVer = Version.parseVersion(left.get(1));
            }
            catch (Exception ex)
            {
                // Version doesn't parse. Make it part of the name.
                lName = names.get(0);
                lVer = null;
            }

            String rName = null;
            Version rVer = null;
            if (names.size() > 1)
            {
                List<String> right = parseDelimitedString(names.get(1), "-");
                rName = right.get(0);
                try
                {
                    rVer = Version.parseVersion(right.get(1));
                }
                catch (Exception ex)
                {
                    rName = names.get(1);
                    rVer = null;
                }
            }

            String versionClause;
            if (lVer != null)
            {
                if ((rVer != null) && (!rVer.equals(lVer)))
                {
                    // Both versions are defined, but different. Make each of them part of the name
                    lName = names.get(0);
                    rName = names.get(1);
                    versionClause = null;
                }
                else
                {
                    versionClause = getBreeVersionClause(lVer);
                }
            }
            else
            {
                versionClause = getBreeVersionClause(rVer);
            }

            if ("J2SE".equals(lName))
            {
                // J2SE is not used in the Capability variant of BREE, use JavaSE here
                // This can only happen with the lName part...
                lName = "JavaSE";
            }

            String nameClause;
            if (rName != null)
                nameClause = "(" + ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE + "=" + lName + "/" + rName + ")";
            else
                nameClause = "(" + ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE + "=" + lName + ")";

            String filter;
            if (versionClause != null)
                filter = "(&" + nameClause + versionClause + ")";
            else
                filter = nameClause;

            filters.add(filter);
        }

        if (filters.size() == 0)
        {
            return Collections.emptyList();
        }
        else
        {
            String reqFilter;
            if (filters.size() == 1)
            {
                reqFilter = filters.get(0);
            }
            else
            {
                // If there are more BREE filters, we need to or them together
                StringBuilder sb = new StringBuilder("(|");
                for (String f : filters)
                {
                    sb.append(f);
                }
                sb.append(")");
                reqFilter = sb.toString();
            }

            SimpleFilter sf = SimpleFilter.parse(reqFilter);
            Requirement req = new Requirement(ExecutionEnvironmentNamespace.EXECUTION_ENVIRONMENT_NAMESPACE);
            req.getDirectives().put(ExecutionEnvironmentNamespace.REQUIREMENT_FILTER_DIRECTIVE, reqFilter);
            return Collections.<Requirement>singletonList(req);
        }
    }

    private static String getBreeVersionClause(Version ver) {
        if (ver == null)
            return null;

        return "(" + ExecutionEnvironmentNamespace.CAPABILITY_VERSION_ATTRIBUTE + "=" + ver + ")";
    }

    static List<ParsedHeaderClause> normalizeRequireClauses(List<ParsedHeaderClause> clauses, String mv)
    {
        // R3 bundles cannot require other bundles.
        if (!mv.equals("2"))
        {
            clauses.clear();
        }
        else
        {
            // Convert bundle version attribute to VersionRange type.
            for (ParsedHeaderClause clause : clauses)
            {
                Object value = clause.m_attrs.get(Constants.BUNDLE_VERSION_ATTRIBUTE);
                if (value != null)
                {
                    clause.m_attrs.put(
                            Constants.BUNDLE_VERSION_ATTRIBUTE,
                            VersionRange.parse(value.toString()));
                }
            }
        }

        return clauses;
    }

    private static List<Requirement> convertRequires(
            List<ParsedHeaderClause> clauses)
    {
        List<Requirement> reqList = new ArrayList<>();
        for (ParsedHeaderClause clause : clauses)
        {
            for (String path : clause.m_paths)
            {
                // Prepend the bundle symbolic name to the array of attributes.
                Map<String, Object> attrs = clause.m_attrs;
                // Note that we use a linked hash map here to ensure the
                // symbolic name attribute is first, which will make indexing
                // more efficient.
// TODO: OSGi R4.3 - This is ordering is kind of hacky.
                // Prepend the symbolic name to the array of attributes.
                Map<String, Object> newAttrs = new LinkedHashMap<>(attrs.size() + 1);
                // We want this first from an indexing perspective.
                newAttrs.put(
                        BundleRevision.BUNDLE_NAMESPACE,
                        path);
                newAttrs.putAll(attrs);
                // But we need to put it again to make sure it wasn't overwritten.
                newAttrs.put(
                        BundleRevision.BUNDLE_NAMESPACE,
                        path);

                // Create filter now so we can inject filter directive.
                SimpleFilter sf = SimpleFilter.convert(newAttrs);

                // Inject filter directive.
// TODO: OSGi R4.3 - Can we insert this on demand somehow?
                Map<String, String> dirs = clause.m_dirs;
                Map<String, String> newDirs = new HashMap<>(dirs.size() + 1);
                newDirs.putAll(dirs);
                newDirs.put(
                        Constants.FILTER_DIRECTIVE,
                        sf.toString());

                Requirement req = new Requirement(BundleRevision.BUNDLE_NAMESPACE);
                req.getAttributes().putAll(newAttrs);
                req.getDirectives().putAll(newDirs);
                reqList.add(req);
            }
        }

        return reqList;
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

    public static List<String> parseDelimitedString(String value, String delim)
    {
        return parseDelimitedString(value, delim, true);
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
}
