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
package org.apache.sling.tagmodifier.impl.components;

import java.io.IOException;
import java.io.InputStream;

import org.apache.sling.commons.html.HtmlParser;
import org.apache.sling.tagmodifier.Tag;
import org.apache.sling.tagmodifier.consumer.HtmlSAXSupport;
import org.apache.sling.tagmodifier.util.DOMBuilder;
import org.osgi.service.component.annotations.Component;
import org.w3c.dom.Document;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

@Component(
    property = {
        "dom=tagmodifier",
        "sax=tagmodifier",
        "version=html5"
    }
)
public class TagHtmlParser implements HtmlParser {



    /**
     * @see org.apache.sling.commons.html.HtmlParser#parse(java.io.InputStream, java.lang.String, org.xml.sax.ContentHandler)
     */
    @Override
    public void parse(final InputStream stream, final String encoding, final ContentHandler contentHandler) throws SAXException {
        Tag.stream(stream,encoding).forEach(new HtmlSAXSupport(contentHandler, null));
    }

    /**
     * @see org.apache.sling.commons.html.HtmlParser#parse(java.lang.String, java.io.InputStream, java.lang.String)
     */
    @Override
    public Document parse(String systemId, InputStream stream, String encoding) throws IOException {
        final DOMBuilder builder = new DOMBuilder();
        Tag.stream(stream, encoding).forEach(new HtmlSAXSupport(builder, builder));
        return builder.getDocument();
    }

}
