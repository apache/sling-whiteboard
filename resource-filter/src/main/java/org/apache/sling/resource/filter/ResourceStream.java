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
package org.apache.sling.resource.filter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.sling.api.resource.Resource;

/**
 * Utility to create a Stream<Resource> of Resource objects from a managed
 * traversal of a Resource tree
 *
 */
public class ResourceStream {
    
    private Resource resource;

    public ResourceStream(Resource resource) {
       this.resource = resource;
    }

    /**
     * Provides a stream of resources starting from the current resource and
     * traversing through its subtree, the path of descent is controlled by the 
     * branch selector
     * 
     * @return self closing {@code Stream<Resource>} of unknown size.
     */
    public Stream<Resource> stream(Predicate<Resource> branchSelector) {
        final Resource resource = this.resource;
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<Resource>() {

            private final LinkedList<Resource> resourcesToCheck = new LinkedList<>();

            {
                resourcesToCheck.addFirst(resource);
            }

            Resource current;

            @Override
            public boolean hasNext() {
                if (resourcesToCheck.isEmpty()) {
                    return false;
                }

                current = resourcesToCheck.removeFirst();
                int index = 0;
                for (Resource child : current.getChildren()) {
                    if (branchSelector.test(child)) {
                        resourcesToCheck.add(index++, child);
                    }
                }

                return true;
            }

            @Override
            public Resource next() {
                return current;
            }
        }, Spliterator.ORDERED | Spliterator.IMMUTABLE), false);
    }
    
    /**
     * Provides a stream of resources starting from the current resource and
     * traversing through its subtree
     * 
     * @return self closing {@code Stream<Resource>} of unknown size.
     */
    public Stream<Resource> stream(){
        return stream(resource -> true);
    }
}
