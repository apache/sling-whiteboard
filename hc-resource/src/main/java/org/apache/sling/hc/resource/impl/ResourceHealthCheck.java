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
package org.apache.sling.hc.resource.impl;

import org.apache.felix.hc.api.FormattingResultLog;
import org.apache.felix.hc.api.HealthCheck;
import org.apache.felix.hc.api.Result;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = ResourceHealthCheck.Config.class, factory = true)
public class ResourceHealthCheck implements HealthCheck {
    
    @ObjectClassDefinition(name = "Health Check: Resources Present", description = "Checks the configured path(s) against the given thresholds")
    public @interface Config {
        @AttributeDefinition(name = "Name", description = "Name of this health check")
        String hc_name() default "Resources Present";

        @AttributeDefinition(name = "Tags", description = "List of tags for this health check, used to select subsets of health checks for execution e.g. by a composite health check.")
        String[] hc_tags() default {};
        
        @AttributeDefinition(name = "Resources", description = "List of resources to check for existence")
        String[] resources() default {};

        @AttributeDefinition
        String webconsole_configurationFactory_nameHint() default "Resources present: {resources}";
    }
    
    @Reference
    private ResourceResolverFactory resolverFactory;
    private Config cfg;
    
    @Activate
    protected void activate(Config cfg) {
        this.cfg = cfg;
    }
    
    @Override
    public Result execute() {
        FormattingResultLog result = new FormattingResultLog();
        
        try ( ResourceResolver resolver = resolverFactory.getServiceResourceResolver(null) ) {
            for (String resource : cfg.resources() )
                checkResource(result, resolver, resource);
        } catch (LoginException e) {
            result.critical("Error obtaining a resource resolver", e);
        }
        
        return new Result(result);
    }

    private void checkResource(FormattingResultLog result, ResourceResolver resolver, String resourcePath) {
        
        result.debug("Checking for resource at {}", resourcePath);
        Resource resource = resolver.getResource(resourcePath);
        
        if ( resource == null )
            result.critical("Resource at {} does not exist", resourcePath);
        else
            result.info("Resource {} exists", resourcePath);
    }

}
