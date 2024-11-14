# Apache Sling OAuth 2.0 client with OIDC support

> [!IMPORTANT]
> The Java APIs exported by this bundle are considered **experimental** and are marked as being
> @ProviderType. Expect them to change frequently until they are considered stable.

This bundle adds support for Sling-based applications to function as an OAuth 2.0 client 
([RFC 6749](https://datatracker.ietf.org/doc/html/rfc6749)) and implements the basis for being an 
[Open ID connect](https://openid.net/developers/how-connect-works/) relying party.

Its main objective is to simplify access to id and access tokens in a secure manner. It currently supports
the authentication code flow based on OIDC and OAuth 2.0 .

## Usage

### High-level APIs

The bundle exposes an abstract `OAuthEnabledSlingServlet` that contains the boilerplate code needed
to obtain a valid OAuth 2 access token.

Basic usage is as follows

```java

import org.apache.sling.extensions.oauth_client.*;

@Component(service = { Servlet.class })
@SlingServletPaths(value = "/bin/myservlet")
public class MySlingServlet extends OAuthEnabledSlingServlet {

    private final MyRemoteService svc;
   
    @Activate
    public MySlingServlet(@Reference OidcConnection connection, 
        @Reference OAuthTokenStore tokenStore,
        @Reference OAuthTokenRefresher oidcClient,
        @Reference MyRemoteService svc) {
        super(connection, tokenStore, oidcClient);
        this.svc = svc;
    }

    @Override
    protected void doGetWithToken(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response,
            OAuthToken token) throws IOException, ServletException {

        this.csv.query("my-query", token.getValue()).writeResponseTo(response.getOutputStream());
    }
}
```

### Low-level APIs

TODO

### Clearing access tokens

If an access token response contains an expiry date the bundle will make sure that it is not
accessible via APIs. This will not cover all scenarios because access tokens can expire or be
invalidated out of band.

The client will need to determine if the access token is invalid as this is a provider-specific
check.

To remove invalid access tokens there is a low-level API

```java
public class MyComponent {}
    @Reference private OAuthTokenStore tokenStore;
    
    public void execute(@Reference OidcConnection connection, ResourceResolver resolver) {
        // code elided
        if ( accessTokenIsInvalid() ) {
            tokenStore.clearAccessToken(connection, resolver);
            // redirect to provider or present a message to the user
        }
    }
}
```

For classes that extend from the `OAuthEnabledSlingServlet` the following method override can be
applied

```java
@Component(service = { Servlet.class })
@SlingServletPaths(value = "/bin/myservlet")
public class MySlingServlet extends OAuthEnabledSlingServlet {

    // other methods elided

    @Override
    protected boolean isInvalidAccessTokenException(Exception e) {
        return e.getCause() instanceof InvalidAccessTokenException;
    }
}
```


### Error handling

The top-level servlets used for the OAuth flow will validate parameters that are expected to be
sent by the client and return a status code of 400 in case the parameters are missing or invalid.

For others problems related to the OAuth flow these servlets throw specific subclasses of ServletException.
The exceptions will return generic messages that can be displayed directly to the user and store
the actual cause in nested exception so that it is logged.

These exceptions are:

- `org.apache.sling.extensions.oauth_client.impl.OAuthCallbackException`
- `org.apache.sling.extensions.oauth_client.impl.OAuthEntryPointException`
- `org.apache.sling.extensions.oauth_client.impl.OAuthFlowException` (superclass)

It is recommended that applications install specific error handlers for these exceptions. See the
[Apache Sling error handling documentation](https://sling.apache.org/documentation/the-sling-engine/errorhandling.html)
for more details.

### Client registration

Client registration is specific to each provider. When registering, note the following:

- the redirect URL must be set to $HOST/system/sling/oauth/callback registered. For development this is typically http://localhost:8080/system/sling/oauth/callback
- write down the client id, client secret obtained from the OIDC provider
- you may need to provide in advance the set of scopes accessible to your client

Validated providers:

- Google, OIDC, with base URL of https://accounts.google.com , see [Google OIDC documentation](https://developers.google.com/identity/protocols/oauth2/openid-connect)
- GitHub, OAuth 2.0, with authorizationEndpoint https://github.com/login/oauth/authorize and tokenEndpoint https://github.com/login/oauth/access_token
- KeyCloak ( see [#keycloak] )

### Deployment

A set of dependencies required by this bundle, on top of the Sling Starter ones, is available at `src/main/features/main.json`.

Since the bundle relies on encryption to create and validate the OAuth 2.0 `state` parameter, a `CryptoService` must be configured

```json
    "org.apache.sling.commons.crypto.internal.FilePasswordProvider~oauth": {
        "path": "secrets/encrypt/password",
        "fix.posixNewline": true
    },
    "org.apache.sling.commons.crypto.jasypt.internal.JasyptRandomIvGeneratorRegistrar~oauth": {
       "algorithm": "SHA1PRNG"
    },
    "org.apache.sling.commons.crypto.jasypt.internal.JasyptStandardPbeStringCryptoService~oauth": {
       "names": [ "sling-oauth" ],
       "algorithm": "PBEWITHHMACSHA512ANDAES_256"
    }
```

The _sling-oauth_ names property is important since it is used to select the CryptoService used by this bundle.

In addition, one of the following types of OSGi configuration must be added:

#### OIDC variant

```json
"org.apache.sling.extensions.oauth_client.impl.OidcConnectionImpl~provider": {
    "name": "provider",
    "baseUrl": "https://example.com",
    "clientId": "$[secret:provider/clientId]",
    "clientSecret": "$[secret:provider/clientSecret]",
    "scopes": ["openid"]
}
```

#### OAuth variant

```json
"org.apache.sling.extensions.oauth_client.impl.OAuthConnectionImpl~github": {
    "name": "provider",
    "authorizationEndpoint": "https://example.com/login/oauth/authorize",
    "tokenEndpoint": "https://example.com/login/oauth/access_token",
    "clientId": "$[secret:provider/clientId]",
    "clientSecret": "$[secret:provider/clientSecret]",
    "scopes": ["user:email"]
}
```

At this point, the OAuth process can be kicked of by navigating to http://localhost:8080/system/sling/oauth/entry-point?c=provider

### Token storage

The tokens can be stored either in the JCR repository, under the user's home, or in Redis. A configuration is required to select a provider.

#### JCR Storage

The tokens are stored under the user's home, under the `oauth-tokens/$PROVIDER_NAME` node.

```json
"org.apache.sling.extensions.oauth_client.impl.JcrUserHomeOAuthTokenStore" : {
}
```

#### Redis storage

```json
"org.apache.sling.extensions.oauth_client.impl.RedisOAuthTokenStore" : {
    "redisUrl": "redis://localhost:6379"
}
```

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
    -d "factoryPid=org.apache.sling.extensions.oauth_client.impl.OidcConnectionImpl" \
    http://localhost:8080/system/console/configMgr/org.apache.sling.extensions.oauth_client.impl.OidcConnectionImpl~keycloak-dev
```

Now you can 

- access KeyCloak on http://localhost:8081 
- access Sling on http://localhost:8080
- start the login process on http://localhost:8080/system/sling/oauth/entry-point?c=keycloak-dev

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

- in the client details page, set the valid redirect URIs to http://localhost:8080/system/sling/oauth/callback and save
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

### Future plans

- make the OAuthEnabledSlingServlet handle any method, not just get
- explore an AuthenticationHandler that can optionally expose the access tokens
- investigate PKCE (RFC 7636)
- investigate encrypted client-side storage of tokens
