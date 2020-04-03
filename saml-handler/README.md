# Apache Sling SAML2 Handler (NOT FOR PRODUCTION)

This project is intended to be a contribution to the [Apache Sling](https://sling.apache.org) project;
 it has a SAML2 Service Provider Authentication Handler and the associated SAML2 servlets and utilities.  
It is a work in progress and not production ready!

SP_POST_Request;_IdP_POST_Response
https://en.wikipedia.org/wiki/SAML_2.0#SP_POST_Request;_IdP_POST_Response

![](SAML2-browser-post.png)

## This bundle 
- Will allow Sling applications to authenticate users against Identity Providers (idp) 
such as Shibboleth using SAML2 protocols. 
- Will sync of user management based on the SAML2 Assertion and OSGi bundle configs
  - Sync attributes from the IDP to the User as specified in the bundle OSGi configs
  - Create the user upon successful IDP authentication
  - Sync user membership of groups as defined in the OSGi configs
- Packages
  - `idp` is a test fixture based on the OpenSAML V3 eBook. It will be useful for minimizing 
  setup for testing purposes. Set to disabled for production.  
  - `sp` is the package for service provider classes utilities
  - `Helpers` static utilities for help using the opensaml library
    
 
##Set up JKS  
 `$ cd sling`   
 `$ mkdir keys`  
 `$ cd keys`
  
 ### Create KeyStore & Generate Self Signed Cert (not for prod)
 keytool \
     -genkeypair \
     -keyalg RSA \
     -validity 365 \
     -alias samlStore \
     -keystore samlKeystore.jks  \
     -keypass key_password \
     -storepass  storepassword \
     -dname "CN=localhost, OU=LSA Technology Services, O=University of Michigan,L=Ann Arbor, S=MI, C=US"
     
 ### Generate IDP KeyPair (not for prod)
 keytool -genkey -alias samlKeys -keyalg RSA -keystore samlKeystore.jks

