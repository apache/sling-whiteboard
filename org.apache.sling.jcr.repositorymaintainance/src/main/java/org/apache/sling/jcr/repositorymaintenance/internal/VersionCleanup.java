/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.jcr.repositorymaintenance.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import javax.management.DynamicMBean;

import org.apache.jackrabbit.oak.commons.jmx.AnnotatedStandardMBean;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.repositorymaintenance.VersionCleanupConfig;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 */
@Component(service = { VersionCleanupMBean.class, Runnable.class, DynamicMBean.class }, property = {
        "jmx.objectname=org.apache.sling.jcr.repositorymaintenance:type=VersionCleanup",
        "scheduler.concurrent:Boolean=false" }, configurationPolicy = ConfigurationPolicy.REQUIRE, immediate = true)
@Designate(ocd = VersionCleanupConfig.class)
public class VersionCleanup extends AnnotatedStandardMBean implements Runnable, VersionCleanupMBean {

    private static final Logger log = LoggerFactory.getLogger(VersionCleanup.class);

    private Thread cleanupThread;
    private final ResourceResolverFactory factory;
    private long lastCleanedVersions;
    private String lastFailureMessage;
    private final List<VersionCleanupPath> versionCleanupConfigs;

    @Activate
    public VersionCleanup(
            @Reference(cardinality = ReferenceCardinality.AT_LEAST_ONE, policyOption = ReferencePolicyOption.GREEDY) final List<VersionCleanupPath> versionCleanupConfigs,
            @Reference final ResourceResolverFactory factory) {
        super(VersionCleanupMBean.class);
        this.factory = factory;
        this.versionCleanupConfigs = versionCleanupConfigs;
        versionCleanupConfigs.sort((c1, c2) -> c1.getPath().compareTo(c2.getPath()) * -1);

    }

    private String getPath(final Session session, final VersionHistory versionHistory) throws RepositoryException {
        String identifier = versionHistory.getVersionableIdentifier();
        try {
            Node versionableNode = session.getNodeByIdentifier(identifier);
            return versionableNode.getPath();
        } catch (ItemNotFoundException infe) {
            log.debug("Unable to get versionable node by ID: {}, exception: {}", identifier, infe.getMessage());
            return versionHistory.getProperty(session.getWorkspace().getName()).getString();
        }
    }

    private void cleanupVersions(final Session session, final Resource history) {
        try {
            final VersionHistory versionHistory = (VersionHistory) session.getItem(history.getPath());
            final String path = getPath(session, versionHistory);
            final VersionCleanupPath config = VersionCleanupPath.getMatchingConfiguration(this.versionCleanupConfigs,
                    path);
            int limit = config.getLimit();

            if (!isMatchingVersion(session, path, versionHistory) && !config.isKeepVersions() && limit > 0) {
                log.debug("Deleted, removing all but last version");
                limit = 1;
            }
            log.debug("Cleaning up versions for: {}", versionHistory.getPath());
            final VersionIterator versionIterator = versionHistory.getAllVersions();
            final List<String> versionNames = new ArrayList<>();
            while (versionIterator.hasNext()) {
                final Version version = versionIterator.nextVersion();
                if (!version.getName().equals("jcr:rootVersion")) {
                    versionNames.add(version.getName());
                }
            }
            if (versionNames.size() > limit) {
                final List<String> toCleanup = versionNames.subList(0, versionNames.size() - limit);
                log.info("Cleaning up {} versions from {} at: {}", toCleanup.size(), path, versionHistory.getPath());
                for (final String item : toCleanup) {
                    versionHistory.removeVersion(item);
                    log.trace("Cleaned up: {}", item);
                    lastCleanedVersions++;
                }
            }
        } catch (final RepositoryException re) {
            log.warn("Failed to cleanup version history for: {}", history.getPath(), re);
        }

    }

    private void findVersions(final Session session, final Resource resource)
            throws RepositoryException, InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException("Process interrupted");
        }
        log.debug("Finding versions under: {}", resource.getPath());
        if ("nt:versionHistory".equals(resource.getResourceType())) {
            resource.getResourceResolver().refresh();
            cleanupVersions(session, resource);
        } else {
            for (final Resource child : resource.getChildren()) {
                findVersions(session, child);
            }
        }
    }

    private boolean isMatchingVersion(Session session, String path, VersionHistory versionHistory)
            throws RepositoryException {
        try {
            VersionManager versionManager = session.getWorkspace().getVersionManager();
            String baseVersionPath = versionManager.getBaseVersion(path).getParent().getPath();
            String versionHistoryPath = versionHistory.getPath();

            return session.nodeExists(path) && isVersionable(session.getNode(path))
                    && baseVersionPath.equals(versionHistoryPath);
        } catch (PathNotFoundException pnfe) {
            log.debug("Path: {} not found: {}", path, pnfe.getMessage());
            return false;
        }
    }

    private boolean isVersionable(final Node node) throws RepositoryException {
        return node != null && node.isNodeType("{http://www.jcp.org/jcr/mix/1.0}versionable");
    }

    @Override
    public void run() {
        if (isRunning()) {
            log.warn("Version cleanup already running!");
        } else {
            cleanupThread = new Thread((this::doRun));
            cleanupThread.setDaemon(true);
            cleanupThread.start();
        }
    }

    private void doRun() {
        log.info("Running version cleanup");
        boolean interrupted = false;
        boolean succeeded = false;
        String failureMessage = null;
        lastCleanedVersions = 0;
        try {
            try (final ResourceResolver adminResolver = factory.getServiceResourceResolver(
                    Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, "sling-versionmgr"))) {
                final Resource versionRoot = adminResolver.getResource("/jcr:system/jcr:versionStorage");
                final Session session = Optional.ofNullable(versionRoot.getResourceResolver().adaptTo(Session.class))
                        .orElseThrow(() -> new RepositoryException("Failed to get session"));
                for (final Resource folder : versionRoot.getChildren()) {
                    log.info("Traversing and cleaning: {}", folder.getPath());
                    findVersions(session, folder);
                }
                succeeded = true;
            }
        } catch (final LoginException le) {
            log.error("Failed to run version cleanup, cannot get service user", le);
            failureMessage = "Failed to run version cleanup, cannot get service user";
        } catch (final RepositoryException re) {
            log.error("Failed to run version cleanup", re);
            failureMessage = "Failed to run version cleanup";
        } catch (final InterruptedException e) { // no need to do anything, at this point nearly done
            log.info("Process interrupted, quitting");
            interrupted = true;
        } finally {
            if (succeeded) {
                this.lastFailureMessage = null;
            } else if (!interrupted) {
                lastFailureMessage = failureMessage != null ? failureMessage
                        : "Failed due to unexpected exception, see logs";
            }
        }
    }

    @Override
    public boolean isRunning() {
        return cleanupThread != null && cleanupThread.isAlive();
    }

    @Override
    public boolean isFailed() {
        return lastFailureMessage != null;
    }

    @Override
    public String getLastMessage() {
        return lastFailureMessage;
    }

    @Override
    public long getLastCleanedVersionsCount() {
        return lastCleanedVersions;
    }

    @Override
    public void start() {
        this.run();
    }

    @Override
    public void stop() {
        Optional.ofNullable(cleanupThread).ifPresent(Thread::interrupt);
    }
}
