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
package org.apache.sling.distribution.chunked;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.Distributor;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;
import org.apache.sling.event.jobs.consumer.JobExecutor;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(property = { JobConsumer.PROPERTY_TOPICS + "=" + ChunkedDistribution.TOPIC })
public class ChunkedDistribution implements JobExecutor {
    private static final int CHUNK_SIZE = 100;

    public static final String TOPIC = "sling/distribution";

    private Logger log = LoggerFactory.getLogger(this.getClass());

    Distributor distributor;

    ResourceResolverFactory resolverFactory;

    @Activate
    public ChunkedDistribution(@Reference Distributor distributor, @Reference ResourceResolverFactory resolverFactory) {
        this.distributor = distributor;
        this.resolverFactory = resolverFactory;
    }

    @Override
    public JobExecutionResult process(Job job, JobExecutionContext context) {
        try {
            String path = Objects.requireNonNull(job.getProperty("path", String.class), "No path parameter provided");
            try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(null)) {
                distribute(resolver, path, context);
                return context.result().succeeded();
            }
        } catch (Exception e) {
            return context.result().message(e.getMessage()).cancelled();
        }
        
    }

    public void distribute(ResourceResolver resolver, String path, JobExecutionContext context) {
        Resource parent = Objects.requireNonNull(resolver.getResource(path), "No resource present at path " + path);

        List<String> paths = DeepTree.getPaths(parent);
        List<List<String>> chunks = getChunks(paths);
        context.initProgress(chunks.size(), -1);
        for (List<String> chunk : chunks) {
            context.incrementProgressCount(1);
            distributeChunk(resolver, chunk);
            if (context.isStopped()) {
                throw new RuntimeException("Job stopped");
            }
        }
    }

    private List<List<String>> getChunks(List<String> paths) {
        List<List<String>> chunks = new ArrayList<>();
        int c = 0;
        while (c < paths.size()) {
            int next = Math.min(paths.size(), c + CHUNK_SIZE);
            chunks.add(paths.subList(c, next));
            c = next;
        }
        return chunks;
    }

    private void distributeChunk(ResourceResolver resolver, List<String> paths) {
        String firstPath = paths.iterator().next();
        try {
            String[] pathsAr = paths.toArray(new String[] {});
            log.info("Distributing chunk starting with {}", firstPath);
            DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.ADD, pathsAr);
            distributor.distribute("publish", resolver, request);
            log.info("Distributing request created");
        } catch (Exception e) {
            log.warn("Error creating distribution request", firstPath);
        }
    }
}
