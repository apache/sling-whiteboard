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
import java.util.regex.Pattern;

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
    public static final int DEFAULT_CHUNK_SIZE = 100;
    public static final String KEY_PATH = "path";
    public static final String KEY_MODE = "mode";
    public static final String KEY_CHUNK_SIZE = "chunkSize";
    public static final String TOPIC = "sling/whiteboard/distribution/chunked";

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
        String path = requireParam(job, KEY_PATH, String.class);
        String modeSt = requireParam(job, KEY_MODE, String.class);
        Mode mode = Mode.valueOf(modeSt);
        Integer chunkSize = requireParam(job, KEY_CHUNK_SIZE, Integer.class);
        log.info("Starting chunked tree distribution for path {}", path);
        try {
            try (ResourceResolver resolver = resolverFactory.getServiceResourceResolver(null)) {
                distribute(resolver, path, mode, chunkSize, context);
                log.info("Finished chunked tree distribution for path {}", path);
                return context.result().succeeded();
            }
        } catch (Exception e) {
            log.warn("Error distributing tree {} with mode {}", path, mode, e);
            context.log(e.getMessage());
            return context.result().message(e.getMessage()).cancelled();
        }
        
    }

    private <T> T requireParam(Job job, String key, Class<T> type) {
        return Objects.requireNonNull(job.getProperty(key, type), "No " + key + " parameter provided");
    }

    public void distribute(ResourceResolver resolver, String path, Mode mode, Integer chunkSize, JobExecutionContext context) {
        Resource parent = Objects.requireNonNull(resolver.getResource(path), "No resource present at path " + path);
        context.log("Getting tree nodes for path=" + path);
        List<String> paths = DeepTree.getPaths(parent, mode);
        List<List<String>> chunks = getChunks(paths, chunkSize);
        context.initProgress(chunks.size(), -1);
        int progress = 0;
        for (List<String> chunk : chunks) {
            context.incrementProgressCount(1);
            progress ++;
            String firstPath = chunk.iterator().next();
            String msg = String.format("Distributing chunk %d/%d starting with %s", progress, chunks.size(), firstPath);
            log.info(msg);
            context.log(msg);
            distributeChunk(resolver, chunk, context);
            if (context.isStopped()) {
                throw new RuntimeException("Job stopped");
            }
        }
    }

    private List<List<String>> getChunks(List<String> paths, Integer chunkSize) {
        List<List<String>> chunks = new ArrayList<>();
        int c = 0;
        while (c < paths.size()) {
            int next = Math.min(paths.size(), c + chunkSize);
            chunks.add(paths.subList(c, next));
            c = next;
        }
        return chunks;
    }

    private void distributeChunk(ResourceResolver resolver, List<String> paths, JobExecutionContext context) {
        try {
            String[] pathsAr = paths.toArray(new String[] {});
            DistributionRequest request = new SimpleDistributionRequest(DistributionRequestType.ADD, pathsAr);
            distributor.distribute("publish", resolver, request);
        } catch (Exception e) {
            String firstPath = paths.iterator().next();
            String msg = "Error creating distribution request first path " + firstPath + " msg: " + e.getMessage();
            throw new RuntimeException(msg, e);
        }
    }

}
