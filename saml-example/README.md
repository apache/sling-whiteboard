## Localhost Setup
Procedure for localhost testing

### Start and Configure an External Identity Provider 
1. Start a Keycloak Server 
`docker run -p 8484:8080 -e KEYCLOAK_USER=admin -e KEYCLOAK_PASSWORD=admin  jboss/keycloak`
2. Login using http://localhost:8484/auth/admin/ 
   - username: admin, password: admin
3. Configure a Realm   
   - Click "Add Realm" 
   - Select the file located at `saml-example/src/main/resources/sling-realm-export.json` 
![](src/main/resources/realm-add.png)
Note. The preconfigured realm contains configuration for the client and the groups, but does not contain users.
4. Add user(s)
   - Select Users under the "Sling" Realm
      ![](src/main/resources/user-create.png)   
   - Set user attributes; specifically "userid"   
      ![](src/main/resources/user-set-attribute.png)
   - Set user password 
      ![](src/main/resources/user-set-password.png)
   - Set user groups; specifically join "pcms-authors"
      ![](src/main/resources/user-add-groups.png)  
   



### Sling SAML2 Service Provider Setup   

1. Start Sling (Assuming a new instance of Sling 12)
2. Run `mvn clean install -P autoInstallBundle` from saml-handler project  
Note: saml-handler is the core bundle offering SAML2 Sign on
3. Run `mvn clean install -P autoInstallPackage` from saml-example project  
Note: saml-example is example setup package containing:  OSGI configurations, service-user and ACL's. 
This setup is detailed in the section below.