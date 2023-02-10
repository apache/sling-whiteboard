# Apache Sling OpenID Connect Relying Party support bundle

## Prerequisites

### Client registration

An OpenID Connect client must be registrered with an authorization server, and a callback URL of $HOST/system/sling/oidc/callback registered. This is typically http://localhost:8080/system/sling/oidc/callback .

Validated providers:

- Google, with base URL of https://accounts.google.com , see [Google OIDC documentation](https://developers.google.com/identity/protocols/oauth2/openid-connect)

## Sling Starter Prerequisites

A number of additional bundles need to be added to the Sling Starter.

```diff
diff --git a/src/main/features/app/starter.json b/src/main/features/app/starter.json
index 9c9231f..18c1586 100644
--- a/src/main/features/app/starter.json
+++ b/src/main/features/app/starter.json
@@ -3,6 +3,34 @@
         {
             "id":"org.apache.sling:org.apache.sling.starter.content:1.0.12",
             "start-order":"20"
+        },
+        {
+            "id":"com.nimbusds:oauth2-oidc-sdk:9.35",
+            "start-order":"20"
+        },
+        {
+            "id":"com.nimbusds:nimbus-jose-jwt:9.22",
+            "start-order":"20"
+        },
+        {
+            "id":"com.nimbusds:content-type:2.2",
+            "start-order":"20"
+        },
+        {
+            "id":"com.nimbusds:lang-tag:1.6",
+            "start-order":"20"
+        },
+        {
+            "id":"org.apache.servicemix.bundles:org.apache.servicemix.bundles.jcip-annotations:1.0_2",
+            "start-order":"20"
+        },
+        {
+            "id":"net.minidev:json-smart:2.4.8",
+            "start-order":"20"
+        },
+        {
+            "id":"net.minidev:accessors-smart:2.4.8",
+            "start-order":"20"
         }
     ]
 }

```

### Deployment and configuration

After deploying the bundle using `mvn package sling:install` go to http://localhost:8080/system/console/configMgr and create a new configuration instance for _OpenID Connect connection details_.

### Kicking off the process

Ensure you are logged in.

- navigate to http://localhost:8080/system/sling/oidc/entry-point?redirect=/bin/browser.html
- you will be redirect to the identity provider, where you will need authenticate yourself and authorize the connection
- you will be redirected to the composum browser

At this point you need to can navigate to /home/users/${USERNAME}/oidc-tokens/${CONNECTION_NAME} and you will see the stored token and expiry date (if available ).


### Local development setup

#### Keycloak

1. Launch Keycloak locally

```
$ docker run --rm --volume (pwd)/keycloak-data:/opt/keycloak/data -p 8081:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:20.0.3 start-dev
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

#### Sling
 
1. OSGi bundles

TODO

2. OSGi config

```
org.apache.sling.servlets.oidc_rp.impl.OidcConnectionImpl
name: keycloak
baseUrl: http://localhost:8081/realms/sling
clientId: oidc-test
clientSecret: ( copied from above)
scopes: openid 

```

#### Obtaining the tokens

- navigate to http://localhost:8080/system/sling/login and login as admin/admin
- go to http://localhost:8080/system/sling/oidc/entry-point?redirect=/bin/browser.html/home/users
- complete the login flow
- navigate in composum to the user name of the admin user and verify that the 'oidc-tokens' node contains a keycloak node with the respective access_token and refresh_token properties 

#### Exporting the test realm

```
$ docker run --rm --volume (pwd)/keycloak-data:/opt/keycloak/data -p 8081:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:20.0.3 export --realm sling --users realm_file --file /opt/keycloak/data/export/sling.json
```

## Whiteboard graduation TODO 

- bundle/package should probably be org.apache.sling.extensions.oidc-rp, as the primary entry point is the Java API
- document usage; make sure to explain this is _not_ an authentication handler
- provide a sample content package and instructions how to use
- review to see if we can use more of the Nimbus SDK, e.g. enpodints discovery, token parsing
- review security best practices
