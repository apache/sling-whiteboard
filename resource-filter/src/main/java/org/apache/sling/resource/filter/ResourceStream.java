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
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Stack;
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
     * Provides a depth first {@code Stream<Resource>} traversal of the resource
     * tree starting with the current resource. The traversal is controlled by the
     * provided predicate which determines if a given child is traversed. If no
     * children matches the predicate, the traversal for that branch ends
     * 
     * @param branchSelector
     *            used to determine whether a given child resource is traversed
     * @return {@code Stream<Resource>} of unknown size.
     */
    public Stream<Resource> stream(Predicate<Resource> branchSelector) {
        final Resource resource = this.resource;
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<Resource>() {

            private final Stack<Iterator<Resource>> resources = new Stack<Iterator<Resource>>();
            private Resource current;
            private Iterator<Resource> iterator;

            {
                resources.push(resource.getChildren().iterator());
                current = resource;
            }

            @Override
            public boolean hasNext() {
                if (current == null) {
                    return seek();
                }
                return true;
            }

            @Override
            public Resource next() {
                Resource next = current;
                current = null;
                return next;
            }

            private boolean seek() {
                while (true) {
                    if (resources.isEmpty()) {
                        return false;
                    }
                    iterator = resources.peek();
                    if (!iterator.hasNext()) {
                        resources.pop();
                    } else {
                        current = iterator.next();
                        if (branchSelector.test(current)) {
                            resources.push(current.getChildren().iterator());
                            break;
                        }
                    }
                }
                return true;
            }

        }, Spliterator.ORDERED | Spliterator.IMMUTABLE), false);
    }

    public Stream<Resource> listChildren(Predicate<Resource> childSelector) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(resource.listChildren(),
                Spliterator.ORDERED | Spliterator.IMMUTABLE), false).filter(childSelector);
    }

}
