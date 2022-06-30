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
package org.apache.sling.servlets.json;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.servlets.json.dynamicrequest.DynamicRequestMapper;
import org.apache.sling.servlets.json.problem.ProblemBuilder;
import org.apache.sling.servlets.json.problem.Problematic;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicRequestServlet extends JacksonJsonServlet {

    private static final Logger log = LoggerFactory.getLogger(DynamicRequestServlet.class);

    private final DynamicRequestMapper mapper;

    public DynamicRequestServlet() {
        mapper = new DynamicRequestMapper(this);
    }

    /**
     * Tries to handle the request by calling a Java method implemented for the
     * respective HTTP request method.
     * <p>
     * This implementation first attempts to resolve handling methods by
     * identifying methods with the @RequestHandler annotation which
     * apply to the provided request. Any such request handler
     * <p>
     * In addition, this method catches ServletException, IOException and
     * RuntimeExceptions thrown from the called methods and sends a JSON
     * Problem response based on the thrown exception
     *
     * @param request  The HTTP request
     * @param response The HTTP response
     * @return <code>true</code> if the requested method
     *         (<code>request.getMethod()</code>)
     *         is known. Otherwise <code>false</code> is returned.
     * @throws ServletException Forwarded from any of the dispatched methods
     * @throws IOException      Forwarded from any of the dispatched methods
     */
    @Override
    protected void service(@NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response) throws ServletException,
            IOException {
        boolean serviced = false;
        try {
            serviced = mapper.mayService(request, response);
            if (!serviced) {
                super.service(request, response);
            }
        } catch (IOException | ServletException | RuntimeException e) {
            if (e instanceof Problematic) {
                sendProblemResponse(response, ((Problematic) e).getProblem());
            } else {
                log.error("Handing uncaught exception", e);
                sendProblemResponse(response, ProblemBuilder.get().fromException(e).build());
            }
        }
    }

}
