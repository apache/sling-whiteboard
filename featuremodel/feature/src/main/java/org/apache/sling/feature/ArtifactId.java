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
package org.apache.sling.feature;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.osgi.framework.Version;

/**
 * An artifact identifier.
 *
 * An artifact is described by it's Apache Maven coordinates consisting of group id,
 * artifact id, and version. In addition, the classifier and type can be specified.
 * If no type is specified, {@code jar} is assumed.
 */
public class ArtifactId implements Comparable<ArtifactId> {

    /** The required group id. */
    private final String groupId;

    /** The required artifact id. */
    private final String artifactId;

    /** The required version. */
    private final String version;

    /** The optional classifier. */
    private final String classifier;

    /** The required type. Defaults to jar. */
    private final String type;

    /**
     * Create a new artifact object
     * @param groupId   The group id (required)
     * @param artifactId   The artifact id (required)
     * @param version The version (required)
     * @param classifier The classifier (optional)
     * @param type The type/extension (optional, defaults to jar)
     * @throws IllegalArgumentException If group id, artifact id or version are {@code null} or if
     *         the version is not a valid version.
     */
    public ArtifactId(final String groupId,
            final String artifactId,
            final String version,
            final String classifier,
            final String type) {
        if ( groupId == null || artifactId == null || version == null ) {
            throw new IllegalArgumentException("Argument must not be null");
        }
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.getOSGiVersion();
        if ( "bundle".equals(type) || type == null || type.isEmpty() ) {
            this.type = "jar";
        } else {
            this.type = type;
        }
        if ( classifier != null && classifier.isEmpty() ) {
            this.classifier = null;
        } else {
            this.classifier = classifier;
        }
    }

    /**
     * Create a new artifact id from a string, the string must either be a
     * mvn url or a mvn id (= coordinates)
     * @param s The string to parse
     * @return The artifact id
     * @throws IllegalArgumentException if the string can't be parsed to a valid artifact id.
     */
    public static ArtifactId parse(final String s) {
        if ( s.contains(":") ) {
            return ArtifactId.fromMvnId(s);
        } else if ( s.contains("/") ) {
            return ArtifactId.fromMvnUrl(s);
        }
        throw new IllegalArgumentException("Unable to parse mvn coordinates/url: " + s);
    }

    /**
     * Create a new artifact id from a maven url,
     * 'mvn:' group-id '/' artifact-id [ '/' [version] [ '/' [type] [ '/' classifier ] ] ] ]
     * @param url The url
     * @return A new artifact id
     * @throws IllegalArgumentException If the url is not valid
     */
    public static ArtifactId fromMvnUrl(final String url) {
        if ( url == null || (url.indexOf(':') != -1 && !url.startsWith("mvn:")) ) {
            throw new IllegalArgumentException("Invalid mvn url: " + url);
        }
        // throw if repository url is included
        if ( url.indexOf('!') != -1 ) {
            throw new IllegalArgumentException("Repository url is not supported for Maven artifacts at the moment.");
        }
        final String coordinates = url.startsWith("mvn:") ? url.substring(4) : url;
        String gId = null;
        String aId = null;
        String version = null;
        String type = null;
        String classifier = null;
        int part = 0;
        String value = coordinates;
        while ( value != null ) {
            final int pos = value.indexOf('/');
            final String current;
            if ( pos == -1 ) {
                current = value;
                value = null;
            } else {
                if ( pos == 0 ) {
                    current = null;
                } else {
                    current = value.substring(0, pos);
                }
                value = value.substring(pos + 1);
            }
            if ( current != null ) {
                if ( part == 0 ) {
                    gId = current;
                } else if ( part == 1 ) {
                    aId = current;
                } else if ( part == 2 ) {
                    version = current;
                } else if ( part == 3 ) {
                    type = current;
                } else if ( part == 4 ) {
                    classifier = current;
                }
            }
            part++;
        }
        return new ArtifactId(gId, aId, version, classifier, type);
    }

    /**
     * Create a new artifact id from maven coordinates/id
     * groupId:artifactId[:packaging[:classifier]]:version
     * @param coordinates The coordinates as outlined above
     * @return A new artifact id
     * @throws IllegalArgumentException If the id is not valid
     */
    public static ArtifactId fromMvnId(final String coordinates) {
        final String[] parts = coordinates.split(":");
        if ( parts.length < 3 || parts.length > 5) {
            throw new IllegalArgumentException("Invalid mvn coordinates: " + coordinates);
        }
        final String gId = parts[0];
        final String aId = parts[1];
        final String version = parts[parts.length - 1];
        final String type = parts.length > 3 ? parts[2] : null;
        final String classifier = parts.length > 4 ? parts[3] : null;

        return new ArtifactId(gId, aId, version, classifier, type);
    }

    /**
     * Return a mvn url
     * @return A mvn url
     * @see #fromMvnUrl(String)
     */
    public String toMvnUrl() {
        final StringBuilder sb = new StringBuilder("mvn:");
        sb.append(this.groupId);
        sb.append('/');
        sb.append(this.artifactId);
        sb.append('/');
        sb.append(version);
        if ( this.classifier != null || !"jar".equals(this.type)) {
            sb.append('/');
            sb.append(this.type);
            if ( this.classifier != null ) {
                sb.append('/');
                sb.append(this.classifier);
            }
        }
        return sb.toString();
    }

    /**
     * Return a mvn id
     * @return The mvn id
     * #see {@link #fromMvnId(String)}
     */
    public String toMvnId() {
        final StringBuilder sb = new StringBuilder();
        sb.append(this.groupId);
        sb.append(':');
        sb.append(this.artifactId);
        sb.append(':');
        sb.append(version);
        if ( this.classifier != null || !"jar".equals(this.type)) {
            sb.append(':');
            sb.append(this.type);
            if ( this.classifier != null ) {
                sb.append(':');
                sb.append(this.classifier);
            }
        }
        return sb.toString();
    }

    /**
     * Return the group id.
     * @return The group id.
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Return the artifact id.
     * @return The artifact id.
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Return the optional classifier.
     * @return The classifier or {@code null}.
     */
    public String getClassifier() {
        return classifier;
    }

    /**
     * Return the type.
     * @return The type.
     */
    public String getType() {
        return type;
    }

    /**
     * Return the version.
     * @return The version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Test whether the artifact id is pointing to the same artifact but potentially a different version
     * @param id The artifact id
     * @return {@code true} if group id, artifact id, type and classifier equal
     */
    public boolean isSame(final ArtifactId id) {
        if ( this.groupId.equals(id.groupId)
             && this.artifactId.equals(id.artifactId)
             && this.type.equals(id.type) ) {
            if (this.classifier == null && id.classifier == null ) {
                return true;
            }
            if ( this.classifier != null ) {
                return this.classifier.equals(id.classifier);
            }
        }
        return false;
    }

    /**
     * Return the OSGi version
     * @return The OSGi version
     */
    public Version getOSGiVersion() {
        String parts[] = version.split("\\.");

        if ( parts.length < 4) {

            int pos = parts[parts.length - 1].indexOf('-');
            if ( pos != -1 ) {
                final String[] newParts = new String[4];
                newParts[0] = parts.length > 1 ? parts[0] : parts[0].substring(0, pos);
                newParts[1] = parts.length > 2 ? parts[1] : (parts.length > 1 ? parts[1].substring(0, pos) : "0");
                newParts[2] = parts.length > 3 ? parts[2] : (parts.length > 2 ? parts[2].substring(0, pos) : "0");
                newParts[3] = parts[parts.length - 1].substring(pos + 1);
                parts = newParts;
            }
            else {
                // special case for strange versions like NUMBER_NUMBER
                for (int i = 0; i < parts.length; i++) {
                    for (pos = parts[i].indexOf('_'); pos != -1 && pos < parts[i].length() - 1; pos = parts[i].indexOf('_')) {
                        List<String> newParts = new ArrayList<>(Arrays.asList(parts));
                        newParts.remove(i);
                        newParts.add(i, parts[i].substring(0, pos));
                        newParts.add(i + 1, parts[i].substring(pos + 1));
                        parts = newParts.toArray(new String[0]);
                    }
                }
            }
        }
        if ( parts.length >= 4 ) {
            final int pos = parts[2].indexOf('-');
            if ( pos != -1 ) {
                parts[3] = parts[2].substring(pos + 1) + "." + parts[3];
                parts[2] = parts[2].substring(0, pos);
            }
        }
        if ( parts.length > 4 ) {
            final StringBuilder sb = new StringBuilder(parts[3]);
            for(int i=4; i<parts.length;i++) {
                sb.append('.');
                sb.append(parts[i]);
            }
            parts[3] = sb.toString();
        }
        if ( parts.length > 3 && parts[3] != null ) {
            final StringBuilder sb = new StringBuilder();
            for ( int i = 0; i < parts[3].length(); i++ )
            {
                final char c = parts[3].charAt( i );
                if ( ( c >= '0' && c <= '9' ) || ( c >= 'a' && c <= 'z' ) || ( c >= 'A' && c <= 'Z' ) || c == '_'
                    || c == '-' ) {
                    sb.append( c );
                } else {
                    sb.append( '_' );
                }
            }
            parts[3] = sb.toString();
        }
        final int majorVersion = parseInt(parts[0], version);
        final int minorVersion;
        final int microVersion;
        if ( parts.length > 1 ) {
            minorVersion = parseInt(parts[1], version);
        } else {
            minorVersion = 0;
        }
        if ( parts.length > 2 ) {
            microVersion = parseInt(parts[2], version);
        } else {
            microVersion = 0;
        }
        final String qualifier = (parts.length > 3 ? parts[3] : "");
        return new Version(majorVersion, minorVersion, microVersion, qualifier);
    }

    /**
     * Create a Maven like relative repository path.
     * @return A relative repository path. The path does not start with a slash.
     */
    public String toMvnPath() {
        final StringBuilder sb = new StringBuilder();
        sb.append(groupId.replace('.', '/'));
        sb.append('/');
        sb.append(artifactId);
        sb.append('/');
        sb.append(version);
        sb.append('/');
        sb.append(artifactId);
        sb.append('-');
        sb.append(version);
        if ( classifier != null ) {
            sb.append('-');
            sb.append(classifier);
        }
        sb.append('.');
        sb.append(type);
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return toMvnUrl().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if(o == null) return false;
        if(!(o instanceof ArtifactId)) return false;
        return toMvnUrl().equals(((ArtifactId)o).toMvnUrl());
    }

    @Override
    public int compareTo(final ArtifactId o) {
        if(o == null) return 1;
        return toMvnUrl().compareTo(o.toMvnUrl());
    }

    @Override
    public String toString() {
        return toMvnId();
    }

    /**
     * Parse an integer.
     */
    private static int parseInt(final String value, final String version) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid version " + version);
        }
    }
}
