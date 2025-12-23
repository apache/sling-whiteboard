[<img src="http://sling.apache.org/res/logos/sling.png"/>](http://sling.apache.org)

# Apache Sling Feature Model Analyser Extension

This extension of the Sling Featuremodel Launcher adds the capability to run analysis of the provided feature model and identify incoherent feature combinations to block startup.

## Usage
Configuration of the Analyser Launcher is similar to the launcher - in addition the apiregion extension has to be added to the classpath and regionsorder has to be configured via system property for all analysers to be executed.
Dynamic configuration of AnalyserTasks is out of scope of first iteration.
`java -Dregionsorder="global,deprecated,internal" -cp org.apache.sling.feature.extension.apiregions-1.0.4.jar:org.apache.sling.feature.extension.analyser-0.0.1-SNAPSHOT.jar:org.apache.sling.feature.launcher-1.0.4.jar org.apache.sling.feature.launcher.impl.Main -u file://$HOME/.m2/repository -f mvn:org.myown/myfeature/1.0.0-SNAPSHOT/slingosgifeature/myclassifier`