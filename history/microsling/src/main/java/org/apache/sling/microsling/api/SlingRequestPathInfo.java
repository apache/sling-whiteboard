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
package org.apache.sling.microsling.api;

/** Sling breaks the request URI into four parts: the path itself, 
 *  optional dot-separated selectors and extension that follow it, and 
 *  an optional path suffix.
 *  
 * <p id="decomp">
 * <b>Decomposition of a Request URL</b>
 * <li>{@link Content#getPath() content path} - The longest substring of the
 * request URI resolving to a {@link Content} object such that the content path
 * is either the complete request URI the next character in the request URI
 * after the content path is either a dot (<code>.</code>) or a slash (<code>/</code>).
 * <li>{@link #getSelectors() selectors} - If the first character in the
 * request URI after the content path is a dot, the string after the dot upto
 * but not including the last dot before the next slash character or the end of
 * the request URI. If the content path spans the complete request URI or if a
 * slash follows the content path in the request, the no seletors exist. If only
 * one dot follows the content path before the end of the request URI or the
 * next slash, no selectors exist. The selectors are available as
 * {@link #getSelectorString() a single string}, an
 * {@link #getSelectors() array of strings}, which is the selector string
 * splitted on dots, or
 * {@link #getSelector(int) selectively by zero-based index}.
 * <li>{@link #getExtension() extension} - The string after the last dot after
 * the content path in the request uri but before the end of the request uri or
 * the next slash after the content path in the request uri. If a slash follows
 * the content path in the request URI, the extension is empty.
 * <li>{@link #getSuffix() suffix path} - If the request URI contains more a
 * slash character after the content path and optional selectors and extension,
 * the path starting with the slash upto the end of the request URI is the
 * suffix path.
 * </ol>
 * TODO: copy examples from the Sling ComponentRequest, or better, generate them
 * from test cases.
 */

public interface SlingRequestPathInfo {
    /**
     * Returns the extension from the URL or an empty string if the request URL
     * does not contain an extension.
     * <p>
     * Decomposition of the request URL is defined in the <a
     * href="#decomp">Decomposition of a Request URL</a> above.
     *
     * @return The extension from the request URL.
     */
    String getExtension();

    /**
     * Returns the i-th selector of the selector string split on dots or
     * <code>null</code> if i&lt;0 or i&gt;getSelectors().length. Alyways
     * returns <code>null</code> if the request has no selectors.
     * <p>
     * Decomposition of the request URL is defined in the <a
     * href="#decomp">Decomposition of a Request URL</a> above.
     *
     * @param i The index of the selector to return.
     * @return The value of the selector if 0 &lt;= i &lt;
     *         <code>getSelectors().length</code> or <code>null</code>
     *         otherwise.
     * @see #getSelectorString()
     * @see #getSelectors()
     */
    String getSelector(int i);

    /**
     * Returns the selectors decoded from the request URL as an array of
     * strings. This array is derived from the
     * {@link #getSelectorString() selector string} by splitting the string on
     * dots. Returns an empty array if the request has no selectors.
     * <p>
     * Decomposition of the request URL is defined in the <a
     * href="#decomp">Decomposition of a Request URL</a> above.
     *
     * @see #getSelectorString()
     * @see #getSelector(int)
     */
    String[] getSelectors();

    /**
     * Returns the selectors decoded from the request URL as string. Returns an
     * empty string if the request has no selectors.
     * <p>
     * Decomposition of the request URL is defined in the <a
     * href="#decomp">Decomposition of a Request URL</a> above.
     *
     * @see #getSelectors()
     * @see #getSelector(int)
     */
    String getSelectorString();

    /**
     * Returns the suffix part of the URL or an empty string if the request URL
     * does not contain a suffix.
     * <p>
     * Decomposition of the request URL is defined in the <a
     * href="#decomp">Decomposition of a Request URL</a> above.
     *
     * @return The suffix part of the request URL.
     */
    String getSuffix();
    
    /** The path that was used to resolve (=load) the Resource that we are processing */
    String getResourcePath();
    
    /** 
     * Return the unused "content path" part of the URL, what comes after the path of the 
     * resolved Resource, but before selectors, extension and suffix
     */
    String getUnusedContentPath();
}
