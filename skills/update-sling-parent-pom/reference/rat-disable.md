The RAT profile was enabled by default with SLING-4511. If this causes failures right after upgrade, you can temporarily disable by running Maven with '-Drat.skip=true'.

If this succeeds, ask the user if the property should be added to the pom.xml to permanently skip the check.
