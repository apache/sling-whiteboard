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
package org.apache.sling.modeling.attributes.commons.impl;

import org.apache.sling.modeling.ModelException;
import org.apache.sling.modeling.attributes.spi.AttributeContext;
import org.apache.sling.modeling.attributes.spi.AttributeHandler;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;

class UrlDefinition extends SimpleDefinition<UrlDefinition> {
    @NotNull
    private static final String TYPE = "sling:url";

    private boolean absolute;

    private boolean templated;

    public UrlDefinition(@NotNull String name) {
        super(name, TYPE, String.class);
    }

    public boolean isAbsolute() {
        return absolute;
    }

    public UrlDefinition withAbsolute() {
        return withAbsolute(true);
    }

    public UrlDefinition withAbsolute(boolean absolute) {
        this.absolute = absolute;
        return this;
    }

    public boolean isTemplated() {
        return templated;
    }

    public UrlDefinition withTemplated() {
        return withTemplated(true);
    }

    public UrlDefinition withTemplated(boolean templated) {
        this.templated = templated;
        return this;
    }

    @Component(
        service = AttributeHandler.class,
        property = {
            AttributeHandler.PROPERTY_TYPE + "=" + TYPE
        }
    )
    public static class UrlAttributeHandler implements AttributeHandler<UrlDefinition> {
        @Override
        @NotNull
        public Object process(@NotNull AttributeContext ctx, @NotNull UrlDefinition def, @NotNull Object value)
                throws ModelException {
            if (def.isAbsolute()) {
                return ctx.getRequest().getContextPath() + value;
            }
            return value;
        }
    }
}
