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

package com.github.htmlparser;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.text.ParseException;
import java.util.stream.Stream;

import org.apache.sling.tagmodifier.Element;
import org.apache.sling.tagmodifier.ElementType;
import org.apache.sling.tagmodifier.Tag;
import org.apache.sling.tagmodifier.TagMapping;
import org.junit.Before;
import org.junit.Test;

public class UnusualHtmlTest {

    private Stream<Element> stream;

    @Before
    public void setUp() throws ParseException, Exception {
        InputStream is = this.getClass().getResourceAsStream("/fail.html");
        stream = Tag.stream(is, "UTF-8");
    }
    
    @Test
    public void docParseTagTest() throws Exception {
        long count = stream.filter(elem -> elem.getType() != ElementType.TEXT ).count();
        assertEquals(4, count);
    }

    @Test
    public void docParseTagTest2() throws Exception {
        long count = stream.filter(elem -> elem.getType() == ElementType.TEXT ).count();
        assertEquals(7, count);
    }
    
    @Test
    public void docParseTagTest3() throws Exception {
        long count = stream.flatMap(TagMapping.map((element,process) ->{
            if (element.containsAttribute("href")) {
                process.next(element, element);
            }
        })).count();
        assertEquals(0, count);
    }
    
}
