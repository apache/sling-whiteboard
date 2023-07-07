# Apache Sling OpenID Connect Relying Party support bundle

> **Warning**
> This bundle is under development, do not use in production.

## Usage

This bundle add support for Sling-based applications to function as
[Open ID connect](https://openid.net/developers/how-connect-works/) relying parties. Its main
objective is to simplify access to user and access id in a secure manner. It currently supports
the Authorization Code flow.

The bundle offers the following entry points

- and `OidcClient` service that communicates with the remote Open ID connect provider
- a `TokenStore` service that allows storage and retrieval of persisted tokens.

Basic usage is as follows

```java

import org.apache.sling.extensions.oidc_rp.*;

@Component(service = { Servlet.class })
@SlingServletPaths(value = "/bin/myservlet")
public class MySlingServlet {

  @Reference private OidcTokenStore tokenStore;
  @Reference private OidcClient oidcClient;

  public void accessRemoteResource(SlingHttpServletRequest request, SlingHttpServletResponse response) {
    OidcConnection connection = getConnection();
    OidcToken tokenResponse = tokenStore.getAccessToken(connection, request.getResourceResolver());
    
    switch ( tokenResponse.getState() ) {
      case VALID:
        doStuffWithToken(tokenResponse.getValue());
        break;
      case MISSING:
        response.sendRedirect(oidcClient.getOidcEntryPointUri(connection, request, "/bin/myservlet").toString());
        break;
      case EXPIRED:
        OidcToken refreshToken = tokenStore.getRefreshToken(connection, request.getResourceResolver());
        if ( refreshToken.getState() != OidcTokenState.VALID )
          response.sendRedirect(oidcClient.getOidcEntryPointUri(connection, request, "/bin/myservlet").toString());
        
        OidcTokens oidcTokens = oidcClient.refreshTokens(connection, refreshToken.getValue());
        tokenStore.persistTokens(connection, request.getResourceResolver(), oidcTokens);
        doStuffWithToken(tokenResponse.getValue());
        break;
    }
  }
}
```

### Client registration

Client registration is specific to each provider. When registering, note the following:

- the redirect URL must be set to $HOST/system/sling/oidc/callback registered. For development this is typically http://localhost:8080/system/sling/oidc/callback
- write down the client id, client secret obtained from the OIDC provider
- you may need to provide in advance the set of scopes accessible to your client

Validated providers:

- Google, with base URL of https://accounts.google.com , see [Google OIDC documentation](https://developers.google.com/identity/protocols/oauth2/openid-connect)
- KeyCloak ( see [#keycloak] )

### Deployment

A set of dependencies required by this bundle, on top of the Sling Starter ones, is available at `src/main/features/main.json`.
In addition, the following OSGi configuration must be added

```json
"org.apache.sling.servlets.oidc_rp.impl.OidcConnectionImpl~provider": {
    "name": "provider",
    "baseUrl": "https://.example.com",
    "clientId": "$[secret:provider/clientId]",
    "clientSecret": "$[secret:provider/clientSecret]",
    "scopes": ["openid"]
}
```

At this point, the OIDC process can be kicked of by navigating to http://localhost:8080/system/sling/oidc/entry-point?c=provider

### Token storage

The tokens are stored under the user's home, under the `oidc-tokens/$PROVIDER_NAME` node.


## Whiteboard graduation TODO 

- allow use of refresh tokens
- extract the token exchange code from the OidcCallbackServlet and move it to the OauthClientImpl
- provide a sample content package
- review security best practices
- investigate whether the OIDC entry point servlet is really needed


## Local development setup

### tl;dr

- run the keycloak container using the instructions for 'use existing test files'
- build the bundle once with `mvn clean install`
- run Sling with `mvn feature-launcher:start feature-launcher:stop -Dfeature-launcher.waitForInput`
- create OSGi config with 

```
export CLIENT_SECRET=$(cat src/test/resources/keycloak-import/sling.json | jq --raw-output '.clients[] | select (.clientId == "oidc-test") | .secret')

$ curl -u admin:admin -X POST -d "apply=true" -d "propertylist=name,baseUrl,clientId,clientSecret,scopes" \
    -d "name=keycloak-dev" \
    -d "baseUrl=http://localhost:8081/realms/sling" \
    -d "clientId=oidc-test"\
    -d "clientSecret=$CLIENT_SECRET" \
    -d "scopes=openid" \
    -d "factoryPid=org.apache.sling.extensions.oidc_rp.impl.OidcConnectionImpl" \
    http://localhost:8080/system/console/configMgr/org.apache.sling.extensions.oidc_rp.impl.OidcConnectionImpl~keycloak-dev
```

Now you can 

- access KeyCloak on http://localhost:8081 
- access Sling on http://localhost:8080
- start the OIDC login process on http://localhost:8080/system/sling/oidc/entry-point?c=keycloak-dev

### Keycloak

#### Use existing test files

Note that this imports the test setup with a single user with a _redirect_uri_ set to _http://localhost*_, which can be a security issue.

```
$ docker run --rm  --volume $(pwd)/src/test/resources/keycloak-import:/opt/keycloak/data/import -p 8081:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:20.0.3 start-dev --import-realm
```

#### Manual setup

1. Launch Keycloak locally

```
$ docker run --rm --volume $(pwd)/keycloak-data:/opt/keycloak/data -p 8081:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:20.0.3 start-dev
```

2. Create test realm

- access http://localhost:8081/
- go to 'Administration Console'
- login with admin:admin
- open dropdown from the top left and press 'Create realm'
- Select the name 'sling' and create it

3. Create client

- in the left navigation area, press 'clients'
- press 'Create client'
- Fill in 'Client ID' as 'oidc-test' and press 'Next'
- Enable 'Client authentication' and press 'Save'

4. Configure clients

- in the client details page, set the valid redirect URIs to http://localhost:8080/system/sling/oidc/callback and save
- navigate to the 'Credentials' tab and copy the Client secret

5. Add users

- in the left navigation area, press 'users'
- press 'create new user'
- fill in username: test and press 'create'
- go to the 'details' tab, clear any required user actions and press 'save'
- go to the 'credentials' tab and press 'set password'
- in the dialog, use 'test' for the password and password confirmation fields and then press 'save'
- confirm by pressing 'save password' in the new dialog


### Exporting the test realm

```
$ docker run --rm --volume (pwd)/keycloak-data:/opt/keycloak/data -p 8081:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:20.0.3 export --realm sling --users realm_file --file /opt/keycloak/data/export/sling.json
```
