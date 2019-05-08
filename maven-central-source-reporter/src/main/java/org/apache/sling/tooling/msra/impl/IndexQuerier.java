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
package org.apache.sling.tooling.msra.impl;

import static org.apache.maven.index.ArtifactAvailability.PRESENT;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.maven.index.ArtifactInfo;
import org.apache.maven.index.ArtifactInfoGroup;
import org.apache.maven.index.GroupedSearchRequest;
import org.apache.maven.index.GroupedSearchResponse;
import org.apache.maven.index.Indexer;
import org.apache.maven.index.MAVEN;
import org.apache.maven.index.context.IndexCreator;
import org.apache.maven.index.context.IndexingContext;
import org.apache.maven.index.expr.SourcedSearchExpression;
import org.apache.maven.index.search.grouping.GAGrouping;
import org.apache.maven.index.updater.IndexUpdateRequest;
import org.apache.maven.index.updater.IndexUpdateResult;
import org.apache.maven.index.updater.IndexUpdater;
import org.apache.maven.index.updater.ResourceFetcher;
import org.apache.maven.index.updater.WagonHelper;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.events.TransferListener;
import org.apache.maven.wagon.observers.AbstractTransferListener;
import org.codehaus.plexus.DefaultContainerConfiguration;
import org.codehaus.plexus.DefaultPlexusContainer;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.PlexusContainerException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexQuerier {

    private static ArtifactInfo extract(ArtifactInfoGroup group) {

        return group.getArtifactInfos()
            .stream()
            .filter(a -> a.getClassifier() == null)
            .findFirst()
            .orElse(null);
    }

    private static final File OUT_DIR = new File("out");

    private final PlexusContainer plexusContainer;

    private final Indexer indexer;

    private final IndexUpdater indexUpdater;

    private final Wagon httpWagon;

    private IndexingContext centralContext;

    private final Logger logger = LoggerFactory.getLogger(getClass());


    public IndexQuerier() throws PlexusContainerException, ComponentLookupException {

        DefaultContainerConfiguration config = new DefaultContainerConfiguration();
        config.setClassPathScanning(PlexusConstants.SCANNING_INDEX);

        this.plexusContainer = new DefaultPlexusContainer(config);

        // lookup the indexer components from plexus
        this.indexer = plexusContainer.lookup(Indexer.class);
        this.indexUpdater = plexusContainer.lookup(IndexUpdater.class);
        // lookup wagon used to remotely fetch index
        this.httpWagon = plexusContainer.lookup(Wagon.class, "https");
    }

    public void loadIndex() throws ComponentLookupException, IOException {
        // Files where local cache is (if any) and Lucene Index should be located
        File centralLocalCache = new File(OUT_DIR, "central-cache");
        File centralIndexDir = new File(OUT_DIR, "central-index");

        // Creators we want to use (search for fields it defines)
        List<IndexCreator> indexers = new ArrayList<>();
        indexers.add(plexusContainer.lookup(IndexCreator.class, "min"));

        // Create context for central repository index
        centralContext = indexer.createIndexingContext("central-context", "central", centralLocalCache, centralIndexDir,
                "https://repo1.maven.org/maven2", null, true, true, indexers);

        // Update the index (incremental update will happen if this is not 1st run and
        // files are not deleted)
        // This whole block below should not be executed on every app start, but rather
        // controlled by some configuration
        // since this block will always emit at least one HTTP GET. Central indexes are
        // updated once a week, but
        // other index sources might have different index publishing frequency.
        // Preferred frequency is once a week.
        logger.info("Updating Index. This might take a while on first run, so please be patient.");
        // Create ResourceFetcher implementation to be used with IndexUpdateRequest
        // Here, we use Wagon based one as shorthand, but all we need is a
        // ResourceFetcher implementation
        TransferListener listener = new AbstractTransferListener() {
            @Override
            public void transferStarted(TransferEvent transferEvent) {
                logger.info("Downloading {}", transferEvent.getResource().getName());
            }

            @Override
            public void transferCompleted(TransferEvent transferEvent) {
                logger.info("Done downloading {}", transferEvent.getResource().getName());
            }
        };
        ResourceFetcher resourceFetcher = new WagonHelper.WagonFetcher(httpWagon, listener, null, null);

        Date centralContextCurrentTimestamp = centralContext.getTimestamp();
        IndexUpdateRequest updateRequest = new IndexUpdateRequest(centralContext, resourceFetcher);
        IndexUpdateResult updateResult = indexUpdater.fetchAndUpdateIndex(updateRequest);
        if (updateResult.isFullUpdate()) {
            logger.info("Full update happened!");
        } else if (updateResult.getTimestamp().equals(centralContextCurrentTimestamp)) {
            logger.info("No update needed, index is up to date!");
        } else {
            logger.info("Incremental update happened, change covered {} - {} period.", centralContextCurrentTimestamp,
                    updateResult.getTimestamp());
        }
    }

    public void querySourceArtifacts(String queryGroupId) throws IOException {

        Query groupId = indexer.constructQuery(MAVEN.GROUP_ID, new SourcedSearchExpression(queryGroupId));
        Query packaging = indexer.constructQuery(MAVEN.PACKAGING, new SourcedSearchExpression("pom"));

        BooleanQuery bq = new BooleanQuery.Builder()
            .add(groupId, Occur.MUST)
            .add(packaging, Occur.MUST_NOT)
            .build();

        searchAndDump(indexer, "all " + queryGroupId + " artifacts", bq);
    }

    private void searchAndDump(Indexer nexusIndexer, String descr, Query q) throws IOException {
        logger.info("Searching for {}", descr);

        GroupedSearchResponse response = nexusIndexer
                .searchGrouped(new GroupedSearchRequest(q, new GAGrouping(), centralContext));

        try (FileOutputStream fos = new FileOutputStream(new File(OUT_DIR, "results.csv"));
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                OutputStreamWriter w = new OutputStreamWriter(bos)) {

            response.getResults().values()
                .stream()
                .map(IndexQuerier::extract)
                .filter(Objects::nonNull)
                .forEach(ai -> writeLine(w, ai));

            logger.info("Total hits: {}", response.getTotalHitsCount());
        }

        logger.info("Data written under {}", OUT_DIR);
    }

    private void writeLine(OutputStreamWriter w, ArtifactInfo aws) {
        try {
            w.write(aws.getGroupId() + ',' + aws.getArtifactId() + ',' + aws.getVersion() + ','
                    + (aws.getSourcesExists() == PRESENT  ? '1' : '0') + '\n');
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
