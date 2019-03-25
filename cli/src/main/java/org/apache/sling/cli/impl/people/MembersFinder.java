/*~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 ~ Licensed to the Apache Software Foundation (ASF) under one
 ~ or more contributor license agreements.  See the NOTICE file
 ~ distributed with this work for additional information
 ~ regarding copyright ownership.  The ASF licenses this file
 ~ to you under the Apache License, Version 2.0 (the
 ~ "License"); you may not use this file except in compliance
 ~ with the License.  You may obtain a copy of the License at
 ~
 ~   http://www.apache.org/licenses/LICENSE-2.0
 ~
 ~ Unless required by applicable law or agreed to in writing,
 ~ software distributed under the License is distributed on an
 ~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 ~ KIND, either express or implied.  See the License for the
 ~ specific language governing permissions and limitations
 ~ under the License.
 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~*/
package org.apache.sling.cli.impl.people;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Component(service = MembersFinder.class)
public class MembersFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(MembersFinder.class);
    private static final String PEOPLE_ENDPOINT = "https://whimsy.apache.org/public/public_ldap_people.json";
    private static final String PROJECTS_ENDPOINT = "https://whimsy.apache.org/public/public_ldap_projects.json";
    private static final int STALENESS_IN_HOURS = 3;
    private Set<Member> members = Collections.emptySet();
    private long lastCheck = 0;

    public synchronized Set<Member> findMembers() {
        final Set<Member> _members = new HashSet<>();
        if (lastCheck == 0 || System.currentTimeMillis() > lastCheck + STALENESS_IN_HOURS * 3600 * 1000) {
            lastCheck = System.currentTimeMillis();
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                JsonParser parser = new JsonParser();
                Set<String> memberIds;
                Set<String> pmcMemberIds;
                try (CloseableHttpResponse response = client.execute(new HttpGet(PROJECTS_ENDPOINT))) {
                    try (InputStream content = response.getEntity().getContent();
                         InputStreamReader reader = new InputStreamReader(content)) {
                        if (response.getStatusLine().getStatusCode() != 200) {
                            throw new IOException("Status line : " + response.getStatusLine());
                        }
                        JsonElement jsonElement = parser.parse(reader);
                        JsonObject json = jsonElement.getAsJsonObject();
                        JsonObject sling = json.get("projects").getAsJsonObject().get("sling").getAsJsonObject();
                        memberIds = new HashSet<>();
                        pmcMemberIds = new HashSet<>();
                        for (JsonElement member : sling.getAsJsonArray("members")) {
                            memberIds.add(member.getAsString());
                        }
                        for (JsonElement pmcMember : sling.getAsJsonArray("owners")) {
                            pmcMemberIds.add(pmcMember.getAsString());
                        }
                    }
                }
                try (CloseableHttpResponse response = client.execute(new HttpGet(PEOPLE_ENDPOINT))) {
                    try (InputStream content = response.getEntity().getContent();
                         InputStreamReader reader = new InputStreamReader(content)) {
                        if (response.getStatusLine().getStatusCode() != 200) {
                            throw new IOException("Status line : " + response.getStatusLine());
                        }
                        JsonElement jsonElement = parser.parse(reader);
                        JsonObject json = jsonElement.getAsJsonObject();
                        JsonObject people = json.get("people").getAsJsonObject();
                        for (String id : memberIds) {
                            String name = people.get(id).getAsJsonObject().get("name").getAsString();
                            _members.add(new Member(id, name, pmcMemberIds.contains(id)));
                        }

                    }
                }
                members = Collections.unmodifiableSet(_members);
            } catch (IOException e) {
                LOGGER.error("Unable to retrieve Apache Sling project members.", e);
            }
        }
        return members;
    }

    public Member getMemberById(String id) {
        for (Member member : findMembers()) {
            if (id.equals(member.getId())) {
                return member;
            }
        }
        return null;
    }

}
