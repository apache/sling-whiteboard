package org.apache.sling.mvresource.impl;

import java.util.Iterator;
import java.util.Map;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.spi.resource.provider.ProviderContext;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(
service=ResourceProvider.class,
configurationPolicy=ConfigurationPolicy.REQUIRE,
property={
        Constants.SERVICE_DESCRIPTION + "=Sling File System Resource Provider",
        Constants.SERVICE_VENDOR + "=The Apache Software Foundation"
})
public class MvStoreResourceProvider extends ResourceProvider<Object> {

    @ObjectClassDefinition(name = "Apache Sling Resource Provider",
            description = "Configure an instance of the file system " +
                          "resource provider in terms of provider root and file system location")
    public @interface Config {

        @AttributeDefinition(name = "DataStore File Name",
                description = "File system directory mapped to the virtual " +
                        "resource tree. This property must not be an empty string. If the path is " +
                        "relative it is resolved against sling.home or the current working directory. " +
                        "The path may be a file or folder. If the path does not address an existing " +
                        "file or folder, an empty folder is created.")
        String provider_file();

        @AttributeDefinition(name = "Provider Root",
                description = "Location in the virtual resource tree where the " +
                "file system resources are mapped in. This property must not be an empty string.")
        String provider_root();

        @AttributeDefinition(name = "Cache Size",
                description = "Max. number of content files cached in memory.")
        int provider_cache_size() default 10000;

        // Internal Name hint for web console.
        String webconsole_configurationFactory_nameHint() default "{provider.fs.mode}: {" + ResourceProvider.PROPERTY_ROOT + "}";
    }
    
    MVStore store;
    
    @Override
    public void start(ProviderContext ctx) {
        super.start(ctx);
        store =  MVStore.open("dataStore");
    }

    @Override
    public void stop() {
        super.stop();
        store.close();
    }

    @Override
    public Resource getResource(ResolveContext<Object> context, String resourcePath, ResourceContext resourceContext, Resource parentResource) {
        MVMap<String,Object> properties = store.openMap(resourcePath);
        return new MvResource(context.getResourceResolver(),resourcePath,properties);
    }

    @Override
    public Resource create(ResolveContext<Object> ctx, String path, Map<String, Object> properties)
            throws PersistenceException {
        String parent = parentPath(path);
        MVMap<String,Boolean> children = store.openMap(parent+":children");
        MVMap<String,Object> data = store.openMap(path);
        children.put(path, true);
        data.putAll(properties);
        store.commit();
        return super.create(ctx, path, properties);
    }

    private String parentPath(String path) {
        int index = path.lastIndexOf('/');
        return path.substring(0, index);
    }

    @Override
    public void delete(ResolveContext<Object> ctx, Resource resource) throws PersistenceException {
        if (!(resource instanceof MVMap)) {
            throw new PersistenceException();
        }
        MVMap<String,Object> map = ((MvResource)resource).getMVMap();
        store.removeMap(map);
        store.commit();
    }

    @Override
    public Iterator<Resource> listChildren(ResolveContext<Object> resolveContext, Resource resource) {
        return null;
    }

}
