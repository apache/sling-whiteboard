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
package org.apache.sling.jaxrs;

import javax.servlet.http.HttpServletRequest;
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
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

@Path("/test")
@Produces(MediaType.TEXT_PLAIN)
@Component(service=TestService.class)
@JaxrsResource
public class TestService {

	static int counter;
	 
	@Reference
	private ResourceResolverFactory factory;

	ResourceResolver getResourceResolver(HttpServletRequest request) {
		ResourceResolver rr = factory.getThreadResourceResolver();
		if ( rr != null ) {
			return rr;
		}
                throw new IllegalStateException("Request does not provide a ResourceResolver");
	}

	@GET
	@Path("/sling/resource")
	public String getResource(@Context HttpServletRequest request, @QueryParam("path") String path) throws Exception {
		try (ResourceResolver rr = getResourceResolver(request)) {
			final Resource r = rr.getResource(path);
			if(r == null) {
				throw new Exception(String.format("Resource %s not found", path));
			}
			return String.format("Got Resource %s of type %s%n", r.getPath(), r.getResourceType());
		}
	}

	@GET
	@Path("/userinfo")
	public String getUserInfo(@Context HttpServletRequest request) throws Exception {
		try (ResourceResolver rr = getResourceResolver(request)) {
			return String.format("userID='%s'%n", rr.getUserID());
		}
	}

	@GET
	@Path("/{one}")
	public String getOne(@PathParam("one") String one) {
		return String.format(
			"The single input was %s (%d characters) and the counter is %d%n", 
			one, one.length(), counter);
	}

	@GET
	@Path("/{one}/{two}")
	public String getTwo(@PathParam("one") String one, @PathParam("two") String two) {
		return String.format(
			"The dual input was %s and %s and the counter is %d%n",
			one, two, counter);
	}

	@POST
	@Path("/increment/{howMuch}")
	public static String incrementCounter(@PathParam("howMuch") int howMuch) {
		counter += howMuch;
		return String.format(
			"The counter has been incremented by %d and is now %d%n",
			howMuch, counter);
	}
}
