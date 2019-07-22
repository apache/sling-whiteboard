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
     * OSGi service registration property indicating the content type this {@code ContentParser} supports. The simplest way to retrieve a
     * {@code ContentParser} for a certain content type is to apply a filter on the service reference:
     *
     * <pre>
     *    {@literal @}Reference(target = "(" + ContentParser.SERVICE_PROPERTY_CONTENT_TYPE + "=" + _value_ + ")")
     *     private ContentParser parser;
     * </pre>
     *
     * If multiple services are registered for the same content type, the above code snippet will provide you with the service
     * implementation with the highest ranking. However, if a certain implementation is needed, an additional filter can be added:
     *
     * <pre>
     *     {@literal @}Reference(target = "(&amp;(" + ContentParser.SERVICE_PROPERTY_CONTENT_TYPE + "=" + _value_ + ")(component.name=" + _class_name_ + "))")
     *      private ContentParser parser;
     * </pre>
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
