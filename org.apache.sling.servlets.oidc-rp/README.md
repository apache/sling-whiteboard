# Apache Sling OpenID Connect Relying Party support bundle

## Sling Starter Deployment

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

## Whiteboard graudation TODO 

- bundle/package should probably be org.apache.sling.extensions.odic-rp, as the primary entry point is the Java API
- document usage; make sure to explain this is _not_ an authentication handler
- provide a sample content package and instructions how to use
- review to see if we can use more of the Nimbus SDK, e.g. enpodints discovery, token parsing
- review security best practices
