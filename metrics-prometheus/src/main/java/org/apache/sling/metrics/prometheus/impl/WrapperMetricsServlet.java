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
package org.apache.sling.metrics.prometheus.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.Servlet;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardContextSelect;
import org.osgi.service.http.whiteboard.propertytypes.HttpWhiteboardServletPattern;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.MetricRegistryListener;
import com.codahale.metrics.Timer;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.dropwizard.DropwizardExports;
import io.prometheus.client.exporter.MetricsServlet;

/**
 * Exports metrics taking into account multiple registry instances
 * 
 * <p>For historical reasons, Sling and Oak have their own metrics registry implementations. This
 * may occur for other applications as well so we take care to gracefully handle multiple child
 * registries.</p>
 */
@HttpWhiteboardServletPattern("/metrics")
@HttpWhiteboardContextSelect("(osgi.http.whiteboard.context.name=org.osgi.service.http)")
@Component(service = Servlet.class)
@Designate(ocd = WrapperMetricsServletConfiguration.class)
public class WrapperMetricsServlet extends MetricsServlet {

    private static final long serialVersionUID = 1L;
    
    private final MetricRegistry metrics = new MetricRegistry();
    @SuppressWarnings("squid:S2226")
    private DropwizardExports exports;
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final ConcurrentMap<MetricRegistry, CopyMetricRegistryListener> childRegistries = new ConcurrentHashMap<>();

    @Activate
    public WrapperMetricsServlet() {
        this.exports = new DropwizardExports(metrics);
        CollectorRegistry.defaultRegistry.register(this.exports);
    }

    @Deactivate
    public void deactivate() {
        CollectorRegistry.defaultRegistry.unregister(this.exports);
    }

    @Reference(service = MetricRegistry.class, cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY)
    void bindMetricRegistry(MetricRegistry metricRegistry, Map<String, Object> properties) {
        
        log.info("Binding Metrics Registry...");
        
        String name = registryName(metricRegistry, properties);
        
        CopyMetricRegistryListener listener = new CopyMetricRegistryListener(this.metrics, metricRegistry, name);
        listener.start();
        childRegistries.put(metricRegistry, listener);
        log.info("Bound Metrics Registry {} ", name);
    }

    void unbindMetricRegistry(MetricRegistry metricRegistry, Map<String, Object> properties) {
        String name = registryName(metricRegistry, properties);
        
        CopyMetricRegistryListener metricRegistryListener = childRegistries.remove(metricRegistry);
        if (metricRegistryListener != null)
            metricRegistryListener.stop();
        log.info("Unbound Metrics Registry {} ", name);
    }

    private String registryName(MetricRegistry metricRegistry, Map<String, Object> properties) {
        String name = (String) properties.get("name");
        if (name == null)
            name = metricRegistry.toString();
        return name;
    }
    
    static class CopyMetricRegistryListener implements MetricRegistryListener {

        private MetricRegistry parent;
        private MetricRegistry child;
        private String name;

        public CopyMetricRegistryListener(MetricRegistry parent, MetricRegistry child, String name) {
            this.parent = parent;
            this.child = child;
            this.name = name;
        }
        
        public void start() {
            child.addListener(this);
        }
        
        public void stop() {
            child.removeListener(this);
            child.getMetrics().keySet().stream()
                .map( this::getMetricName )
                .forEach( this::removeMetric );
        }

        @Override
        public void onGaugeAdded(String name, Gauge<?> gauge) {
            addMetric(name, gauge);
            
        }

        @Override
        public void onGaugeRemoved(String name) {
            removeMetric(name);
            
        }

        @Override
        public void onCounterAdded(String name, Counter counter) {
            addMetric(name, counter);
        }

        @Override
        public void onCounterRemoved(String name) {
            removeMetric(name);
        }

        @Override
        public void onHistogramAdded(String name, Histogram histogram) {
            addMetric(name, histogram);
        }

        @Override
        public void onHistogramRemoved(String name) {
            removeMetric(name);
        }

        @Override
        public void onMeterAdded(String name, Meter meter) {
            addMetric(name, meter);
        }

        @Override
        public void onMeterRemoved(String name) {
            removeMetric(name);
        }

        @Override
        public void onTimerAdded(String name, Timer timer) {
            addMetric(name, timer);
        }

        @Override
        public void onTimerRemoved(String name) {
            removeMetric(name);
        }

        private void addMetric(String metricName, Metric m) {
            parent.register(getMetricName(metricName), m);
        }

        private void removeMetric(String metricName) {
            parent.remove(getMetricName(metricName));
        }

        private String getMetricName(String metricName) {
            return name + "_" + metricName;
        }
    }
}
