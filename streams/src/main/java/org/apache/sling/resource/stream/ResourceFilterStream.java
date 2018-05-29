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
package org.apache.sling.resource.stream;

import javax.annotation.Nonnull;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.resource.stream.impl.ParseException;

public class ResourceFilterStream extends ResourceStream {

    protected ResourceFilterStream(Resource resource) {
        super(resource);
    }

    /**
     * Base resource for traversal
     * 
     * @param resource
     *            traversal starting point
     * @return new instance of ResourceQuery;
     */
    public static ResourceFilterStream from(@Nonnull Resource resource) {
        return new ResourceFilterStream(resource);
    }
    
    
    /**
     * Predicate used to select child resources for traversal
     * 
     * @param branchSelector
     *            resourceFilter script for traversal control
     * @return ResourceStream
     * @throws ParseException
     */
    public ResourceStream setBranchSelector(String branchSelector) throws ParseException {
        return setBranchSelector(new ResourceFilter(branchSelector));
    }
    
    /**
     * ResourceFilter script to identify Resource objects to add to the
     * Stream<Resource>
     * 
     * @param resourceSelector
     *            ResourceFilter script
     * @return ResourceStream
     * @throws ParseException
     */
    public ResourceStream setResourceSelector(String resourceSelector) throws ParseException {
        return setResourceSelector(new ResourceFilter(resourceSelector));
    }
    
}