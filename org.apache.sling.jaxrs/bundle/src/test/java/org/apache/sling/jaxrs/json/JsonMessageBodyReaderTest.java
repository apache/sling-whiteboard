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
package org.apache.sling.jaxrs.json;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class JsonMessageBodyReaderTest {

    private JsonMessageBodyReader<Map> reader = new JsonMessageBodyReader<>();

    @ParameterizedTest
    @CsvSource(value = {
            "application/json,true",
            "application/problem+json,true",
            "application/xml,false"
    })
    void testIsReadable(String mediaType, boolean expected) {
        assertEquals(expected,
                reader.isReadable(null, null, new Annotation[0], MediaType.valueOf(mediaType)));
    }

    @Test
    void testReadFrom() throws WebApplicationException, IOException {
        Map read = reader.readFrom(Map.class, getClass(), null, null, null,
                new ByteArrayInputStream("{\"Hello\":\"World\"}".getBytes()));
        assertEquals("World", read.get("Hello"));
    }

}
