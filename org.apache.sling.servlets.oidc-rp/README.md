# Apache Sling OpenID Connect Relying Party support bundle

> **Warning**
> This bundle is under development, do not use in production.

## Introduction

This bundle add support for Sling-based applications to function as
[Open ID connect](https://openid.net/developers/how-connect-works/) relying parties. Its main
objective is to simplify access to user and access tokens in a secure manner.

## Whiteboard graduation TODO 

- bundle/package should probably be org.apache.sling.extensions.oidc, as the primary entry point is the Java API
- clarify Java API and allow extracting both id and access tokens
- allow use of refresh tokens
- document usage for the supported OIDC providers; make sure to explain this is _not_ an authentication handler
- provide a sample content package and instructions how to use
- review security best practices

## Prerequisites

### Client registration

An OpenID Connect client must be registrered with an authorization server, and a callback URL of $HOST/system/sling/oidc/callback registered. This is typically http://localhost:8080/system/sling/oidc/callback .

Validated providers:

- Google, with base URL of https://accounts.google.com , see [Google OIDC documentation](https://developers.google.com/identity/protocols/oauth2/openid-connect)

## Sling Starter Prerequisites

A number of additional bundles need to be added to the Sling Starter, see the feature model definition at src/main/features/main.json .

### Deployment and configuration

After deploying the bundle using `mvn package sling:install` go to http://localhost:8080/system/console/configMgr and create a new configuration factory instance for _OpenID Connect connection details_. Write down the name property, we'll refer to it as `$CONNECTION_NAME`.

### Kicking off the process

Ensure you are logged in.

- navigate to http://localhost:8080/system/sling/oidc/entry-point?c=$CONNECTION_NAME&redirect=/bin/browser.html
- you will be redirect to the identity provider, where you will need authenticate yourself and authorize the connection
- you will be redirected to the composum browser

At this point you can navigate to /home/users/${USERNAME}/oidc-tokens/${CONNECTION_NAME} and you will see the stored access token.

### Local development setup

#### tl;dr

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
    -d "factoryPid=org.apache.sling.servlets.oidc_rp.impl.OidcConnectionImpl" \
    http://localhost:8080/system/console/configMgr/org.apache.sling.servlets.oidc_rp.impl.OidcConnectionImpl~keycloak-dev
```

Now you can 

- access KeyCloak on http://localhost:8081 
- access Sling on http://localhost:8080
- start the OIDC login process on http://localhost:8080/system/sling/oidc/entry-point?c=keycloak-dev

#### Keycloak

##### Use existing test files

Note that this imports the test setup with a single user with a _redirect_uri_ set to _http://localhost*_, which can be a security issue.

```
$ docker run --rm  --volume $(pwd)/src/test/resources/keycloak-import:/opt/keycloak/data/import -p 8081:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:20.0.3 start-dev --import-realm
```

##### Manual setup

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
- go to http://localhost:8080/system/sling/oidc/entry-point?c=keycloak&redirect=/bin/browser.html/home/users
- complete the login flow
- navigate in composum to the user name of the admin user and verify that the 'oidc-tokens' node contains a keycloak node with the respective access_token and refresh_token properties 

#### Exporting the test realm

```
$ docker run --rm --volume (pwd)/keycloak-data:/opt/keycloak/data -p 8081:8080 -e KEYCLOAK_ADMIN=admin -e KEYCLOAK_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:20.0.3 export --realm sling --users realm_file --file /opt/keycloak/data/export/sling.json
```
