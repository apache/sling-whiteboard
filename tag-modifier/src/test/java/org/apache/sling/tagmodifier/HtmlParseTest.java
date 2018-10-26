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

package org.apache.sling.tagmodifier;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.text.ParseException;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.sling.tagmodifier.Element;
import org.apache.sling.tagmodifier.ElementType;
import org.apache.sling.tagmodifier.Tag;
import org.apache.sling.tagmodifier.TagMapping;
import org.apache.sling.tagmodifier.consumer.HtmlSAXSupport;
import org.apache.sling.tagmodifier.util.HtmlStreams;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

public class HtmlParseTest {

    Stream<Element> stream;

    @Before
    public void setUp() throws ParseException, Exception {
        InputStream is = this.getClass().getResourceAsStream("/demo.html");
        stream = Tag.stream(is, "UTF-8");
    }

    @Test
    public void docParseTagTest() throws Exception {
        long count = stream.filter(elem -> elem.getType() == ElementType.START_TAG).count();
        assertEquals(902, count);
    }

    @Test
    public void docParseAllTest() throws Exception {
        long count = stream.count();
        assertEquals(2928, count);
    }

    @Test
    public void docParseAllTestToString() throws Exception {
        stream.map(HtmlStreams.TO_HTML).count();
    }

    @Test
    public void docParseSAXTest() {
        HtmlSAXSupport support = new HtmlSAXSupport(new DefaultHandler2() {
            @Override
            public void startElement(String uri, String localName, String qName, Attributes attributes)
                    throws SAXException {
                //System.out.println(localName);
            }

        }, new DefaultHandler2());
        stream.forEach(support);
    }

    @Test
    public void docParseTagTest3() throws Exception {
        long count = stream.flatMap(TagMapping.map((element, process) -> {
            if (element.containsAttribute("href")) {
                //System.out.println(element.getAttributeValue("href"));
                process.next(element);
            }
        })).count();
        assertEquals(356, count);
    }

    private static Function<Element, Stream<Element>> CONVERT_LINKS = TagMapping.map((element, process) -> {
        if (element.containsAttribute("href")) {
            String value = element.getAttributeValue("href");
            if (value != null && value.startsWith("/")) {
                element.setAttribute("href", "http://www.apache.org" + value);
            }
        }
        if (element.containsAttribute("src")) {
            String value = element.getAttributeValue("src");
            if (value != null && value.startsWith("/")) {
                element.setAttribute("src", "http://www.apache.org" + value);
            }
        }
        process.next(element);
    });

    @Test
    public void convertLinkTest() throws Exception {
        long count = stream.flatMap(CONVERT_LINKS).count();
        assertEquals(2928, count);
    }

    @Test
    public void convertLinkAndPrintTest() throws Exception {
        //stream.flatMap(CONVERT_LINKS).map(HtmlStreams.TO_HTML).forEach(System.out::print);
    }

}
