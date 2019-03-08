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
package org.apache.sling.cli.impl.jira;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.osgi.service.component.annotations.Component;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@Component(service = VersionFinder.class)
public class VersionFinder {

    public Version find(String versionName) throws IOException {
        Version version;
        
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            version = findVersion(versionName, client);
            populateRelatedIssuesCount(client, version);
        }
        
        return version;
    }

    private Version findVersion(String versionName, CloseableHttpClient client) throws IOException {
        Version version;
        HttpGet get = new HttpGet("https://issues.apache.org/jira/rest/api/2/project/SLING/versions");
        get.addHeader("Accept", "application/json");
        try (CloseableHttpResponse response = client.execute(get)) {
            try (InputStream content = response.getEntity().getContent();
                    InputStreamReader reader = new InputStreamReader(content)) {
                if (response.getStatusLine().getStatusCode() != 200)
                    throw new IOException("Status line : " + response.getStatusLine());
                Gson gson = new Gson();
                Type collectionType = TypeToken.getParameterized(List.class, Version.class).getType();
                List<Version> versions = gson.fromJson(reader, collectionType);
                version = versions.stream()
                    .filter(v -> v.getName().equals(versionName))
                    .findFirst()
                    .orElseThrow( () -> new IllegalArgumentException("No version found with name " + versionName));
            }
        }
        return version;
    }

    private void populateRelatedIssuesCount(CloseableHttpClient client, Version version) throws IOException {

        HttpGet get = new HttpGet("https://issues.apache.org/jira/rest/api/2/version/" + version.getId() +"/relatedIssueCounts");
        get.addHeader("Accept", "application/json");
        try (CloseableHttpResponse response = client.execute(get)) {
            try (InputStream content = response.getEntity().getContent();
                    InputStreamReader reader = new InputStreamReader(content)) {
                if (response.getStatusLine().getStatusCode() != 200)
                    throw new IOException("Status line : " + response.getStatusLine());
                Gson gson = new Gson();
                VersionRelatedIssuesCount issuesCount = gson.fromJson(reader, VersionRelatedIssuesCount.class);
                
                version.setRelatedIssuesCount(issuesCount.getIssuesFixedCount());
            }
        }
    }

    static class VersionRelatedIssuesCount {

        private int issuesFixedCount;

        public int getIssuesFixedCount() {
            return issuesFixedCount;
        }

        public void setIssuesFixedCount(int issuesFixedCount) {
            this.issuesFixedCount = issuesFixedCount;
        }
    }
}
