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
package org.apache.sling.cli.impl.release;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.cli.impl.Command;
import org.apache.sling.cli.impl.Credentials;
import org.apache.sling.cli.impl.CredentialsService;
import org.apache.sling.cli.impl.nexus.StagingRepository;
import org.apache.sling.cli.impl.nexus.StagingRepositoryFinder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Command.class,
    property = {
        Command.PROPERTY_NAME_COMMAND + "=release",
        Command.PROPERTY_NAME_SUBCOMMAND + "=update-reporter",
        Command.PROPERTY_NAME_SUMMARY + "=Updates the Apache Reporter System with the new release information"
    }
)
public class UpdateReporterCommand implements Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateReporterCommand.class);

    @Reference
    private StagingRepositoryFinder repoFinder;

    @Reference
    private CredentialsService credentialsService;

    private CredentialsProvider credentialsProvider;

    @Override
    public void execute(String target) {
        try {
            StagingRepository repository = repoFinder.find(Integer.parseInt(target));
            Release release = Release.fromString(repository.getDescription());
            try (CloseableHttpClient client =
                         HttpClients.custom().setDefaultCredentialsProvider(credentialsProvider).build()) {
                HttpPost post = new HttpPost("https://reporter.apache.org/addrelease.py");
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                List<NameValuePair> parameters = new ArrayList<>();
                Date now = new Date();
                parameters.add(new BasicNameValuePair("date", Long.toString(now.getTime() / 1000)));
                parameters.add(new BasicNameValuePair("committee", "sling"));
                parameters.add(new BasicNameValuePair("version", release.getFullName()));
                parameters.add(new BasicNameValuePair("xdate", simpleDateFormat.format(now)));
                post.setEntity(new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8));
                try (CloseableHttpResponse response = client.execute(post)) {
                    if (response.getStatusLine().getStatusCode() != 200) {
                        throw new IOException(String.format("The Apache Reporter System update failed. Got response code %s instead of " +
                                "200.", response.getStatusLine().getStatusCode()));
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error(String.format("Unable to update reporter service; passed command: %s.", target), e);
        }

    }

    @Activate
    private void activate() {
        Credentials credentials = credentialsService.getCredentials();
        credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(new AuthScope("reporter.apache.org", 443),
                new UsernamePasswordCredentials(credentials.getUsername(), credentials.getPassword()));
    }
}
