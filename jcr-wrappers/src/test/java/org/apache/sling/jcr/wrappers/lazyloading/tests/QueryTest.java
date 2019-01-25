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
package org.apache.sling.jcr.wrappers.lazyloading.tests;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/** Verify that content is loaded on demand for queries that touch
 *  our test content.
 */
public class QueryTest extends TestBase {
    
    @MethodSource
    static Stream<String> countAndStatementProvider() {
        return Stream.of(
            // Format is "number of results#query statement"
            // A negative number means expect an Exception

            // These statements contain /content/ which will trigger
            // lazy loading of our test content
            "1#/jcr:root/content/slingshot/lazy/docs/content-model.html",
            "1#/jcr:root/content/starter/lazy/gradient.png",
            "1#/jcr:root/content/starter/lazy/img/asf-logo.svg",
            
            // This one returns no results as it doesn't trigger content loading
            "0#/jcr:root//asf-logo.png"
        );
    }
    
    @ParameterizedTest
    @MethodSource("countAndStatementProvider")
    public void testQuery(String countAndStatement) throws RepositoryException {
        assertNoContent();

        final String [] params = countAndStatement.split("#");
        final String statement = params[1];
        final int expected = Integer.valueOf(params[0]);
        
        final Query q = session.getWorkspace().getQueryManager().createQuery(statement, Query.XPATH);
        
        if(expected < 0) {
            try {
                q.execute();
                fail("Expected an Exception for " + statement);
            } catch(RepositoryException asExpected) {
            }
        } else {
            final QueryResult qr = q.execute();
            final AtomicInteger count = new AtomicInteger();
            qr.getRows().forEachRemaining(row -> count.incrementAndGet());
            assertEquals(expected, count.get(), "Expecting " + expected + " rows for query " + statement);
        }
    }
}