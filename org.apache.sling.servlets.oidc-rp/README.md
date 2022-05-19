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


## Whiteboard graudation TODO 

- bundle/package should probably be org.apache.sling.extensions.oidc-rp, as the primary entry point is the Java API
- document usage; make sure to explain this is _not_ an authentication handler
- provide a sample content package and instructions how to use
- review to see if we can use more of the Nimbus SDK, e.g. enpodints discovery, token parsing
- review security best practices
