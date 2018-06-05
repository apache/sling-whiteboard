/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.fileoptim.impl;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.fileoptim.FileOptimizer;
import org.apache.sling.fileoptim.FileOptimizerService;
import org.apache.sling.fileoptim.OptimizationResult;
import org.apache.sling.fileoptim.impl.FileOptimizerServiceImpl.Config;
import org.apache.sling.fileoptim.models.OptimizedFile;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the FileOptimizerService
 */
@Component(service = FileOptimizerService.class, immediate = true)
@Designate(ocd = Config.class)
public class FileOptimizerServiceImpl implements FileOptimizerService, ServiceListener {

	@ObjectClassDefinition(name = "%file.optimizer.name", description = "%file.optimizer.description", localization = "OSGI-INF/l10n/bundle")
	public @interface Config {

		@AttributeDefinition(name = "%file.optimizer.hash.algorithm.name", description = "%file.optimizer.hash.algorithm.description")
		String hashAlgorithm() default "MD5";
	}

	private static final Logger log = LoggerFactory.getLogger(FileOptimizerServiceImpl.class);

	private BundleContext bundleContext;

	private Config config;

	private Map<String, List<ServiceReference<FileOptimizer>>> fileOptimizers = new HashMap<String, List<ServiceReference<FileOptimizer>>>();

	@Activate
	@Modified
	public void activate(ComponentContext context, Config config) throws InvalidSyntaxException {
		bundleContext = context.getBundleContext();
		this.config = config;
		this.rebuildOptimizerCache();
		bundleContext.addServiceListener(this, "(" + Constants.OBJECTCLASS + "=" + FileOptimizer.class.getName() + ")");
	}

	private void addOptimizer(Map<String, List<ServiceReference<FileOptimizer>>> tempCache, String metaType,
			ServiceReference<FileOptimizer> ref) {
		if (!tempCache.containsKey(metaType)) {
			tempCache.put(metaType, new ArrayList<ServiceReference<FileOptimizer>>());
		}
		tempCache.get(metaType).add(ref);
	}

	private String calculateHash(byte[] bytes) {
		MessageDigest messageDigest;
		try {
			messageDigest = MessageDigest.getInstance(config.hashAlgorithm());
			messageDigest.reset();
			messageDigest.update(bytes);
			return Base64.encodeBase64String(messageDigest.digest());
		} catch (NoSuchAlgorithmException e) {
			log.error("Exception generating hash", e);
		}
		return null;
	}

	@Override
	public boolean canOptimize(Resource fileResource) {
		if (!fileResource.getName().equals(JcrConstants.JCR_CONTENT)
				&& fileResource.getChild(JcrConstants.JCR_CONTENT) != null) {
			fileResource = fileResource.getChild(JcrConstants.JCR_CONTENT);
		}
		OptimizedFile of = fileResource.adaptTo(OptimizedFile.class);
		return of != null && of.getDisabled() != true && fileOptimizers.containsKey(of.getMimeType())
				&& fileOptimizers.get(of.getMimeType()).size() > 0;
	}

	@Deactivate
	public void deactivate(ComponentContext context) {
		context.getBundleContext().removeServiceListener(this);
	}

	/**
	 * @return the fileOptimizers
	 */
	public Map<String, List<ServiceReference<FileOptimizer>>> getFileOptimizers() {
		return fileOptimizers;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.sling.fileoptim.FileOptimizerService#getOptimizedContents(org.
	 * apache.sling.api.resource.Resource)
	 */
	@Override
	public OptimizationResult getOptimizedContents(Resource fileResource) throws IOException {
		if (!fileResource.getName().equals(JcrConstants.JCR_CONTENT)
				&& fileResource.getChild(JcrConstants.JCR_CONTENT) != null) {
			fileResource = fileResource.getChild(JcrConstants.JCR_CONTENT);
		}
		OptimizationResult result = new OptimizationResult(fileResource);

		OptimizedFile optim = fileResource.adaptTo(OptimizedFile.class);

		boolean optimize = true;
		byte[] original = IOUtils.toByteArray(optim.getContent());
		if (StringUtils.isNotBlank(optim.getHash()) && optim.getHash().equals(calculateHash(original))) {
			optimize = false;
		}

		if (optimize) {
			log.debug("Optimizing file resource {}", fileResource);
			List<ServiceReference<FileOptimizer>> optimizers = fileOptimizers.get(optim.getMimeType());
			for (ServiceReference<FileOptimizer> ref : optimizers) {
				FileOptimizer optimizer = bundleContext.getService(ref);
				if (optimizer != null) {
					byte[] optimized = optimizer.optimizeFile(original, optim.getMimeType());
					if (optimized != null && optimized.length < original.length) {

						double savings = 1.0 - ((double) optimized.length / (double) original.length);

						log.debug("Optimized file with {} saving {}%", optimizer.getName(), Math.round(savings * 100));
						result.setAlgorithm(optimizer.getName());
						result.setSavings(savings);
						result.setOptimized(true);
						result.setOptimizedSize(optimized.length);
						result.setOriginalSize(original.length);
						result.setOptimizedContents(optimized);
					} else {
						log.debug("Optimizer {} was not able to optimize the file", optimizer.getName());
					}
				} else {
					log.warn("No service retrieved for service reference {}", ref);
				}
			}
		} else {
			log.trace("Resource {} is already optimized", fileResource);
		}
		return result;
	}

	@Override
	public boolean isOptimized(Resource fileResource) {

		if (!fileResource.getName().equals(JcrConstants.JCR_CONTENT)
				&& fileResource.getChild(JcrConstants.JCR_CONTENT) != null) {
			fileResource = fileResource.getChild(JcrConstants.JCR_CONTENT);
		}

		OptimizedFile of = fileResource.adaptTo(OptimizedFile.class);
		try {
			String calculatedHash = calculateHash(IOUtils.toByteArray(of.getContent()));
			log.debug("Comparing stored {} and calculated {} hashes", of.getHash(), calculatedHash);
			return ObjectUtils.equals(of.getHash(), calculatedHash);
		} catch (IOException e) {
			log.error("Exception checking if file optimized, assuming false", e);
			return false;
		}
	}

	@Override
	public OptimizationResult optimizeFile(Resource fileResource, boolean autoCommit) throws IOException {

		OptimizationResult result = getOptimizedContents(fileResource);

		ModifiableValueMap mvm = fileResource.adaptTo(ModifiableValueMap.class);

		Set<String> mixins = new HashSet<String>(Arrays.asList(mvm.get(JcrConstants.JCR_MIXINTYPES, new String[0])));
		mixins.add(OptimizedFile.MT_OPTIMIZED);
		mvm.put(JcrConstants.JCR_MIXINTYPES, mixins.toArray(new String[] {}));

		mvm.put(OptimizedFile.PN_ALGORITHM, result.getAlgorithm());
		mvm.put(OptimizedFile.PN_HASH, calculateHash(result.getOptimizedContents()));
		mvm.put(OptimizedFile.PN_ORIGINAL, result.getOptimizedContents());
		mvm.put(OptimizedFile.PN_SAVINGS, result.getSavings());

		OptimizedFile optim = fileResource.adaptTo(OptimizedFile.class);
		mvm.put(JcrConstants.JCR_DATA, IOUtils.toByteArray(optim.getContent()));

		if (autoCommit) {
			log.debug("Persisting changes...");
			fileResource.getResourceResolver().commit();
		}

		return result;

	}

	private void rebuildOptimizerCache() {
		log.debug("rebuildOptimizerCache");
		Map<String, List<ServiceReference<FileOptimizer>>> tempCache = new HashMap<String, List<ServiceReference<FileOptimizer>>>();
		Collection<ServiceReference<FileOptimizer>> references = null;
		try {
			references = bundleContext.getServiceReferences(FileOptimizer.class, null);
		} catch (Exception e) {
			log.error("Exception retrieving service references", e);
		}
		for (ServiceReference<FileOptimizer> ref : references) {
			Object mimeType = ref.getProperty(FileOptimizer.MIME_TYPE);
			if (mimeType != null && mimeType instanceof String[]) {
				for (String mt : (String[]) mimeType) {
					addOptimizer(tempCache, mt, ref);
				}
			} else if (mimeType != null) {
				addOptimizer(tempCache, (String) mimeType, ref);
			}
		}
		for (List<ServiceReference<FileOptimizer>> optList : tempCache.values()) {
			Collections.sort(optList);
		}
		this.fileOptimizers = tempCache;
	}

	@Override
	public void serviceChanged(ServiceEvent event) {
		rebuildOptimizerCache();
	}

	/**
	 * @param fileOptimizers
	 *            the fileOptimizers to set
	 */
	public void setFileOptimizers(Map<String, List<ServiceReference<FileOptimizer>>> fileOptimizers) {
		this.fileOptimizers = fileOptimizers;
	}

}
