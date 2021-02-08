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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionResponse;
import org.apache.sling.distribution.Distributor;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionContext.ResultBuilder;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.apache.sling.testing.mock.sling.MockSling;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;

@RunWith(MockitoJUnitRunner.class)
public class ChunkedDistributionTest {
    
    @Mock
    private Distributor distributor;
    
    @Mock
    DistributionResponse resp;

    @Mock
    private Job job;

    @Mock
    private JobExecutionContext jcontext;

    @Mock
    private ResultBuilder resultBuilder;
    
    @Captor
    ArgumentCaptor<DistributionRequest> requestCaptor;
    
    @Test
    public void testDistribute() throws PersistenceException, LoginException {
        BundleContext context = MockOsgi.newBundleContext();
        ResourceResolverFactory resolverFactory = MockSling.newResourceResolverFactory(ResourceResolverType.JCR_OAK, context);
        ResourceResolver resolver = resolverFactory.getServiceResourceResolver(null);
        ResourceHelper.createResource(resolver, resolver.getResource("/"), "test");
        ChunkedDistribution dist = new ChunkedDistribution(distributor, resolverFactory);
        when(job.getProperty("path", String.class)).thenReturn("/test");
        when(distributor.distribute(Mockito.eq("publish"), Mockito.any(ResourceResolver.class), requestCaptor.capture())).thenReturn(resp);
        
        dist.distribute(resolver, "/test", Mode.OnlyHierarchyNodes, ChunkedDistribution.DEFAULT_CHUNK_SIZE, jcontext);
        
        DistributionRequest request = requestCaptor.getValue();
        
        assertThat(request.getPaths(), Matchers.arrayContaining("/test"));
    }
}
