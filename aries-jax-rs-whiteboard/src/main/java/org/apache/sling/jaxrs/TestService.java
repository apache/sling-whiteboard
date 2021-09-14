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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.jaxrs.whiteboard.propertytypes.JaxrsResource;

@Path("/jaxrs")
@Produces(MediaType.TEXT_PLAIN)
@Component(service=TestService.class)
@JaxrsResource
public class TestService {
	 
	@GET
	@Path("/{one}")
	public String getOne(@PathParam("one") String one) {
		return String.format("The single input was %s (%d characters)", one, one.length());
	}

	@GET
	@Path("/{one}/{two}")
	public String getTwo(@PathParam("one") String one, @PathParam("two") String two) {
		return String.format("The dual input was %s and %s", one, two);
	}

}