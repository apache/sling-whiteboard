/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.apache.sling.models.persist.impl;

import javax.jcr.RepositoryException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.persist.JcrPersist;
import org.osgi.service.component.annotations.Component;

/**
 * Main class that does the magic of writing POJO directly using transparent persistence.
 */
@Component(service = JcrPersist.class)
public class JcrPersistImpl implements JcrPersist {
    @Override
    public void persist(Object object, ResourceResolver resourceResolver) throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException {
        JcrWriter.persist(object, resourceResolver, true);
    }
    
    @Override
    public void persist(Object object, ResourceResolver resourceResolver, boolean deepPersist) throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException {
        JcrWriter.persist(object, resourceResolver, deepPersist);
    }
    
    @Override
    public void persist(String nodePath, Object object, ResourceResolver resourceResolver) throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException {
        JcrWriter.persist(nodePath, object, resourceResolver, true);
    }

    @Override
    public void persist(String nodePath, Object object, ResourceResolver resourceResolver, boolean deepPersist) throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException {
        JcrWriter.persist(nodePath, object, resourceResolver, deepPersist);
    }
}
