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

/**
 * An artifact consists of
 * <ul>
 *   <li>An id
 *   <li>metadata
 * </ul>
 */
public class Artifact implements Comparable<Artifact> {

    /** This key might be used by bundles to define the start order. */
    public static final String KEY_START_ORDER = "start-order";

    /** The artifact id. */
    private final ArtifactId id;

    /** Artifact metadata. */
    private final KeyValueMap metadata = new KeyValueMap();

    /**
     * Construct a new artifact
     * @param id The id of the artifact.
     * @throws IllegalArgumentException If id is {@code null}.
     */
    public Artifact(final ArtifactId id) {
        if ( id == null ) {
            throw new IllegalArgumentException("id must not be null.");
        }
        this.id = id;
    }

    /**
     * Get the id of the artifact.
     * @return The id.
     */
    public ArtifactId getId() {
        return this.id;
    }

    /**
     * Get the metadata of the artifact.
     * The metadata can be modified.
     * @return The metadata.
     */
    public KeyValueMap getMetadata() {
        return this.metadata;
    }

    /**
     * Get the start order of the artifact.
     * This is a convenience method which gets the value for the property named
     * {@code #KEY_START_ORDER} from the metadata.
     * @return The start order, if no start order is defined, {@code 0} is returned.
     * @throws NumberFormatException If the stored metadata is not a number
     * @throws IllegalStateException If the stored metadata is a negative number
     */
    public int getStartOrder() {
        final String order = this.getMetadata().get(Artifact.KEY_START_ORDER);
        final int startOrder;
        if ( order != null ) {
            startOrder = Integer.parseInt(order);
            if ( startOrder < 0 ) {
                throw new IllegalStateException("Start order must be >= 0 but is " + order);
            }
        } else {
            startOrder = 0;
        }

        return startOrder;
    }

    /**
     * Set the start order of the artifact
     * This is a convenience method which sets the value of the property named
     * {@code #KEY_START_ORDER} from the metadata.
     * @param startOrder The start order
     * @throws IllegalArgumentException If the number is negative
     */
    public void setStartOrder(final int startOrder) {
        if ( startOrder < 0 ) {
            throw new IllegalArgumentException("Start order must be >= 0 but is " + startOrder);
        }
        if ( startOrder == 0 ) {
            this.getMetadata().remove(KEY_START_ORDER);
        } else {
            this.getMetadata().put(KEY_START_ORDER, String.valueOf(startOrder));
        }
    }

    @Override
    public int compareTo(final Artifact o) {
        return this.id.compareTo(o.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return this.id.equals(((Artifact)obj).id);
    }

    @Override
    public String toString() {
        return "Artifact [id=" + id.toMvnId()
                + "]";
    }
}
