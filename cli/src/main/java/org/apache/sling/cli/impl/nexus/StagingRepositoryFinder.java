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
package org.apache.sling.cli.impl.nexus;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.sling.cli.impl.nexus.StagingRepository.Status;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.google.gson.Gson;

@Component(
    configurationPolicy = ConfigurationPolicy.REQUIRE,
    service = StagingRepositoryFinder.class
)
@Designate(ocd = StagingRepositoryFinder.Config.class)
public class StagingRepositoryFinder {
    
    private static final String REPOSITORY_PREFIX = "orgapachesling-";

    @ObjectClassDefinition
    static @interface Config {
        @AttributeDefinition(name="Username")
        String username();
        
        @AttributeDefinition(name="Password")
        String password();
    }

    private BasicCredentialsProvider credentialsProvider;
    
    @Activate
    protected void activate(Config cfg) {
        credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope("repository.apache.org", 443), 
                new UsernamePasswordCredentials(cfg.username(), cfg.password()));
    }
    
    public List<StagingRepository> list() throws IOException {
        return this. <List<StagingRepository>> withStagingRepositories( reader -> {
            Gson gson = new Gson();
            return gson.fromJson(reader, StagingRepositories.class).getData().stream()
                    .filter( r -> r.getType() == Status.closed)
                    .filter( r -> r.getRepositoryId().startsWith(REPOSITORY_PREFIX) )
                    .collect(Collectors.toList());            
        });
    }

    public StagingRepository find(int stagingRepositoryId) throws IOException {
        return this.<StagingRepository> withStagingRepositories( reader -> {
            Gson gson = new Gson();
            return gson.fromJson(reader, StagingRepositories.class).getData().stream()
                    .filter( r -> r.getType() == Status.closed)
                    .filter( r -> r.getRepositoryId().startsWith(REPOSITORY_PREFIX) )
                    .filter( r -> r.getRepositoryId().endsWith("-" + stagingRepositoryId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No repository found with id " + stagingRepositoryId));            
        });
    }
    
    private <T> T withStagingRepositories(Function<InputStreamReader, T> function) throws IOException {
        try ( CloseableHttpClient client = HttpClients.custom()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build() ) {
            HttpGet get = new HttpGet("https://repository.apache.org/service/local/staging/profile_repositories");
            get.addHeader("Accept", "application/json");
            try ( CloseableHttpResponse response = client.execute(get)) {
                try ( InputStream content = response.getEntity().getContent();
                        InputStreamReader reader = new InputStreamReader(content)) {
                    if ( response.getStatusLine().getStatusCode() != 200 )
                        throw new IOException("Status line : " + response.getStatusLine());
                    
                    return function.apply(reader);
                }
            }
        }       
    }
}
