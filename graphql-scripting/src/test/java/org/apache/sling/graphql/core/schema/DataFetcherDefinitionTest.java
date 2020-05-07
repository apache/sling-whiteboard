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
package org.apache.sling.graphql.core.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class DataFetcherDefinitionTest {

    private final String input;
    private final String expected;
    private final Class<?> failureClass;

    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        final List<Object []> result = new ArrayList<>();
        
        result.add(new Object[] { "fetch:test/withOptions/sha512,armored(UTF-8) $.path", "test#withOptions#sha512,armored(UTF-8)#$.path" });
        result.add(new Object[] { "fetch:namespace2/FetcherOption/upperCase", "namespace2#FetcherOption#upperCase#" });
        result.add(new Object[] { "fetch:namespace2/FetcherExpression \t  sqrt(42)/3.4", "namespace2#FetcherExpression##sqrt(42)/3.4" });
        result.add(new Object[] { "fetch:namespace2/noOptions", "namespace2#noOptions##" });
        result.add(new Object[] { "wrongPrefix:namespace2/noOptions", IllegalArgumentException.class });
        result.add(new Object[] { "nimportequoi", IllegalArgumentException.class });
        result.add(new Object[] { "", IllegalArgumentException.class });
        result.add(new Object[] { null, IllegalArgumentException.class });
        
        return result;
    }

    public DataFetcherDefinitionTest(String input, Object expected) {
        this.input = input;
        if(expected instanceof String) {
            this.expected = (String)expected;
            this.failureClass = null;
        } else {
            this.expected = null;
            this.failureClass = (Class<?>)expected;
        }
    }

    @Test
    public void testMatch() throws Exception {
        if(failureClass == null) {
            final DataFetcherDefinition d = new DataFetcherDefinition(input);
            assertEquals("DataFetcherDefinition#" + expected, d.toString());
        } else {
            try {
                new DataFetcherDefinition(input);
                fail("Expecting a " + failureClass.getName());
            } catch(Throwable t) {
                assertEquals("Expecting a " + failureClass.getName(), failureClass, t.getClass());
            }
        } 
    }
}
