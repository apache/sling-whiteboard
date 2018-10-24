/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.tagmodifier;

import java.util.Map;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface Element {
    /**
     * Retrieve the associated ElementType
     * 
     * @return the type of element
     */
    ElementType getType();

    /**
     * Must be defined as true to allow default methods to work correctly, this
     * method and the getAttributes method are used together to implement attribute
     * support in an element.
     * 
     * @return true if Element supports attributes
     */
    boolean supportsAttributes();

    Map<String, AttrValue> getAttributes();

    /**
     * The String value that best represents what this element is.
     * 
     * @return
     */
    String getValue();

    /**
     * Accepts a Visitor to visit
     * 
     * @param visitor
     * @return
     */
    default <T> T accept(Visitor<T> visitor) {
        return visitor.visit(this);
    }

    /**
     * Whether this element contains Attributes
     * 
     * @return
     */
    default boolean hasAttributes() {
        if (supportsAttributes()) {
            return !getAttributes().isEmpty();
        }
        return false;
    }

    default boolean containsAttribute(String attrName) {
        return getAttributes().containsKey(attrName);
    }

    default String getAttributeValue(String name) {
        if (supportsAttributes()) {
            return getAttributes().get(name).toString();
        }
        return null;
    }

    default void setAttribute(String name, String value) {
        if (supportsAttributes()) {
            getAttributes().put(name, new AttrValue(value));
            return;
        }
        throw new UnsupportedOperationException();
    }
}
