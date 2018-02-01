The example are currently broken - the json needs to be updated to the latest format (change from start level to start order)

java -jar ../feature-applicationbuilder/target/org.apache.sling.feature.applicationbuilder-0.0.1-SNAPSHOT.jar -d sling -u ~/.m2/repository -o sling.json
java -jar ../feature-launcher/target/org.apache.sling.feature.launcher-0.0.1-SNAPSHOT.jar -a sling.json -I -v
