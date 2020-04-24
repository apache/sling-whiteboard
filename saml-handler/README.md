# Apache Sling SAML2 Handler (NOT FOR PRODUCTION)

This project is intended to be a contribution to the [Apache Sling](https://sling.apache.org) project;
 it has a SAML2 Service Provider Authentication Handler and the associated SAML2 servlets and utilities. It is a work in progress and not production ready!

## Overview
https://en.wikipedia.org/wiki/SAML_2.0

* The SAMLRequest uses HTTP Redirect Binding, and the contained Authn Request object instructs the IDP to use HTTP Post Binding. 

![](src/main/resources/Saml2SP.png)
 
## Features  
Sling applications to authenticate users against Identity Providers (idp) 
such as Keycloak or Shibboleth using SAML2 protocols.

Synchronize user management based on the SAML2 Assertion and OSGi bundle configs  
  - Create the user upon successful IDP authentication
  - Sync user membership of groups as defined in the OSGi configs (Pending)

`sp` is the package for service provider classes   
`Helpers` hold static utility methods used with the OpenSAML V3 library  
The parent `saml2` package has interface definitions, the bundle Activator

 
## JKS Options  
Just as Jetty requires a JKS to enable https, the SAML2 SP bundle requires a JKS to hold the IDP's signing certificate and to hold the SAML2 Service providers encryption key-pair. One suggestion is to locate these under the sling folder...
 
 
 `$ cd sling`   
 `$ mkdir keys`  
 `$ cd keys`
  
### Enable Jetty HTTPS

Create KeyStore & Generate Self Signed Cert (not for prod). While https on Jetty is technically not required, it serves a few purposes here: provides better security for direct access, and confirms the Java Keystore is configured properly and accessible by the sling system user. 
  
#### Make a JKS to hold a SSL (self-signed) Certificate 
 `$ keytool -genkeypair -keyalg RSA -validity 365 -alias sslstore -keystore sslKeystore.jks -keypass jettykeypass
-storepass  JKSPassord -dname "CN=localhost, OU=LSA Technology Services, O=University of Michigan,L=Ann Arbor, S=MI, C=US"`

Note: Make note of the JKS filename and path, storepass, keypass, and cert alias.  

#### Configure https on Jetty   
The following are based on the example sslKeystore contained under resources.  
org.apache.felix.https.enable=B"true"  
org.osgi.service.http.port.secure=I"443"  
org.apache.felix.https.keystore="./sling/keys/sslKeystore.jks"  
org.apache.felix.https.keystore.password="JKSPassord"  
org.apache.felix.https.keystore.key.password="jettykeypass" 
org.apache.felix.https.truststore.password="JKSPassord"     

![](src/main/resources/jettyHttps.png)

Note: To use the example sslKeystore.jks, copy it to your Sling folder ./sling/keys/sslKeystore.jks  
After enabling Jetty to use https over port 2443, you will need to accept the browser security warning when accessing https://localhost:2443/ due to the use of a self-signed certificate.

## IDP Test Setup
Run an IDP locally.  
1. Assuming Sling runs on 8080, use the docker commnd to run Keycloak on port *8088* sign into the IDP using admin:admin   
1.1. `docker run -p 8088:8080 -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin quay.io/keycloak/keycloak:9.0.2`  
or run it in standalone https://www.keycloak.org/docs/latest/getting_started/  
1.2. Configure the IDP manually with some basic settings such as the one described below;   
    * create a localhost realm with a Client ID set to https://localhost:2443/ or whatever entity identifier you want
    * convert the realm cert (http://localhost:8088/auth/admin/master/console/#/realms/localhost/keys) from pem to der
    * save this file and import it into the JKS giving it an alias localhost. The SP uses this validate the IDP's signature
    * Enabled = On
    * Include AuthnStatement = On
    * Sign Assertions = On
    * Encrypt Assertions = On
    * Client Signature Required = Off (This could be an open issue. Works for shib-idp not keycloak)
    * Valid Redirect URIs = https://localhost:2443/*
    * Base URL = https://localhost:2443/
    * Master SAML Processing URL = https://localhost:2443/          
2. Create a User & a Group
    * Username = saml2-example   
    * Password = password
    * Group: pcms-authors
    * Assign the user to the group
    * Configure the IDP to release the group
    * Decide or set an attribute to for the User ID
    * Add an other user attributes desired to be released
    * Intercept the SAML Response and verify the content
      

References
https://www.keycloak.org/getting-started/getting-started-docker

Links  
admin:admin log-in http://localhost:8088/auth/admin  
localhost realm http://localhost:8088/auth/admin/master/console/#/realms/localhost/clients
     
### Put your Service Provider KeyPair into a JKS 
Option 1: Generate using Keytool  
`$ keytool -genkey -alias samlKeys -keyalg RSA -keystore samlKeystore.jks`  
Make note of the storepass, alias, filename, and key password.  
These will all be needed to configure SP encryption.  
The generated JKS should be imported into your Keycloak localhost Client "SAML Keys"

Option 2: Import Existing into jks  
`$ keytool -importkeystore -srckeystore serviceProviderKeys.p12 -destkeystore samlKeystore.jks
-srcstoretype pkcs12 -alias spKeysAlias`
 
 

### Put your Identity Provider's Signing Certificate into SAML JKS
Get the cert.pem from Realm Setting > Keys
Copy pem cert and paste into a text file

-----BEGIN CERTIFICATE-----   
MIICoTCCAYkCBgFxqKn5fjANBgkqhkiG9w0BAQsFADAUMRIwEAYDVQQDDAlsb2Nh   
bGhvc3QwHhcNMjAwNDIzMjAwOTAzWhcNMzAwNDIzMjAxMDQzWjAUMRIwEAYDVQQD   
....   
-----END CERTIFICATE-----
    
`$ keytool -import -file idp-signing.pem  -keystore samlKeystore.jks -alias IDPSigningAlias`


## SAML2 Service Provider Configurations
### Apache Felix JAAS Configuration Factory Configuration (OSGI) 
jaas.controlFlag=Sufficient  
jaas.ranking=110  
jaas.realmName=jackrabbit.oak  
jaas.classname=org.apache.sling.auth.saml2.sp.Saml2LoginModule  

![](src/main/resources/jaasConfiguration.png)

### SAML2 SP Service User Mapper (OSGI and ACL)
Configure a Service User to handle the User Management.  
org.apache.sling.auth.saml2:Saml2UserMgtService=saml2-user-mgt
![](src/main/resources/serviceUserMapping.png)

Grant the system user `saml2-user-mgt` sufficient ACL's to create and write to users home.
![](src/main/resources/saml2-user-mgt-acls.png)



### SAML2 SP Configuration Example (OSGI)
path=https://localhost:2443/  
service.ranking=1000  
entityID=https://localhost:2443/  
acsPath=/sp/consumer  
saml2userIDAttr=urn:oid:2.5.4.42  
saml2userHome=/home/users/saml  
saml2groupMembershipAttr=  
saml2SessionAttr=saml2AuthInfo  
saml2IDPDestination=https://localhost:8088/idp/profile/SAML2/Rediect/SSO   
saml2SPEnabled=true   
jksFileLocation=./sling/keys/slingSP.jks   
jksStorePassword=storepass   
idpCertAlias=localhost
spKeysAlias=slingSP
spKeysPassword=encPassword

![](src/main/resources/saml2localKeycloak.png)

Note: After configuring the SAML2 authentication handler, the Sling Form login can still be accessed directly http://localhost:8080/system/sling/form/login?resource=%2F

Optionally, configure Keycloak to release a group to the Sling Client and create that group within Sling. Add the group ID (from the assetion) to the OSGI configuration.

## Test   
Visit https://localhost:2443 and observe login takes place on the http://localhost:8088 Keycloak Identity Provider.
![](src/main/resources/userSignInToIDP.png)

Enter the credentials saml2-example:password, and observe user is granted access to the system.
![](src/main/resources/signedInUser.png)

