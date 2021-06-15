package org.apache.sling.feature.osgi.impl;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.feature.FeatureService;

public class Activator implements BundleActivator {

	private ServiceRegistration<FeatureService> reg = null;

	@Override
	public void start(BundleContext context) throws Exception {

		Dictionary<String, Object> dict = new Hashtable<>();
		dict.put(Constants.SERVICE_VENDOR, "sling");

		reg = context.registerService(FeatureService.class, new FeatureServiceImpl(), dict);
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		reg.unregister();

	}

}
