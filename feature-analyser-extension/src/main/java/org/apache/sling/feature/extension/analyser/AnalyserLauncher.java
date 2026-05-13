/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.feature.extension.analyser;


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.analyser.Analyser;
import org.apache.sling.feature.analyser.AnalyserResult;
import org.apache.sling.feature.analyser.task.AnalyserTask;
import org.apache.sling.feature.analyser.task.impl.CheckApiRegions;
import org.apache.sling.feature.analyser.task.impl.CheckApiRegionsDependencies;
import org.apache.sling.feature.analyser.task.impl.CheckApiRegionsDuplicates;
import org.apache.sling.feature.analyser.task.impl.CheckApiRegionsOrder;
import org.apache.sling.feature.analyser.task.impl.CheckBundleExportsImports;
import org.apache.sling.feature.analyser.task.impl.CheckBundlesForResources;
import org.apache.sling.feature.analyser.task.impl.CheckRequirementsCapabilities;
import org.apache.sling.feature.builder.ArtifactProvider;
import org.apache.sling.feature.io.artifacts.ArtifactManager;
import org.apache.sling.feature.io.artifacts.ArtifactManagerConfig;
import org.apache.sling.feature.launcher.spi.Launcher;
import org.apache.sling.feature.launcher.spi.LauncherPrepareContext;
import org.apache.sling.feature.launcher.spi.LauncherRunContext;
import org.apache.sling.feature.scanner.Scanner;
import org.osgi.framework.FrameworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AnalyserLauncher implements Launcher {


    private Runnable analyserRunner;
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void prepare(final LauncherPrepareContext context,
            final ArtifactId frameworkId,
            final Feature app) throws Exception {
        
        final ArtifactManager am = ArtifactManager.getArtifactManager(new ArtifactManagerConfig());
        final Scanner scanner = new Scanner(new ArtifactProvider(){

            @Override
            public URL provide(ArtifactId id) {
                try {
                    return am.getArtifactHandler(id.toMvnUrl()).getLocalURL();
                } catch (final IOException e) {
                    return null;
                }
            }
        });
       
        this.analyserRunner = new Runnable() {
            
            @SuppressWarnings("serial")
            @Override
            public void run() {
                 try {
                     Map<String, Map<String,String>> analyserTaskConfigs = new HashMap<>();
                     List<AnalyserTask> analyserTasks = new ArrayList<AnalyserTask>();
                     
                     analyserTasks.add(new CheckApiRegions());
                     String regionsorder = System.getProperty("regionsorder");
                     if (StringUtils.isNotEmpty(regionsorder)) {
	                     CheckApiRegionsOrder caro = new CheckApiRegionsOrder();
	                     analyserTaskConfigs.put(caro.getId(), new HashMap<String, String>(){{
	                         put("order", System.getProperty("regionsorder"));
	                     }});
	                     analyserTasks.add(caro);
                     }
                     CheckApiRegionsDependencies cardep = new CheckApiRegionsDependencies();
                     String location = System.getProperty("sling.feature.apiregions.resource.bundles.properties");
                     if(StringUtils.isNotEmpty(location)) {
	                     File bundleprosrc = new File(location);
	                     Path original = bundleprosrc.toPath();
	                     Path target = Paths.get("fmtemp/group_assembled_1.0.0/regionOrigins.properties");
	                     Files.createDirectories(target.getParent());
	                     Files.copy(original, target, StandardCopyOption.REPLACE_EXISTING); 
	                     
	                     analyserTaskConfigs.put("all", new HashMap<String, String>(){{
	                         put("fileStorage", target.getParent().getParent().toAbsolutePath().toString());
	                     }});
	                     analyserTasks.add(cardep);
                     }
                     analyserTasks.add(new CheckApiRegionsDuplicates());
                     analyserTasks.add(new CheckBundleExportsImports());
                     analyserTasks.add(new CheckRequirementsCapabilities());
                     analyserTasks.add(new CheckBundlesForResources());
                     
                     
	                 Analyser analyser  = new Analyser(scanner, analyserTaskConfigs, analyserTasks.toArray(new AnalyserTask[analyserTasks.size()]));
                     
                     
                    final AnalyserResult result = analyser.analyse(app, frameworkId);
                    for (final String msg : result.getWarnings()) {
                        context.getLogger().warn(msg);  
                    }
                    for (final String msg : result.getErrors()) {
                    	context.getLogger().error(msg);
                    }

                    if (!result.getErrors().isEmpty()) {
                    	context.getLogger().error("Analyser detected errors. See log output for error messages.");
                        System.exit(1);
                    }
                
                } catch (Exception e) {
                	context.getLogger().error(e.getMessage());
                    System.exit(1);
                }
            }
        };
    }
    

    /**
     * Run the launcher
     * @throws Exception If anything goes wrong
     */
    @Override
    public int run(final LauncherRunContext context, final ClassLoader cl) throws Exception {
        try {
          analyserRunner.run();
        } catch ( final Exception e) {
            System.exit(1);
        }
        return FrameworkEvent.STOPPED;
    }

}