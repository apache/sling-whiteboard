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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.feature.ArtifactId;
import org.apache.sling.feature.Feature;
import org.apache.sling.feature.MatchingRequirement;
import org.apache.sling.feature.launcher.spi.LauncherPrepareContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.mockito.Mockito;
import org.osgi.framework.Constants;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyserLauncherTest {
	
	@Rule
	public final ExpectedSystemExit exit = ExpectedSystemExit.none();
	
	@Test
	public void testSuccessOnWarn() throws Exception {
		

		AnalyserLauncher al = new AnalyserLauncher();
		LauncherPrepareContext context = Mockito.mock(LauncherPrepareContext.class);
		Logger logger = spy(LoggerFactory.getLogger(AnalyserLauncher.class));
		when(context.getLogger()).thenReturn(logger);
		Feature app = new Feature(ArtifactId.fromMvnId("a:b:1"));
		
		@SuppressWarnings("serial")
		Map<String, String> directives = new HashMap<String,String>() {{
	        put(Constants.RESOLUTION_DIRECTIVE, Constants.RESOLUTION_OPTIONAL);
		}};
		addUnsatisfiedRequirement(directives, app);
		
		al.prepare(context, null, app);
		al.run(null, null);
		verify(logger).warn(any());
	}

	@Test
	public void testSystemExitOnError() throws Exception {

		exit.expectSystemExitWithStatus(1);
		AnalyserLauncher al = new AnalyserLauncher();
		LauncherPrepareContext context = Mockito.mock(LauncherPrepareContext.class);
		Logger logger = spy(LoggerFactory.getLogger(AnalyserLauncher.class));
		when(context.getLogger()).thenReturn(logger);
		Feature app = new Feature(ArtifactId.fromMvnId("a:b:1"));
		
		@SuppressWarnings("serial")
		Map<String, String> directives = new HashMap<String,String>() {{
	        put(Constants.RESOLUTION_DIRECTIVE, Constants.RESOLUTION_MANDATORY);
		}};
		addUnsatisfiedRequirement(directives, app);
		
		al.prepare(context, null, app);
		al.run(null, null);
		verify(logger).error(any());
	}


	private void addUnsatisfiedRequirement(Map<String, String> directives, Feature app) {
		MatchingRequirement req = new MatchingRequirement () {
			
			@Override
			public Resource getResource() {
				return null;
			}
			
			@Override
			public String getNamespace() {
				return null;
			}
			
			@Override
			public Map<String, String> getDirectives() {
				return directives;
			}
			
			@Override
			public Map<String, Object> getAttributes() {
				return null;
			}
			
			@Override
			public boolean matches(Capability cap) {
				return false;
			}
		};
				 
		app.getRequirements().add(req);
	}

}
