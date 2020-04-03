# Notice


This code is Derivative Works from [webprofile-ref-project-v3](https://bitbucket.org/srasmusson/webprofile-ref-project-v3)
* modified from a SOAP back-channel binding to an encypted & signed front-channel Redirect Binding for the AuthnRequest.
* ArtifactResolutionServlet.java removed as the SAMLResponse to the SP was changed to use POST Bindings  
* Relevant ConsumerServlet.java methods moved to the Authentication Handler 
* ConsumerServlet.java removed
* Renamed OpenSAMLUtils.java to Helpers.java
* [Apache License for webprofile-ref-project-v3](https://bitbucket.org/srasmusson/webprofile-ref-project-v3/src/master/LICENSE)
* SPCredentials originally created by Privat on 13/05/14 was heavily modified to make use of JKS stored on the file-system. 
