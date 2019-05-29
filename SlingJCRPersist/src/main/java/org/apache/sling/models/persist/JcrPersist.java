package org.apache.sling.models.persist;

import javax.jcr.RepositoryException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;

/**
 * Definition of JCR Persist service
 */
public interface JcrPersist {

    void persist(Object object, ResourceResolver resourceResolver) throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException;

    void persist(Object object, ResourceResolver resourceResolver, boolean deepPersist) throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException;

    void persist(String nodePath, Object object, ResourceResolver resourceResolver) throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException;

    void persist(String nodePath, Object object, ResourceResolver resourceResolver, boolean deepPersist) throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException;
    
}
