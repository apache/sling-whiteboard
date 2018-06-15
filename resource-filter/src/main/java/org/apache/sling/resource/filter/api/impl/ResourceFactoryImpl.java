/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.resource.filter.api.impl;

import java.util.List;

import org.apache.sling.resource.filter.ResourceFilter;
import org.apache.sling.resource.filter.api.Context;
import org.apache.sling.resource.filter.api.ResourceFilterFactory;
import org.apache.sling.resource.filter.api.ResourceFilterFunction;
import org.apache.sling.resource.filter.impl.ParseException;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(property = { "service.description=ResourceFilter Factory", "service.vendor=The Apache Software Foundation" })
public class ResourceFactoryImpl implements ResourceFilterFactory {

    @Reference(policyOption=ReferencePolicyOption.GREEDY)
    List<ResourceFilterFunction> functions;

    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public ResourceFilter getResourceFilter(String script) {
        try {
            ResourceFilter filter = new ResourceFilter(script);
            Context context = filter.getContext();
            for (ResourceFilterFunction func : functions) {
                context.addArgument(func.getName(), func);
            }
        } catch (ParseException e) {
            log.error(e.getLocalizedMessage());
        }
        return null;
    }

}
