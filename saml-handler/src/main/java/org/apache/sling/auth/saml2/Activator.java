/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sling.auth.saml2;

import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.xmlsec.config.impl.JavaCryptoValidationInitializer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.security.Provider;
import java.security.Security;
import java.util.Dictionary;
import java.util.Hashtable;

public class Activator implements BundleActivator {

    private static final Logger logger = LoggerFactory.getLogger(Activator.class);
    private ConfigurationAdmin configAdmin;

    public void start(BundleContext context) throws IOException, InvalidSyntaxException {
        // Classloading
        BundleWiring bundleWiring = context.getBundle().adapt(BundleWiring.class);
        ClassLoader loader = bundleWiring.getClassLoader();
        Thread thread = Thread.currentThread();
        thread.setContextClassLoader(InitializationService.class.getClassLoader());
        try {
            initializeOpenSaml();
        } catch (InitializationException e) {
            throw new SAML2RuntimeException("Java Cryptographic Extension could not initialize. " +
                    "This happens when JCE implementation is incomplete, and not meeting OpenSAML standards.", e);
        } finally {
            thread.setContextClassLoader(loader);
        }
        setConfigAdmin(context);
        if ( needsSamlJaas()){
            configureSamlJaas();
        }
    }

    public void stop(BundleContext context) throws IOException, InvalidSyntaxException {
        if (configAdmin != null){
            removeSamlJaas();
        }
    }

    public static void initializeOpenSaml() throws InitializationException{
        JavaCryptoValidationInitializer jcvi = new JavaCryptoValidationInitializer();
        jcvi.init();
        InitializationService.initialize();
        logger.info("Activating Apache Sling SAML2 SP Bundle. And Initializing JCE, Java Cryptographic Extension");
        for (Provider jceProvider : Security.getProviders()) {
            logger.info(jceProvider.getInfo());
        }
    }

    protected void configureSamlJaas() throws IOException {
        Dictionary<String, Object> props = new Hashtable();
        props.put("jaas.classname", "org.apache.sling.auth.saml2.sp.Saml2LoginModule");
        props.put("jaas.controlFlag", "Sufficient");
        props.put("jaas.realmName", "jackrabbit.oak");
        props.put("jaas.ranking", 110);
        configAdmin.createFactoryConfiguration("org.apache.felix.jaas.Configuration.factory", null).update(props);
    }

    protected boolean needsSamlJaas() throws IOException, InvalidSyntaxException {
        Configuration[] configs = configAdmin.listConfigurations("(jaas.classname=org.apache.sling.auth.saml2.sp.Saml2LoginModule)");
        return configs == null;
    }

    protected void removeSamlJaas() throws IOException, InvalidSyntaxException {
        Configuration[] configs = configAdmin.listConfigurations("(jaas.classname=org.apache.sling.auth.saml2.sp.Saml2LoginModule)");
        if (configs == null){
            return;
        }
        for ( Configuration config : configs){
            config.delete();
        }
    }

    public void setConfigAdmin(BundleContext bundleContext) {
        ServiceReference<?> serviceReference = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
        this.configAdmin = (ConfigurationAdmin) bundleContext.getService(serviceReference);
    }
}