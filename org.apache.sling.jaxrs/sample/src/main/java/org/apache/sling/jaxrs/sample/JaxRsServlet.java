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
package org.apache.sling.jaxrs.sample;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jaxrs.json.problem.ProblemBuilder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsApplicationSelect;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Component(service = JaxRsServlet.class)
@JaxrsResource
@JaxrsApplicationSelect(value = "(jax.rs.whiteboard.name=test)")
public class JaxRsServlet {

    static int counter;

    @Reference
    private ResourceResolverFactory factory;

    ResourceResolver getResourceResolver(HttpServletRequest request) {
        ResourceResolver rr = factory.getThreadResourceResolver();
        if (rr != null) {
            return rr;
        }
        throw new IllegalStateException("Request does not provide a ResourceResolver");
    }

    @GET
    @Path("/sling/resource")
    public Map<String, Object> getResource(@Context HttpServletRequest request, @QueryParam("path") String path) {
        try (ResourceResolver rr = getResourceResolver(request)) {
            final Resource r = rr.getResource(path);
            if (r == null) {
                throw ProblemBuilder.get().withStatus(404).withDetail(String.format("Resource %s not found", path))
                        .buildThrowable();
            }
            return Map.of("path", r.getPath(), "type", r.getResourceType());
        }
    }

    @GET
    @Path("/userinfo")
    public Map<String, Object> getUserInfo(@Context HttpServletRequest request) throws Exception {
        try (ResourceResolver rr = getResourceResolver(request)) {
            return Map.of("userId", rr.getUserID());
        }
    }

    @GET
    @Path("/{one}")
    public Map<String, Object> getOne(@PathParam("one") String one) {
        return Map.of("message", String.format(
                "The single input was %s (%d characters)",
                one, one.length(), counter));
    }

    @GET
    @Path("/{one}/{two}")
    public Map<String, Object> getTwo(@PathParam("one") String one, @PathParam("two") String two) {
        return Map.of("message", String.format(
                "The dual input was %s and %s",
                one, two, counter));
    }

    @POST
    @Path("/name")
    @Consumes(MediaType.APPLICATION_JSON)
    public static Map<String, Object> echoName(Map<String, Object> params) {
        return Map.of("message", "Hello " + params.get("name") + "!");
    }

    @POST
    @Path("/increment/{howMuch}")
    @Consumes(MediaType.WILDCARD)
    public static Map<String, Object> incrementCounter(@PathParam("howMuch") int howMuch) {
        counter += howMuch;
        return Map.of("message", String.format(
                "The counter has been incremented by %d and is now %d%n",
                howMuch, counter));
    }
}
