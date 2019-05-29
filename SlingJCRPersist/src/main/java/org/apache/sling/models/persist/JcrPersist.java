package org.apache.sling.models.persist;

import javax.jcr.RepositoryException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.jetbrains.annotations.NotNull;

/**
 * Definition of JCR Persist service
 */
public interface JcrPersist {

    void persist(@NotNull Object object, @NotNull ResourceResolver resourceResolver) throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException;

    void persist(@NotNull Object object, @NotNull ResourceResolver resourceResolver, boolean deepPersist) throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException;

    void persist(String nodePath, @NotNull Object object, @NotNull ResourceResolver resourceResolver) throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException;

    void persist(String nodePath, @NotNull Object object, @NotNull ResourceResolver resourceResolver, boolean deepPersist) throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException;
    
}
