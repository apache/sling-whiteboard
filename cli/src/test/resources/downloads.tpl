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

// ------------------------------------------------------------------------------------------------
// Sling downloads page
// http://www.apache.org/dev/release-download-pages.html explains how the apache.org mirrored
// downloads page work. Basically, we provide a downloads.html page with a few placeholders
// and a form to select the download mirrog, and a downloads.cgi page which wraps the apache.org
// download logic CGI.
// ------------------------------------------------------------------------------------------------

// ------------------------------------------------------------------------------------------------
// Downloads template data
// The page template itself is found below.
// To convert from the old svn downloads.list ust
//    while read l; do echo "  \"$l\","; done < content/downloads.list
// ------------------------------------------------------------------------------------------------
def launchpadVersion="11"

def slingIDETooling=[
  "Sling IDE Tooling for Eclipse|eclipse|1.2.2|A p2 update site which can be installed in Eclipse.|sling-ide-tooling"
]

def slingApplication=[
  "Sling Starter Standalone|A self-runnable Sling jar|org.apache.sling.starter|.jar|${launchpadVersion}|Y",
  "Sling Starter WAR|A ready-to run Sling webapp as a war file|org.apache.sling.starter|-webapp.war|${launchpadVersion}|Y",
  "Sling Source Release|The released Sling source code|org.apache.sling.starter|-source-release.zip|${launchpadVersion}|Y",
  "Sling CMS App|A reference CMS App built on Apache Sling|org.apache.sling.cms.builder|.jar|0.11.2|org.apache.sling.app.cms",
]

def mavenPlugins=[
  "JSPC Maven Plugin|jspc-maven-plugin|2.1.0|Y",
  "Maven Launchpad Plugin|maven-launchpad-plugin|2.3.4|Y",
  "Sling Maven Plugin|sling-maven-plugin|2.4.0|Y",
  "Slingstart Maven Plugin|slingstart-maven-plugin|1.8.2|Y",
  "HTL Maven Plugin|htl-maven-plugin|1.2.2-1.4.0|Y",
]

def bundles=[
  "Adapter|org.apache.sling.adapter|2.1.10|Y|jar",
  "Adapter Annotations|adapter-annotations|1.0.0|Y|jar",
  "API|org.apache.sling.api|2.20.0|Y|jar",
  "Auth Core|org.apache.sling.auth.core|1.4.2|Y|jar",
  "Auth Form|org.apache.sling.auth.form|1.0.12|Y|jar",
  "Authentication XING API|org.apache.sling.auth.xing.api|0.0.2|Y|jar",
  "Authentication XING Login|org.apache.sling.auth.xing.login|0.0.2|Y|jar",
  "Authentication XING OAuth|org.apache.sling.auth.xing.oauth|0.0.2|Y|jar",
  "Bundle Resource Provider|org.apache.sling.bundleresource.impl|2.3.2|Y|jar",
  "Capabilities|org.apache.sling.capabilities|0.1.2|Y|jar",
  "Capabilities JCR|org.apache.sling.capabilities.jcr|0.1.2|Y|jar",
  "Clam|org.apache.sling.clam|1.0.2|Y|jar",   
  "Classloader Leak Detector|org.apache.sling.extensions.classloader-leak-detector|1.0.0|Y|jar",
  "CMS App API|org.apache.sling.cms.api|0.11.2|org.apache.sling.app.cms|jar",
  "CMS App Core|org.apache.sling.cms.core|0.11.2|org.apache.sling.app.cms|jar",
  "CMS App Reference|org.apache.sling.cms.reference|0.11.2|org.apache.sling.app.cms|jar",
  "CMS App UI|org.apache.sling.cms.ui|0.11.2|org.apache.sling.app.cms|jar",
  "Commons Classloader|org.apache.sling.commons.classloader|1.4.4|Y|jar",
  "Commons Clam|org.apache.sling.commons.clam|1.0.2|Y|jar",
  "Commons Compiler|org.apache.sling.commons.compiler|2.3.6|Y|jar",
  "Commons FileSystem ClassLoader|org.apache.sling.commons.fsclassloader|1.0.8|Y|jar",
  "Commons HTML|org.apache.sling.commons.html|1.1.0|Y|jar",
  "Commons Johnzon|org.apache.sling.commons.johnzon|1.1.0|Y|jar",
  "Commons Log|org.apache.sling.commons.log|5.1.10|Y|jar",
  "Commons Log WebConsole Plugin|org.apache.sling.commons.log.webconsole|1.0.0|Y|jar",
  "Commons Log Service|org.apache.sling.commons.logservice|1.0.6|Y|jar",
  "Commons Metrics|org.apache.sling.commons.metrics|1.2.6|Y|jar",
  "Commons RRD4J metrics reporter|org.apache.sling.commons.metrics-rrd4j|1.0.2|Y|jar",
  "Commons Mime Type Service|org.apache.sling.commons.mime|2.2.0|Y|jar",
  "Commons OSGi|org.apache.sling.commons.osgi|2.4.0|Y|jar",
  "Commons Scheduler|org.apache.sling.commons.scheduler|2.7.2|Y|jar",
  "Commons Testing|org.apache.sling.commons.testing|2.1.2|Y|jar",
  "Commons Threads|org.apache.sling.commons.threads|3.2.18|Y|jar",
  "Content Detection Support|org.apache.sling.commons.contentdetection|1.0.2|Y|jar",
  "Context-Aware Configuration API|org.apache.sling.caconfig.api|1.1.2|Y|jar",
  "Context-Aware Configuration bnd Plugin|org.apache.sling.caconfig.bnd-plugin|1.0.2|Y|jar",
  "Context-Aware Configuration Impl|org.apache.sling.caconfig.impl|1.4.14|Y|jar",
  "Context-Aware Configuration Mock Plugin|org.apache.sling.testing.caconfig-mock-plugin|1.3.2|Y|jar",
  "Context-Aware Configuration SPI|org.apache.sling.caconfig.spi|1.3.4|Y|jar",
  "Crankstart API|org.apache.sling.crankstart.api|1.0.0|N|jar",
  "Crankstart API Fragment|org.apache.sling.crankstart.api.fragment|1.0.2|N|jar",
  "Crankstart Core|org.apache.sling.crankstart.core|1.0.0|N|jar",
  "Crankstart Launcher|org.apache.sling.crankstart.launcher|1.0.0|Y|jar",
  "Crankstart Launcher Sling Extensions|org.apache.sling.crankstart.sling.extensions|1.0.0|Y|jar",
  "Crankstart Launcher Test Services|org.apache.sling.crankstart.test.services|1.0.0|Y|jar",
  "DataSource Provider|org.apache.sling.datasource|1.0.4|Y|jar",
  "Discovery API|org.apache.sling.discovery.api|1.0.4|Y|jar",
  "Discovery Impl|org.apache.sling.discovery.impl|1.2.12|Y|jar",
  "Discovery Commons|org.apache.sling.discovery.commons|1.0.20|Y|jar",
  "Discovery Base|org.apache.sling.discovery.base|2.0.8|Y|jar",
  "Discovery Oak|org.apache.sling.discovery.oak|1.2.28|Y|jar",
  "Discovery Standalone|org.apache.sling.discovery.standalone|1.0.2|Y|jar",
  "Discovery Support|org.apache.sling.discovery.support|1.0.4|Y|jar",
  "Distributed Event Admin|org.apache.sling.event.dea|1.1.4|Y|jar",
  "Distribution API|org.apache.sling.distribution.api|0.3.0|Y|jar",
  "Distribution Core|org.apache.sling.distribution.core|0.4.0|Y|jar",
  "Distribution Integration Tests|org.apache.sling.distribution.it|0.1.2|Y|jar",
  "Distribution Sample|org.apache.sling.distribution.sample|0.1.6|Y|jar",
  "Dynamic Include|org.apache.sling.dynamic-include|3.1.2|Y|jar",
  "Engine|org.apache.sling.engine|2.6.18|Y|jar",
  "Event|org.apache.sling.event|4.2.12|Y|jar",
  "Event API|org.apache.sling.event.api|1.0.0|Y|jar",
  "Feature Model|org.apache.sling.feature|1.0.0|Y|jar",
  "Feature Model Analyser|org.apache.sling.feature.analyser|0.8.0|Y|jar",
  "Feature Model IO|org.apache.sling.feature.io|1.0.0|Y|jar",
  "Feature Model Converter|org.apache.sling.feature.modelconverter|0.8.0|Y|jar",
  "Feature Flags|org.apache.sling.featureflags|1.2.2|Y|jar",
  "File Optimization|org.apache.sling.fileoptim|0.9.2|org.apache.sling.file.optimization|jar",
  "File System Resource Provider|org.apache.sling.fsresource|2.1.14|Y|jar",
  "I18n|org.apache.sling.i18n|2.5.12|Y|jar",
  "HApi|org.apache.sling.hapi|1.1.0|Y|jar",
  "Health Check Annotations|org.apache.sling.hc.annotations|1.0.6|Y|jar",
  "Health Check Core|org.apache.sling.hc.core|1.2.10|Y|jar",
  "Health Check API|org.apache.sling.hc.api|1.0.2|Y|jar",
  "Health Check Integration Tests|org.apache.sling.hc.it|1.0.4|Y|jar",
  "Health Check JUnit Bridge|org.apache.sling.hc.junit.bridge|1.0.2|Y|jar",
  "Health Check Samples|org.apache.sling.hc.samples|1.0.6|Y|jar",
  "Health Check Support|org.apache.sling.hc.support|1.0.4|Y|jar",
  "Health Check Webconsole|org.apache.sling.hc.webconsole|1.1.2|Y|jar",
  "Installer Core|org.apache.sling.installer.core|3.9.0|Y|jar",
  "Installer Console|org.apache.sling.installer.console|1.0.2|Y|jar",
  "Installer Configuration Support|org.apache.sling.installer.factory.configuration|1.2.0|Y|jar",
  "Installer Health Checks|org.apache.sling.installer.hc|2.0.0|Y|jar",
  "Installer Subystems Support|org.apache.sling.installer.factory.subsystems|1.0.0|Y|jar",
  "Installer File Provider|org.apache.sling.installer.provider.file|1.1.0|Y|jar",
  "Installer JCR Provider|org.apache.sling.installer.provider.jcr|3.1.26|Y|jar",
  "Installer Vault Package Install Hook|org.apache.sling.installer.provider.installhook|1.0.4|Y|jar",
  "javax activation|org.apache.sling.javax.activation|0.1.0|Y|jar",
  "JCR API|org.apache.sling.jcr.api|2.4.0|Y|jar",
  "JCR API Wrapper|org.apache.sling.jcr.jcr-wrapper|2.0.0|Y|jar",
  "JCR Base|org.apache.sling.jcr.base|3.0.6|Y|jar",
  "JCR ClassLoader|org.apache.sling.jcr.classloader|3.2.4|Y|jar",
  "JCR Content Loader|org.apache.sling.jcr.contentloader|2.3.0|Y|jar",
  "JCR Content Parser|org.apache.sling.jcr.contentparser|1.2.6|Y|jar",
  "JCR DavEx|org.apache.sling.jcr.davex|1.3.10|Y|jar",
  "JCR Jackrabbit AccessManager|org.apache.sling.jcr.jackrabbit.accessmanager|3.0.4|Y|jar",
  "JCR Jackrabbit UserManager|org.apache.sling.jcr.jackrabbit.usermanager|2.2.8|Y|jar",
  "JCR Oak Server|org.apache.sling.jcr.oak.server|1.2.2|Y|jar",
  "JCR Registration|org.apache.sling.jcr.registration|1.0.6|Y|jar",
  "JCR Repoinit|org.apache.sling.jcr.repoinit|1.1.8|Y|jar",
  "JCR Resource|org.apache.sling.jcr.resource|3.0.18|Y|jar",
  "JCR Resource Security|org.apache.sling.jcr.resourcesecurity|1.0.2|Y|jar",
  "JCR Web Console Plugin|org.apache.sling.jcr.webconsole|1.0.2|Y|jar",
  "JMX Resource Provider|org.apache.sling.jmx.provider|1.0.2|Y|jar",
  "JCR WebDAV|org.apache.sling.jcr.webdav|2.3.8|Y|jar",
  "JUnit Core|org.apache.sling.junit.core|1.0.26|Y|jar",
  "JUnit Remote Tests Runners|org.apache.sling.junit.remote|1.0.12|Y|jar",
  "JUnit Scriptable Tests Provider|org.apache.sling.junit.scriptable|1.0.12|Y|jar",
  "JUnit Tests Teleporter|org.apache.sling.junit.teleporter|1.0.18|Y|jar",
  "JUnit Health Checks|org.apache.sling.junit.healthcheck|1.0.6|Y|jar",
  "Launchpad API|org.apache.sling.launchpad.api|1.2.0|Y|jar",
  "Launchpad Base|org.apache.sling.launchpad.base|5.6.10-2.6.26|Y|jar",
  "Launchpad Base - Application Launcher|org.apache.sling.launchpad.base|5.6.10-2.6.26|Y|war",
  "Launchpad Base - Web Launcher|org.apache.sling.launchpad.base|5.6.10-2.6.26|Y|war",
  "Launchpad Installer|org.apache.sling.launchpad.installer|1.2.2|Y|jar",
  "Launchpad Integration Tests|org.apache.sling.launchpad.integration-tests|1.0.8|Y|jar",
  "Launchpad Test Fragment Bundle|org.apache.sling.launchpad.test-fragment|2.0.16|Y|jar",
  "Launchpad Test Bundles|org.apache.sling.launchpad.test-bundles|0.0.6|Y|jar",
  "Launchpad Testing|org.apache.sling.launchpad.testing|11|Y|jar",
  "Launchpad Testing WAR|org.apache.sling.launchpad.testing-war|11|Y|jar",
  "Launchpad Testing Services|org.apache.sling.launchpad.test-services|2.0.16|Y|jar",
  "Launchpad Testing Services WAR|org.apache.sling.launchpad.test-services-war|2.0.16|Y|war",
  "Log Tracer|org.apache.sling.tracer|1.0.6|Y|jar",
  "Models API|org.apache.sling.models.api|1.3.8|Y|jar",
  "Models bnd Plugin|org.apache.sling.bnd.models|1.0.0|Y|jar",
  "Models Implementation|org.apache.sling.models.impl|1.4.10|Y|jar",
  "Models Jackson Exporter|org.apache.sling.models.jacksonexporter|1.0.8|Y|jar",
  "NoSQL Generic Resource Provider|org.apache.sling.nosql.generic|1.1.0|Y|jar",
  "NoSQL Couchbase Client|org.apache.sling.nosql.couchbase-client|1.0.2|Y|jar",
  "NoSQL Couchbase Resource Provider|org.apache.sling.nosql.couchbase-resourceprovider|1.1.0|Y|jar",
  "NoSQL MongoDB Resource Provider|org.apache.sling.nosql.mongodb-resourceprovider|1.1.0|Y|jar",
  "Oak Restrictions|org.apache.sling.oak.restrictions|1.0.2|Y|jar",
  "Pax Exam Utilities|org.apache.sling.paxexam.util|1.0.4|Y|jar",
  "Performance Test Utilities|org.apache.sling.performance.base|1.0.2|org.apache.sling.performance|jar",
  "Pipes|org.apache.sling.pipes|3.1.0|Y|jar",
  "Provisioning Model|org.apache.sling.provisioning.model|1.8.4|Y|jar",
  "Repoinit Parser|org.apache.sling.repoinit.parser|1.2.2|Y|jar",
  "Resource Access Security|org.apache.sling.resourceaccesssecurity|1.0.0|Y|jar",
  "Resource Builder|org.apache.sling.resourcebuilder|1.0.4|Y|jar",
  "Resource Collection|org.apache.sling.resourcecollection|1.0.2|Y|jar",
  "Resource Filter|org.apache.sling.resource.filter|1.0.0|Y|jar",
  "Resource Inventory|org.apache.sling.resource.inventory|1.0.8|Y|jar",
  "Resource Merger|org.apache.sling.resourcemerger|1.3.8|Y|jar",
  "Resource Presence|org.apache.sling.resource.presence|0.0.2|Y|jar",
  "Resource Resolver|org.apache.sling.resourceresolver|1.6.6|Y|jar",
  "Rewriter|org.apache.sling.rewriter|1.2.2|Y|jar",
  "Failing Server-Side Tests|org.apache.sling.testing.samples.failingtests|1.0.6|N|jar",
  "Sample Integration Tests|org.apache.sling.testing.samples.integrationtests|1.0.6|N|jar",
  "Sample Server-Side Tests|org.apache.sling.testing.samples.sampletests|1.0.6|N|jar",
  "Scripting API|org.apache.sling.scripting.api|2.2.0|Y|jar",
  "Scripting Console|org.apache.sling.scripting.console|1.0.0|Y|jar",
  "Scripting Core|org.apache.sling.scripting.core|2.0.56|Y|jar",
  "Scripting EL API Wrapper|org.apache.sling.scripting.el-api|1.0.0|Y|jar",
  "Scripting Java|org.apache.sling.scripting.java|2.1.2|Y|jar",
  "Scripting JavaScript|org.apache.sling.scripting.javascript|3.0.4|Y|jar",
  "Scripting JSP|org.apache.sling.scripting.jsp|2.3.4|Y|jar",
  "Scripting JSP API Wrapper|org.apache.sling.scripting.jsp-api|1.0.0|Y|jar",
  "Scripting JSP Taglib|org.apache.sling.scripting.jsp.taglib|2.4.0|Y|jar",
  "Scripting Groovy|org.apache.sling.scripting.freemarker|1.0.0|Y|jar",
  "Scripting Groovy|org.apache.sling.scripting.groovy|1.0.4|Y|jar",
  "Scripting HTL Runtime|org.apache.sling.scripting.sightly.runtime|1.1.0-1.4.0|Y|jar",
  "Scripting HTL Compiler|org.apache.sling.scripting.sightly.compiler|1.1.2-1.4.0|Y|jar",
  "Scripting HTL Java Compiler|org.apache.sling.scripting.sightly.compiler.java|1.1.2-1.4.0|Y|jar",
  "Scripting HTL Engine|org.apache.sling.scripting.sightly|1.1.2-1.4.0|Y|jar",
  "Scripting HTL JavaScript Use Provider|org.apache.sling.scripting.sightly.js.provider|1.0.28|Y|jar",
  "Scripting HTL Sling Models Use Provider|org.apache.sling.scripting.sightly.models.provider|1.0.8|Y|jar",
  "Scripting HTL REPL|org.apache.sling.scripting.sightly.repl|1.0.6|Y|jar",
  "Scripting Thymeleaf|org.apache.sling.scripting.thymeleaf|2.0.0|Y|jar",
  "Security|org.apache.sling.security|1.1.16|Y|jar",
  "Service User Mapper|org.apache.sling.serviceusermapper|1.4.2|Y|jar",
  "Service User WebConsole|org.apache.sling.serviceuser.webconsole|1.0.0|Y|jar",
  "Servlet Annotations|org.apache.sling.servlets.annotations|1.2.4|Y|jar",
  "Servlet Helpers|org.apache.sling.servlet-helpers|1.1.8|Y|jar",
  "Servlets Get|org.apache.sling.servlets.get|2.1.40|Y|jar",
  "Servlets Post|org.apache.sling.servlets.post|2.3.28|Y|jar",
  "Servlets Resolver|org.apache.sling.servlets.resolver|2.5.2|Y|jar",
  "Settings|org.apache.sling.settings|1.3.10|Y|jar",
  "Slf4j MDC Filter|org.apache.sling.extensions.slf4j.mdc|1.0.0|Y|jar",
  "Sling Query|org.apache.sling.query|4.0.2|Y|jar",
  "Starter Content|org.apache.sling.starter.content|1.0.2|Y|jar",
  "Starter Startup|org.apache.sling.starter.startup|1.0.6|Y|jar",
  "Superimposing Resource Provider|org.apache.sling.superimposing|0.2.0|Y|jar",
  "System Bundle Extension: Activation API|org.apache.sling.fragment.activation|1.0.2|Y|jar",
  "System Bundle Extension: WS APIs|org.apache.sling.fragment.ws|1.0.2|Y|jar",
  "System Bundle Extension: XML APIs|org.apache.sling.fragment.xml|1.0.2|Y|jar",
  "Tenant|org.apache.sling.tenant|1.1.4|Y|jar",
  "Testing Clients|org.apache.sling.testing.clients|1.2.0|Y|jar",
  "Testing Email|org.apache.sling.testing.email|1.0.0|Y|jar",
  "Testing Hamcrest|org.apache.sling.testing.hamcrest|1.0.2|Y|jar",
  "Testing JCR Mock|org.apache.sling.testing.jcr-mock|1.4.2|Y|jar",
  "Testing Logging Mock|org.apache.sling.testing.logging-mock|2.0.0|Y|jar",
  "Testing OSGi Mock Core|org.apache.sling.testing.osgi-mock.core|2.4.6|org.apache.sling.testing.osgi-mock|jar",
  "Testing OSGi Mock JUnit 4|org.apache.sling.testing.osgi-mock.junit4|2.4.6|org.apache.sling.testing.osgi-mock|jar",
  "Testing OSGi Mock JUnit 5|org.apache.sling.testing.osgi-mock.junit5|2.4.6|org.apache.sling.testing.osgi-mock|jar",
  "Testing PaxExam|org.apache.sling.testing.paxexam|2.0.0|Y|jar",
  "Testing Rules|org.apache.sling.testing.rules|1.0.8|Y|jar",
  "Testing Resource Resolver Mock|org.apache.sling.testing.resourceresolver-mock|1.1.22|Y|jar",
  "Testing Server Setup Tools|org.apache.sling.testing.serversetup|1.0.1|Y|jar",
  "Testing Sling Mock Core|org.apache.sling.testing.sling-mock.core|2.3.4|org.apache.sling.testing.sling-mock|jar",
  "Testing Sling Mock JUnit 4|org.apache.sling.testing.sling-mock.junit4|2.3.4|org.apache.sling.testing.sling-mock|jar",
  "Testing Sling Mock JUnit 5|org.apache.sling.testing.sling-mock.junit5|2.3.4|org.apache.sling.testing.sling-mock|jar",
  "Testing Sling Mock Oak|org.apache.sling.testing.sling-mock-oak|2.1.2|Y|jar",
  "Tooling Support Install|org.apache.sling.tooling.support.install|1.0.4|Y|jar",
  "Tooling Support Source|org.apache.sling.tooling.support.source|1.0.4|Y|jar",
  "URL Rewriter|org.apache.sling.urlrewriter|0.0.2|Y|jar",
  "Validation API|org.apache.sling.validation.api|1.0.0|Y|jar",
  "Validation Core|org.apache.sling.validation.core|1.0.4|Y|jar",
  "Web Console Branding|org.apache.sling.extensions.webconsolebranding|1.0.2|Y|jar",
  "Web Console Security Provider|org.apache.sling.extensions.webconsolesecurityprovider|1.2.0|Y|jar",
  "XSS Protection|org.apache.sling.xss|2.1.0|Y|jar",
  "XSS Protection Compat|org.apache.sling.xss.compat|1.1.0|N|jar"
]
                                                                      
def deprecated=[
  "Auth OpenID|Not Maintained|org.apache.sling.auth.openid|1.0.4",
  "Auth Selector|Not Maintained|org.apache.sling.auth.selector|1.0.6",
  "Background Servlets Engine|Not Maintained|org.apache.sling.bgservlets|1.0.8",
  "Background Servlets Integration Test|Not Maintained|org.apache.sling.bgservlets.testing|1.0.0",
  "Commons JSON|Replaced with Commons Johnzon|org.apache.sling.commons.json|2.0.20",
  "Explorer|Replaced with Composum|org.apache.sling.extensions.explorer|1.0.4",
  "GWT Integration|Not Maintained|org.apache.sling.extensions.gwt.servlet|3.0.0",
  "JCR Compiler|Replaced with FS ClassLoader|org.apache.sling.jcr.compiler|2.1.0",
  "JCR Jackrabbit Server|Replaced with Apache Jackrabbit Oak|org.apache.sling.jcr.jackrabbit.server|2.3.0",
  "JCR Prefs|Replaced with CA Configs|org.apache.sling.jcr.prefs|1.0.0",
  "Karaf repoinit|Removed|org.apache.sling.karaf-repoinit|0.2.0",
  "Launchpad Content|Replaced with Starter Content|org.apache.sling.launchpad.content|2.0.12",
  "Path-based RTP sample|Not Maintained|org.apache.sling.samples.path-based.rtp|2.0.4",
  "Scripting JSP Taglib Compat|Superseded by the XSS API bundle|org.apache.sling.scripting.jsp.taglib.compat|1.0.0",
  "Scripting JST|Not Maintained|org.apache.sling.scripting.jst|2.0.6",
  "Servlets Compat|Not Maintained|org.apache.sling.servlets.compat|1.0.2",
  "Testing Sling Mock Jackrabbit|Not Maintained|org.apache.sling.testing.sling-mock-jackrabbit|1.0.0",
  "Testing Tools|SLING-5703|org.apache.sling.testing.tools|1.0.16",
  "Thread Dumper|Replaced with Apache Felix Thread Dumper|org.apache.sling.extensions.threaddump|0.2.2"
]

// ------------------------------------------------------------------------------------------------
// Utilities
// ------------------------------------------------------------------------------------------------
def downloadLink(label, artifact, version, suffix) {
	def sep = version ? "-" : ""
	def path = "sling/${artifact}${sep}${version}${suffix}"
	def digestsBase = "https://www.apache.org/dist/${path}"

	a(href:"[preferred]${path}", label)
	yield " ("
	a(href:"${digestsBase}.asc", "asc")
	yield ", "
	a(href:"${digestsBase}.sha1", "sha1")
	yield ")"
	newLine()
}

def githubLink(artifact,ghflag) {
	if(ghflag == 'Y') {
		artifact = artifact.replaceAll('\\.','-')
    def url = "https://github.com/apache/sling-${artifact}"
    // remove duplicate sling- prefix
    url = url.replaceAll('sling-sling-','sling-')
		a(href:url, "GitHub")
		newLine()
	} else if (ghflag != 'N') {
		artifact = ghflag.replaceAll('\\.','-')
    // remove duplicate sling- prefix
    def url = "https://github.com/apache/sling-${artifact}"
    url = url.replaceAll('sling-sling-','sling-')
		a(href:url, "GitHub")
		newLine()
	} else {
		yield "N/A"
	}
}

def tableHead(String [] headers) {
	thead() {
		tr() {
			headers.each { header ->
				th(header)
			}
		}
	}

}

 // ------------------------------------------------------------------------------------------------
// Downloads page layout
// ------------------------------------------------------------------------------------------------
layout 'layout/main.tpl', true,
        projects: projects,
        tags : contents {
            include template: 'tags-brick.tpl'
        },
        lastModified: contents {
            include template : 'lastmodified-brick.tpl'
        },
        bodyContents: contents {

            div(class:"row"){
                div(class:"small-12 columns"){
                    section(class:"wrap"){
                        yieldUnescaped content.body

						h2("Sling Application")
						table(class:"table") {
							tableHead("Artifact", "Version", "GitHub", "Provides", "Package")
							tbody() {
								slingApplication.each { line ->
									tr() {
										def data = line.split("\\|")
										td(data[0])
										td(data[4])
										td(){
											githubLink(data[2], data[5])
										}
										td(data[1])
										def artifact = "${data[2]}-${data[4]}${data[3]}"
										td(){
											downloadLink(artifact, artifact, "", "")
										}
									}
								}
							}
						}

						h2("Sling IDE Tooling")
						table(class:"table") {
							tableHead("Artifact", "Version", "Provides", "Update Site")
							tbody() {
								slingIDETooling.each { line ->
									tr() {
										def data = line.split("\\|")
										td(data[0])
										td(data[2])
										td(data[3])
										def artifact = "${data[1]}/${data[2]}"
										td(){
											downloadLink("Update site", artifact, "", "")
										}
									}
								}
							}
						}

						h2("Sling Components")
						table(class:"table") {
							tableHead("Artifact", "Version", "GitHub", "Binary", "Source")
							tbody() {
								bundles.each { line ->
									tr() {
										def data = line.split("\\|")
										td(data[0])
										td(data[2])
										def artifact = data[1]
										def version = data[2]
										def ghflag = data[3]
										def extension = data[4]
										td(){
											githubLink(artifact,ghflag)
										}
										td(){
											downloadLink("Bundle", artifact, version, "." + extension)
										}
										td(){
											downloadLink("Source ZIP", artifact, version, "-source-release.zip")
										}
									}
								}
							}
						}

						h2("Maven Plugins")
						table(class:"table") {
							tableHead("Artifact", "Version", "GitHub", "Binary", "Source")
							tbody() {
								mavenPlugins.each { line ->
									tr() {
										def data = line.split("\\|")
										td(data[0])
										td(data[2])
										def artifact = data[1]
										def version = data[2]
										def ghflag = data[3]
										td(){
											githubLink(artifact, ghflag)
										}
										td(){
											downloadLink("Maven Plugin", artifact, version, ".jar")
										}
										td(){
											downloadLink("Source ZIP", artifact, version, "-source-release.zip")
										}
									}
								}
							}
						}
    
						h2("Deprecated")
						table(class:"table") {
							tableHead("Artifact", "Replacement", "Version", "Binary", "Source")
							tbody() {
								deprecated.each { line ->
									tr() {
										def data = line.split("\\|")
										td(data[0])
										td(data[1])
										td(data[3])
										def artifact = data[2]
										def version = data[3]
										td(){
											downloadLink("Bundle", artifact, version, ".jar")
										}
										td(){
											downloadLink("Source ZIP", artifact, version, "-source-release.zip")
										}
									}
								}
							}
						}
                    }
                }
            }
        }
