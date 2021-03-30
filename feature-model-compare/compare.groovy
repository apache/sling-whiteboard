#!/usr/bin/env groovy

@Grab("org.apache.sling:org.apache.sling.feature:1.2.10")
@Grab("org.apache.johnzon:johnzon-core:1.2.3")
@Grab("org.apache.sling:org.apache.sling.commons.johnzon:1.2.2")
@Grab("org.apache.felix:org.apache.felix.cm.json:1.0.2")
@Grab("org.osgi:org.osgi.util.function:1.0.0")
@Grab("org.apache.felix:org.apache.felix.converter:1.0.14")
@Grab("org.apache.sling:org.apache.sling.feature.diff:0.0.6")
@Grab("org.osgi:osgi.core:6.0.0")

import org.apache.sling.feature.ArtifactId
import org.apache.sling.feature.Feature
import org.apache.sling.feature.diff.DiffRequest

import java.io.FileReader
import java.io.PrintWriter
import java.io.Reader

import static org.apache.sling.feature.io.json.FeatureJSONReader.read
import static org.apache.sling.feature.diff.FeatureDiff.compareFeatures
import static org.apache.sling.feature.io.json.FeatureJSONWriter.write

Feature loadFeature(String jsonFile, String classifierSuffix) {
    println "Loading feature from '${jsonFile}'"
    File file = new File(jsonFile)
    Feature feature = null
    try(Reader reader = new FileReader(file)) {
        feature = read(reader, null)
        println "Loaded feature: ${feature}"
    }
    ArtifactId newArtifactId = feature.getId().changeClassifier("${feature.getId().getClassifier()}_${classifierSuffix}")
    feature = feature.copy(newArtifactId)
    return feature
}

if (args.length != 2) {
    println 'Usage ./compare.groovy feature-a.json feature-b.json'
    System.exit(1)
}

Feature featureA = loadFeature(args[0], 'a')
Feature featureB = loadFeature(args[1], 'b')

DiffRequest diffRequest = new DiffRequest()
    .setPrevious(featureA)
    .setCurrent(featureB)

Feature featureDiff = compareFeatures(diffRequest)

String mvnPath = featureDiff.getId().toMvnPath()
mvnPath = mvnPath.substring(mvnPath.lastIndexOf('/') + 1)
println "Writing to: ${mvnPath}"

try(Writer writer = new FileWriter(new File(mvnPath))) {
    write(writer, featureDiff)
}
