package org.apache.sling.upgrade.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.sling.upgrade.BundleEntry;
import org.apache.sling.upgrade.ConfigEntry;
import org.apache.sling.upgrade.UpgradeRequest;
import org.apache.sling.upgrade.UpgradeService;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = UpgradeService.class)
public class UpgradeServiceImpl implements UpgradeService {

    private static final Logger log = LoggerFactory.getLogger(UpgradeServiceImpl.class);

    private BundleContext bundleContext;

    @Activate
    public void activate(ComponentContext context) {
        bundleContext = context.getBundleContext();
    }

    @Override
    public UpgradeRequest readSlingJar(InputStream jar) throws IOException {
        log.trace("readSlingJar");

        UpgradeRequest request = null;
        try (JarInputStream jarIs = new JarInputStream(jar)) {
            Manifest manifest = jarIs.getManifest();

            request = new UpgradeRequest(manifest);

            JarEntry entry = jarIs.getNextJarEntry();
            while (entry != null) {
                log.debug("Reading entry {}", entry.getName());
                if (BundleEntry.matches(entry)) {
                    request.getBundles().add(new BundleEntry(entry, jarIs, bundleContext));
                } else if (ConfigEntry.matches(entry)) {
                    request.getConfigs().add(new ConfigEntry(entry, jarIs));
                }
                entry = jarIs.getNextJarEntry();
            }

            Collections.sort(request.getBundles());
        }
        return request;
    }

}
