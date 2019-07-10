/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.contentparser.api;

import java.io.IOException;
import java.io.InputStream;

import org.osgi.annotation.versioning.ProviderType;

/**
 * A {@code ContentParser} parses Sling resource trees from a file. Implementations have to be thread-safe. A consumer requiring a {@code
 * ContentParser} reference should filter based on the {@link #SERVICE_PROPERTY_CONTENT_TYPE} in order to get a content type specific
 * parser.
 */
@ProviderType
public interface ContentParser {

    /**
     * JSON content descriptor file.
     *
     * @see <a href="https://sling.apache.org/documentation/bundles/content-loading-jcr-contentloader.html#json-descriptor-files">JCR
     * ContentLoader JSON descriptor files</a>
     */
    String JSON_CONTENT_TYPE = "json";

    /**
     * XML content descriptor file.
     *
     * @see <a href="https://sling.apache.org/documentation/bundles/content-loading-jcr-contentloader.html#xml-descriptor-files">JCR
     * ContentLoader XML descriptor files</a>
     */
    String XML_CONTENT_TYPE = "xml";

    /**
     * JCR XML content (FileVault XML),aAlso known as extended document view XML. Extends the regular document view as specified by JCR 2.0
     * with specifics like multi-value and type information.
     *
     * @see <a href="https://docs.adobe.com/content/docs/en/spec/jcr/2.0/7_Export.html#7.3%20Document%20View">JCR 2.0, 7.3 Document View</a>
     * @see <a href="http://jackrabbit.apache.org/filevault/">Jackrabbit FileVault</a>
     */
    String JCR_XML_CONTENT_TYPE = "jcr.xml";

    /**
     * OSGi service registration property indicating the content type this {@code ContentParser} supports.
     *
     * @see #JSON_CONTENT_TYPE
     * @see #XML_CONTENT_TYPE
     * @see #JCR_XML_CONTENT_TYPE
     */
    String SERVICE_PROPERTY_CONTENT_TYPE = "org.apache.sling.contentparser.content_type";

    /**
     * Parse content in a "stream-based" way. Each resource that is found in the content is reported to the {@link ContentHandler}.
     *
     * @param contentHandler content handler that accepts the parsed content
     * @param inputStream    stream with serialized content
     * @param parserOptions  parser options, providing different settings for handling the serialized content
     * @throws IOException    when an I/O error occurs
     * @throws ParseException when a parsing error occurs.
     */
    void parse(ContentHandler contentHandler, InputStream inputStream, ParserOptions parserOptions) throws IOException, ParseException;

}
