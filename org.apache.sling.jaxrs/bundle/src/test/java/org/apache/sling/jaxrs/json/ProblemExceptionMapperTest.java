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

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.ws.rs.core.Response;

import org.apache.sling.jaxrs.json.problem.ProblemBuilder;
import org.apache.sling.jaxrs.json.problem.ThrowableProblem;
import org.junit.jupiter.api.Test;

class ProblemExceptionMapperTest {

    @Test
    void mapsProblem() {
        ProblemExceptionMapper mapper = new ProblemExceptionMapper();
        ThrowableProblem throwable = ProblemBuilder.get().withStatus(400).buildThrowable();
        Response response = mapper.toResponse(throwable);
        assertEquals(400, response.getStatus());
        assertEquals("application/problem+json", response.getHeaderString("Content-Type"));
        assertEquals(throwable.getProblem(), response.getEntity());
    }

}
