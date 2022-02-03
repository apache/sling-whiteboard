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
package org.apache.sling.commons.log.json.impl;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.spi.ContextAwareBase;
import net.logstash.logback.encoder.LogstashEncoder;

/**
 * OSGi service for loading the root Sling Commons Log configuration and
 * registering the custom logger configurations.
 * 
 * Note this requires the root logger of the Logaback context to have an
 * appender of type {@link ConsoleAppender} with the name "console" which has
 * it's encoder set as an {@link LogstashEncoder}
 */
@Component(immediate = true, service = OsgiConfigurator.class)
@Designate(ocd = OsgiConfigurator.Config.class)
public class OsgiConfigurator extends ContextAwareBase {

    private final List<LoggerConfiguration> queuedConfigurations = new CopyOnWriteArrayList<>();
    private final Set<String> loggerNames = Collections
            .newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private final Map<LoggerConfiguration, List<Logger>> attachedLoggers = new ConcurrentHashMap<>();

    private Config config;
    private LoggerContext loggerContext;
    private OutputStreamAppender<ILoggingEvent> appender;
    private Instant start;
    private ScheduledFuture<?> future;
    private boolean initialized = false;
    private LogManager logManager;

    private LogFileSourceJsonProvider logFileSourceProvider = new LogFileSourceJsonProvider();

    @Activate
    public void activate(Config config) {
        this.config = config;
        start = Instant.now();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        future = scheduler.scheduleWithFixedDelay(this::initialize, 1, 1, TimeUnit.MILLISECONDS);
    }

    @Deactivate
    public void deactivate() {
        if (loggerContext != null) {
            loggerContext.stop();
        }
    }

    private void initialize() {
        if (isSlf4jInitialized()) {
            future.cancel(false);

            loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            this.setContext(loggerContext);

            addInfo("OsgiConfigurator started at " + start.toEpochMilli());
            addInfo("Initialization started after " + (Instant.now().toEpochMilli() - start.toEpochMilli()) + "ms");

            addInfo("Initializing OSGi Configuration");
            Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
            applyLogManagerConfiguration(rootLogger);

            Appender<ILoggingEvent> foundAppender = rootLogger.getAppender(config.appenderName());
            if (!(foundAppender instanceof OutputStreamAppender<?>)) {
                addError("Did not find expected appender: " + config.appenderName());
                return;
            }
            this.appender = (OutputStreamAppender<ILoggingEvent>) foundAppender;

            Encoder<?> foundEncoder = appender.getEncoder();
            if (!(foundEncoder instanceof LogstashEncoder)) {
                addWarn("Did not find expected logstash encoder");
            } else {
                LogstashEncoder encoder = (LogstashEncoder) foundEncoder;
                logFileSourceProvider.setContext(context);
                logFileSourceProvider.start();
                encoder.addProvider(logFileSourceProvider);
            }
            addInfo("OSGi initialization complete");
            initialized = true;

            addInfo("Attaching " + queuedConfigurations.size() + " queued configurations");
            queuedConfigurations.forEach(this::attachLogger);
            queuedConfigurations.clear();
        }
    }

    private void applyLogManagerConfiguration(Logger rootLogger) {
        addInfo("Applying log manager configuration");
        loggerContext.setMaxCallerDataDepth(logManager.getConfig().org_apache_sling_commons_log_maxCallerDataDepth());
        loggerContext
                .setPackagingDataEnabled(logManager.getConfig().org_apache_sling_commons_log_packagingDataEnabled());
        rootLogger.setLevel(Level.valueOf(logManager.getConfig().org_apache_sling_commons_log_level()));
        addInfo("Log manager configuration applied");
    }

    private static boolean isSlf4jInitialized() {
        return LoggerFactory.getILoggerFactory() instanceof LoggerContext;
    }

    @Reference(cardinality = ReferenceCardinality.MANDATORY, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void bindLogManager(LogManager logManager) {
        if (!isInitialized()) {
            this.logManager = logManager;
        } else {
            this.logManager = logManager;
            applyLogManagerConfiguration(loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME));
        }
    }

    protected void unbindLogManager(LogManager logManager) {
        // nothing needs done, we'll wait until a new one is provided
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    protected void bindLoggerConfiguration(LoggerConfiguration config) {
        if (!isInitialized()) {
            queuedConfigurations.add(config);
        } else {
            attachLogger(config);
        }
    }

    private void attachLogger(LoggerConfiguration config) {
        List<Logger> loggers = Arrays.stream(config.getConfig().org_apache_sling_commons_log_names())
                .filter(n -> !loggerNames.contains(n)).map(loggerName -> {
                    addInfo("Adding logger: " + loggerName + " at level "
                            + config.getConfig().org_apache_sling_commons_log_level() + " from config: "
                            + config.getPid());
                    Logger logger = loggerContext.getLogger(loggerName);
                    logger.setLevel(Level.valueOf(config.getConfig().org_apache_sling_commons_log_level()));
                    logger.addAppender(appender);
                    loggerNames.add(loggerName);
                    return logger;
                }).collect(Collectors.toList());
        attachedLoggers.put(config, loggers);
        logFileSourceProvider.setAttachedLoggers(attachedLoggers);
    }

    protected void unbindLoggerConfiguration(LoggerConfiguration config) {
        if (!isInitialized()) {
            queuedConfigurations.remove(config);
        } else {
            Optional.ofNullable(attachedLoggers.remove(config)).orElse(Collections.emptyList()).forEach(logger -> {
                addInfo("Removing logger: " + logger + " provided by config: "
                        + config.getConfig().getClass().getName());
                loggerNames.remove(logger.getName());
                logger.detachAndStopAllAppenders();
            });
            logFileSourceProvider.setAttachedLoggers(attachedLoggers);
        }
    }

    /**
     * @return the initialized
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * @return the attachedLoggers
     */
    public Map<LoggerConfiguration, List<Logger>> getAttachedLoggers() {
        return attachedLoggers;
    }

    /**
     * @return the config
     */
    public Config getConfig() {
        return config;
    }

    /**
     * @param config the config to set
     */
    public void setConfig(Config config) {
        this.config = config;
    }

    @ObjectClassDefinition(name = "%log.json.name", description = "%log.json.description", localization = "OSGI-INF/l10n/metatype")
    public @interface Config {
        @AttributeDefinition(name = "%appenderName.name", description = "%appenderName.description")
        String appenderName() default "console";
    }
}
