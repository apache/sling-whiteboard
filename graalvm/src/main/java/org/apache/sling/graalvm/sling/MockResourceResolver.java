package org.apache.sling.graalvm.sling;

import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.osgi.service.component.annotations.Component;

@Component(service=ResourceResolver.class)
public class MockResourceResolver implements ResourceResolver {

    private final ResourceProvider<?> provider;

    public MockResourceResolver(MockResourceProvider provider) {
        this.provider = provider;
    }

    @Override
    public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
        return null;
    }

    @Override
    public Resource resolve(HttpServletRequest request, String absPath) {
        return provider.getResource(null, absPath, null, null);
    }

    @Override
    public Resource resolve(String absPath) {
        return null;
    }

    @Override
    public Resource resolve(HttpServletRequest request) {
        return null;
    }

    @Override
    public String map(String resourcePath) {
        return null;
    }

    @Override
    public String map(HttpServletRequest request, String resourcePath) {
        return null;
    }

    @Override
    public Resource getResource(String path) {
        return null;
    }

    @Override
    public Resource getResource(Resource base, String path) {
        return null;
    }

    @Override
    public String[] getSearchPath() {
        return null;
    }

    @Override
    public Iterator<Resource> listChildren(Resource parent) {
        return null;
    }

    @Override
    public Resource getParent(Resource child) {
        return null;
    }

    @Override
    public Iterable<Resource> getChildren(Resource parent) {
        return null;
    }

    @Override
    public Iterator<Resource> findResources(String query, String language) {
        return null;
    }

    @Override
    public Iterator<Map<String, Object>> queryResources(String query, String language) {
        return null;
    }

    @Override
    public boolean hasChildren(Resource resource) {
        return false;
    }

    @Override
    public ResourceResolver clone(Map<String, Object> authenticationInfo) throws LoginException {
        return null;
    }

    @Override
    public boolean isLive() {
        return false;
    }

    @Override
    public void close() {

    }

    @Override
    public String getUserID() {
        return null;
    }

    @Override
    public Iterator<String> getAttributeNames() {
        return null;
    }

    @Override
    public Object getAttribute(String name) {
        return null;
    }

    @Override
    public void delete(Resource resource) throws PersistenceException {

    }

    @Override
    public Resource create(Resource parent, String name, Map<String, Object> properties) throws PersistenceException {
        return null;
    }

    @Override
    public void revert() {

    }

    @Override
    public void commit() throws PersistenceException {

    }

    @Override
    public boolean hasChanges() {
        return false;
    }

    @Override
    public String getParentResourceType(Resource resource) {
        return null;
    }

    @Override
    public String getParentResourceType(String resourceType) {
        return null;
    }

    @Override
    public boolean isResourceType(Resource resource, String resourceType) {
        return false;
    }

    @Override
    public void refresh() {

    }

    @Override
    public Resource copy(String srcAbsPath, String destAbsPath) throws PersistenceException {
        return null;
    }

    @Override
    public Resource move(String srcAbsPath, String destAbsPath) throws PersistenceException {
        return null;
    }

}