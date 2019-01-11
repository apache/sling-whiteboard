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
package org.apache.sling.types.data.commons;

import java.util.List;
import java.util.Optional;

import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.types.TypeException;
import org.apache.sling.types.data.Property;
import org.apache.sling.types.data.validation.ValidationError;
import org.apache.sling.types.spi.Context;
import org.jetbrains.annotations.NotNull;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface PropertyService {

	@NotNull
	Optional<?> getValue(@NotNull Context<Resource> ctx, @NotNull Property prop) throws TypeException;

	@NotNull
	List<@NotNull ValidationError> setValue(@NotNull Context<Resource> ctx, @NotNull Property prop,
			RequestParameter... params) throws TypeException;
}
