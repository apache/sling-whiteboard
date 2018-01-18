/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package org.apache.tomcat.buildutil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPConstants;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Base64;
import org.codehaus.plexus.util.Scanner;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

// file copied and adapted from http://svn.apache.org/viewvc/tomcat/trunk/java/org/apache/tomcat/buildutil/SignCode.java?revision=1789744&view=co
/**
 * The <tt>SignMojo</tt> executs a signing operation against the Symantec Secure App Service
 * 
 * <p>It uses the SOAP API to send the artifacts for signing and download them.</p>
 * 
 * <p>The recommended usage of the plugin is to define the sensitive parameters in a profile
 * in the <tt>settings.xml</tt> file:
 * 
 * <ol>
 *   <li>codesign.userName</li>
 *   <li>codesign.password</li>
 *   <li>codesign.partnerCode</li>
 *   <li>codesign.keyStore</li>
 *   <li>codesign.keyStorePassword</li>
 * </ol>
 * </p>
 * 
 * <p>Following that, the plugin configuration can be done in the pom.xml file for non-sensitive parameters.</p>
 * 
 */
@Mojo(
        name = "sign",
        defaultPhase = LifecyclePhase.PACKAGE,
        threadSafe = true
)
public class SignCodeMojo extends AbstractMojo {

    private static final URL SIGNING_SERVICE_URL;

    private static final String NS = "cod";

    private static final MessageFactory SOAP_MSG_FACTORY;

    static {
        try {
            SIGNING_SERVICE_URL = new URL(
                    "https://api-appsec-cws.ws.symantec.com/webtrust/SigningService");
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
        try {
            SOAP_MSG_FACTORY = MessageFactory.newInstance(SOAPConstants.SOAP_1_1_PROTOCOL);
        } catch (SOAPException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    @Component
    private BuildContext buildContext;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;    

    /**
     * The username of the API user
     */
    @Parameter(required = true, defaultValue="${codesign.userName}")
    private String userName;
    
    /**
     * The password of the API user
     */
    @Parameter(required = true, defaultValue="${codesign.password}")
    private String password;
    
    /**
     * The partner code, initially sent via an email to you titled 'Your Secure App Service API username'
     */
    @Parameter(required = true, defaultValue="${codesign.partnerCode}")
    private String partnerCode;
    
    @Parameter(defaultValue = "${project.name}")
    private String applicationName;
    
    @Parameter(defaultValue = "${project.version}")
    private String applicationVersion;

    @Parameter(required = true, defaultValue="${codesign.keyStore}")
    private String keyStore;
    
    @Parameter(required = true, defaultValue="${codesign.keyStorePassword}")
    private String keyStorePassword;
    
    /**
     * Allows definition of additional artifacts to sign
     */
    @Parameter
    private FileSet[] artifactSets;

    /**
     * When set to true the project's primary artifact will be added to the list of files to sign
     */
    @Parameter
    private boolean includeProjectArtifact;
    
    @Parameter(property="codesign.sslDebug")
    private boolean sslDebug;
    
    /**
     * Use <tt>Java TEST Signing Sha256</tt> for testing and <tt>Java Signing Sha256</tt> for prod 
     */
    @Parameter(required = true, defaultValue="${codesign.signingService}")
    private String signingService;


    @Override
    public void execute() throws MojoExecutionException {
    	
    	List<File> filesToSign = new ArrayList<>();
    	
    	if ( includeProjectArtifact )
    		filesToSign.add(project.getArtifact().getFile());
    	
    	if ( artifactSets != null ) {
    		for ( FileSet artifactSet : artifactSets ) {
		    	File base = new File(project.getBasedir(), artifactSet.getDirectory());
		    	Scanner scanner = buildContext.newScanner(base);
		    	scanner.setIncludes(artifactSet.getIncludes().toArray(new String[0]));
		    	scanner.setExcludes(artifactSet.getExcludes().toArray(new String[0]));
		    	scanner.scan();
		    	for ( String file : scanner.getIncludedFiles() ) {
		    		filesToSign.add(new File(base, file));
		    	}
    		}
    	}
    	
    	if ( filesToSign.isEmpty() ) { 
    		getLog().info("No files to sign, skipping");
    		return;
    	}
    	
    	for ( File toSign : filesToSign )
    		getLog().info("Would sign " + toSign);

        // Set up the TLS client
        System.setProperty("javax.net.ssl.keyStore", keyStore);
        System.setProperty("javax.net.ssl.keyStorePassword", keyStorePassword);
        String oldSslDebug = null;
    	if ( sslDebug ) {
    	    oldSslDebug = System.setProperty("javax.net.debug","all");
    	}
    	
        SignedFiles signedFiles = new SignedFiles(filesToSign);
    	
        try {
            String signingSetID = makeSigningRequest(signedFiles);
            downloadSignedFiles(signedFiles, signingSetID);
        } catch (SOAPException | IOException e) {
            throw new MojoExecutionException("Signing failed : " + e.getMessage(), e);
        } finally {
            if ( sslDebug ) {
                if ( oldSslDebug != null ) {
                    System.setProperty("javax.net.debug", oldSslDebug);
                } else {
                    System.clearProperty("javax.net.debug");
                }
            }
        }
    }


    private String makeSigningRequest(SignedFiles signedFiles) throws SOAPException, IOException, MojoExecutionException {
        log("Constructing the code signing request");

        SOAPMessage message = SOAP_MSG_FACTORY.createMessage();
        SOAPBody body = populateEnvelope(message, NS);

        SOAPElement requestSigning = body.addChildElement("requestSigning", NS);
        SOAPElement requestSigningRequest =
                requestSigning.addChildElement("requestSigningRequest", NS);

        addCredentials(requestSigningRequest, this.userName, this.password, this.partnerCode);

        SOAPElement applicationName =
                requestSigningRequest.addChildElement("applicationName", NS);
        applicationName.addTextNode(this.applicationName);

        SOAPElement applicationVersion =
                requestSigningRequest.addChildElement("applicationVersion", NS);
        applicationVersion.addTextNode(this.applicationVersion);

        SOAPElement signingServiceName =
                requestSigningRequest.addChildElement("signingServiceName", NS);
        signingServiceName.addTextNode(this.signingService);

        SOAPElement commaDelimitedFileNames =
                requestSigningRequest.addChildElement("commaDelimitedFileNames", NS);
        commaDelimitedFileNames.addTextNode(signedFiles.getCommaSeparatedUploadFileNames());

        SOAPElement application =
                requestSigningRequest.addChildElement("application", NS);
        application.addTextNode(signedFiles.getApplicationString());

        // Send the message
        SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
        SOAPConnection connection = soapConnectionFactory.createConnection();

        log("Sending signing request to server and waiting for response");
        SOAPMessage response = connection.call(message, SIGNING_SERVICE_URL);

        if ( getLog().isDebugEnabled()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(2 * 1024);
            response.writeTo(baos);
            getLog().debug(baos.toString("UTF-8"));
        }

        log("Processing response");
        SOAPElement responseBody = response.getSOAPBody();

        // Should come back signed
        NodeList bodyNodes = responseBody.getChildNodes();
        NodeList requestSigningResponseNodes = bodyNodes.item(0).getChildNodes();
        NodeList returnNodes = requestSigningResponseNodes.item(0).getChildNodes();

        String signingSetID = null;
        String signingSetStatus = null;
        StringBuilder errors = new StringBuilder();

        for (int i = 0; i < returnNodes.getLength(); i++) {
            Node returnNode = returnNodes.item(i);
            if (returnNode.getLocalName().equals("signingSetID")) {
                signingSetID = returnNode.getTextContent();
            } else if (returnNode.getLocalName().equals("signingSetStatus")) {
                signingSetStatus = returnNode.getTextContent();
            } else if (returnNode.getLocalName().equals("result") ) {
                final NodeList returnChildNodes = returnNode.getChildNodes();
                for (int j = 0; j < returnChildNodes.getLength(); j++ ) {
                    if ( returnChildNodes.item(j).getLocalName().equals("errors") ) {
                        extractErrors(returnChildNodes.item(j), errors);
                    }
                }
            }
        }

        if (!signingService.contains("TEST") && !"SIGNED".equals(signingSetStatus) ||
                signingService.contains("TEST") && !"INITIALIZED".equals(signingSetStatus) ) {
            throw new BuildException("Signing failed. Status was: " + signingSetStatus + " . Reported errors: " + errors + ".");
        }

        return signingSetID;
    }


    private void extractErrors(Node errorsNode, StringBuilder errors) {
        
        for (int i = 0 ; i < errorsNode.getChildNodes().getLength(); i++) {
            Node errorNode = errorsNode.getChildNodes().item(i);
            final NodeList errorChildNodes = errorNode.getChildNodes();
            for ( int j = 0; j < errorChildNodes.getLength(); j++) {
                Node item = errorChildNodes.item(j);
                if ( item.getLocalName().equals("errorMessage") ) {
                    if ( errors.length() > 0 ) {
                        errors.append(" ,");
                    }
                    errors.append(item.getTextContent());
                }
            }
        }
    }

    // pass-through method to make it easier to copy/paste code from tomcat's ant mojos
    private void log(String msg) {
		getLog().info(msg);
	}
    
    public class BuildException extends MojoExecutionException {
    	
		private static final long serialVersionUID = 1L;

		public BuildException(String message) {
    		super(message);
    	}
    	
    }

    private void downloadSignedFiles(SignedFiles signedFiles, String id)
            throws SOAPException, IOException, BuildException {

        log("Downloading signed files. The signing set ID is: " + id);

        SOAPMessage message = SOAP_MSG_FACTORY.createMessage();
        SOAPBody body = populateEnvelope(message, NS);

        SOAPElement getSigningSetDetails = body.addChildElement("getSigningSetDetails", NS);
        SOAPElement getSigningSetDetailsRequest =
                getSigningSetDetails.addChildElement("getSigningSetDetailsRequest", NS);

        addCredentials(getSigningSetDetailsRequest, this.userName, this.password, this.partnerCode);

        SOAPElement signingSetID =
                getSigningSetDetailsRequest.addChildElement("signingSetID", NS);
        signingSetID.addTextNode(id);

        SOAPElement returnApplication =
                getSigningSetDetailsRequest.addChildElement("returnApplication", NS);
        returnApplication.addTextNode("true");

        // Send the message
        SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
        SOAPConnection connection = soapConnectionFactory.createConnection();

        log("Requesting signed files from server and waiting for response");
        SOAPMessage response = connection.call(message, SIGNING_SERVICE_URL);

        log("Processing response");
        SOAPElement responseBody = response.getSOAPBody();

        // Check for success

        // Extract the signed file(s) from the ZIP
        NodeList bodyNodes = responseBody.getChildNodes();
        NodeList getSigningSetDetailsResponseNodes = bodyNodes.item(0).getChildNodes();
        NodeList returnNodes = getSigningSetDetailsResponseNodes.item(0).getChildNodes();

        String result = null;
        String data = null;

        for (int i = 0; i < returnNodes.getLength(); i++) {
            Node returnNode = returnNodes.item(i);
            if (returnNode.getLocalName().equals("result")) {
                result = returnNode.getChildNodes().item(0).getTextContent();
            } else if (returnNode.getLocalName().equals("signingSet")) {
                data = returnNode.getChildNodes().item(1).getTextContent();
            }
        }

        if (!"0".equals(result)) {
            throw new BuildException("Download failed. Result code was: " + result);
        }

        signedFiles.extractFilesFromApplicationString(data);
    }


    private static SOAPBody populateEnvelope(SOAPMessage message, String namespace)
            throws SOAPException {
        SOAPPart soapPart = message.getSOAPPart();
        SOAPEnvelope envelope = soapPart.getEnvelope();
        envelope.addNamespaceDeclaration(
                "soapenv","http://schemas.xmlsoap.org/soap/envelope/");
        envelope.addNamespaceDeclaration(
                namespace,"http://api.ws.symantec.com/webtrust/codesigningservice");
        return envelope.getBody();
    }


    private static void addCredentials(SOAPElement requestSigningRequest,
            String user, String pwd, String code) throws SOAPException {
        SOAPElement authToken = requestSigningRequest.addChildElement("authToken", NS);
        SOAPElement userName = authToken.addChildElement("userName", NS);
        userName.addTextNode(user);
        SOAPElement password = authToken.addChildElement("password", NS);
        password.addTextNode(pwd);
        SOAPElement partnerCode = authToken.addChildElement("partnerCode", NS);
        partnerCode.addTextNode(code);
    }



    /**
     * Ensures that unique file names are sent to the signing service
     * 
     * <p>We can't rely on the order in which we set the file names in the signing request, since
     * that is not preserved in the zipped response.</p>
     * 
     * <p>The file extension is kept since the signing service appears to use it to figure out what
     * to sign and how to sign it.</p>
     *
     */
    static class SignedFiles {
        
        private Map<String, File> fileNameMapping = new HashMap<>();
        
        public SignedFiles(List<File> filesToSign) {
            for (int i = 0; i < filesToSign.size(); i++) {
                File f = filesToSign.get(i);
                String fileName = f.getName();
                int extIndex = fileName.lastIndexOf('.');
                String newName;
                if (extIndex < 0) {
                    newName = Integer.toString(i);
                } else {
                    newName = Integer.toString(i) + fileName.substring(extIndex);
                }

                fileNameMapping.put(newName, f);
            }
        }
        
        public String getCommaSeparatedUploadFileNames() {
            
            return StringUtils.join(fileNameMapping.keySet().iterator(), ",");
        }

        /**
         * Zips the files, base 64 encodes the resulting zip and then returns the
         * string. It would be far more efficient to stream this directly to the
         * signing server but the files that need to be signed are relatively small
         * and this simpler to write.
         * 
         * @throws IOException in case of any IO problems 
         *
         */
        public String getApplicationString() throws IOException {

            // 16 MB should be more than enough for Tomcat
            // TODO: Refactoring this entire class so it uses streaming rather than
            //       buffering the entire set of files in memory would make it more
            //       widely useful.
            ByteArrayOutputStream baos = new ByteArrayOutputStream(16 * 1024 * 1024);
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                byte[] buf = new byte[32 * 1024];
                for ( Map.Entry<String, File> entry : fileNameMapping.entrySet() ) {
                    try (FileInputStream fis = new FileInputStream(entry.getValue())) {
                        ZipEntry zipEntry = new ZipEntry(entry.getKey());
                        zos.putNextEntry(zipEntry);
                        int numRead;
                        while ( (numRead = fis.read(buf)) >= 0) {
                            zos.write(buf, 0, numRead);
                        }
                    }
                }
            }

            return new String(Base64.encodeBase64(baos.toByteArray()), "UTF-8");
        }

        /**
         * Removes base64 encoding, unzips the files and writes the new files over
         * the top of the old ones.
         */
        public void extractFilesFromApplicationString(String data) throws IOException {
            
            ByteArrayInputStream bais = new ByteArrayInputStream(Base64.decodeBase64(data.getBytes("UTF-8")));
            try (ZipInputStream zis = new ZipInputStream(bais)) {
                byte[] buf = new byte[32 * 1024];
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    File outFile = fileNameMapping.get(entry.getName());
                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        int numRead;
                        while ((numRead = zis.read(buf)) >= 0) {
                            fos.write(buf, 0, numRead);
                        }
                    }
                }
            }
        }
    }
}
