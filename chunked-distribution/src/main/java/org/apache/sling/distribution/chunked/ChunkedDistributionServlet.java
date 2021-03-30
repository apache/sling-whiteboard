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

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.JobManager.QueryType;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = { Servlet.class })
@SlingServletPaths("/libs/sling/distribution/tree")
@SlingServletResourceTypes(
    resourceTypes="sling/distribution/tree", 
    methods= {"GET", "POST"},
    extensions="html"
)
public class ChunkedDistributionServlet extends SlingAllMethodsServlet {
    
    private JobManager jobMananger;

    @Activate
    public ChunkedDistributionServlet(@Reference JobManager jobMananger) {
        this.jobMananger = jobMananger;
    }
    
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        String type = request.getParameter("type");
        QueryType queryType = type !=null ? QueryType.valueOf(type) : QueryType.ALL;
        TimeZone tz = TimeZone.getTimeZone("UTC");
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'"); // Quoted "Z" to indicate UTC, no timezone offset
        df.setTimeZone(tz);
        PrintWriter wr = response.getWriter();
        Collection<Job> jobs = jobMananger.findJobs(queryType, ChunkedDistribution.TOPIC, 10, (Map<String, Object>[]) null);
        wr.println("Jobs");
        printJobs(df, wr, jobs);
    }

    private void printJobs(DateFormat df, PrintWriter wr, Collection<Job> jobs) {
        for (Job job : jobs) {
            String startedAt = df.format(job.getProcessingStarted().getTime());
            wr.println(String.format("id: %s, status:%s, step: %d/%d, startedAt: %s", job.getId(), job.getJobState().toString(), job.getFinishedProgressStep(),  job.getProgressStepCount(), startedAt));
            String[] log = job.getProgressLog();
            if (log != null) {
                for (String line : log) {
                    wr.println("  " + line);
                }
            }
        }
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        PrintWriter wr = response.getWriter();
        String command = request.getParameter("command");
        String jobId = request.getParameter("id");
        if ("stop".equals(command)) {
            jobMananger.stopJobById(jobId);
            wr.println("Sent stop signal to " + jobId);
            return;
        }
        String path = request.getParameter(ChunkedDistribution.KEY_PATH);
        String modeSt = request.getParameter(ChunkedDistribution.KEY_MODE);
        String mode = modeSt != null ? modeSt : Mode.OnlyHierarchyNodes.name();

        String chunkSizeSt = request.getParameter(ChunkedDistribution.KEY_CHUNK_SIZE);
        Integer chunkSize = chunkSizeSt != null ? Integer.parseInt(chunkSizeSt) : ChunkedDistribution.DEFAULT_CHUNK_SIZE;
        
        Map<String, Object> props = new HashMap<>();
        props.put("path", path);
        props.put("mode", mode);
        props.put("chunkSize", chunkSize);
        Job job = jobMananger.addJob(ChunkedDistribution.TOPIC, props);
        wr.println(job.getId());
    }
    
}
