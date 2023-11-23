package org.apache.sling.bnd.providertype;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import aQute.bnd.osgi.Analyzer;
import aQute.bnd.osgi.Clazz;
import aQute.bnd.osgi.Descriptors.TypeRef;
import aQute.bnd.osgi.Resource;
import aQute.bnd.service.AnalyzerPlugin;
import aQute.lib.json.Decoder;
import aQute.lib.json.JSONCodec;
import aQute.service.reporter.Reporter;

/**
 * Enforces that no classes implement or extend a type marked as provider.
 * Provider types are retrieved from the resource "META-INF/api-info.json" which is expected to be provided
 * in the class path.
 */
public class ProviderTypeScanner implements AnalyzerPlugin {

    private static final String API_INFO_JSON_RESOURCE_PATH = "META-INF/api-info.json";
    private static final String FIELD_PROVIDER_TYPES = "providerTypes";
    private static final String MESSAGE = "Type \"%s\" %s provider type \"%s\". This is not allowed!";

    @Override
    public boolean analyzeJar(Analyzer analyzer) throws Exception {
        List<Resource> apiInfoJsonResources = analyzer.findResources(s -> s.equals(API_INFO_JSON_RESOURCE_PATH)).collect(Collectors.toList());
        if(apiInfoJsonResources.isEmpty()) {
            analyzer.warning("Could not find resource \"%s\" exposed from the classpath", API_INFO_JSON_RESOURCE_PATH);
        } else {
            Set<String> providerTypes = new HashSet<>();
            for (Resource apiInfoJsonResource : apiInfoJsonResources) {
                try {
                    providerTypes.addAll(collectProviderTypes(analyzer, apiInfoJsonResource));
                } catch (Exception e) {
                    throw new IllegalStateException("Could not parse JSON from resource " + apiInfoJsonResource, e);
                }
            }
            checkIfExtendingType(analyzer, analyzer.getClassspace().values(), providerTypes);
        }
        return false;
    }

    private void checkIfExtendingType(Reporter reporter, Collection<Clazz> clazzes, Set<String> providerTypes) {
        for (Clazz clazz : clazzes) {
            if (clazz.getSuper() != null &&  (providerTypes.contains(clazz.getSuper().getFQN()))) {
                reporter.error(MESSAGE, clazz.getFQN(), "extends", clazz.getSuper().getFQN());
            }
            for (TypeRef interfaceType : clazz.interfaces()) {
                if (providerTypes.contains(interfaceType.getFQN())) {
                    reporter.error(MESSAGE, clazz.getFQN(), "implements", interfaceType.getFQN());
                }
            }
        }
    }

    private Set<String> collectProviderTypes(Reporter reporter, Resource apiInfoResource) throws Exception {
        JSONCodec codec = new JSONCodec();
        // read JSON file
        try (InputStream input = apiInfoResource.openInputStream();
            Decoder decoder = codec.dec().from(input)) {
            Map<?, ?> jsonMap = decoder.get(Map.class);
            Object providerTypes = jsonMap.get(FIELD_PROVIDER_TYPES);
            if (providerTypes == null) {
                reporter.error("Resource \"%s\" does not contain a field named \"%s\"", API_INFO_JSON_RESOURCE_PATH, FIELD_PROVIDER_TYPES);
            } else if (!(providerTypes instanceof Collection)) {
                reporter.error("Field \"%s\" in JSON resource \"%s\" is not containing a string array but a type converted to %s", FIELD_PROVIDER_TYPES, API_INFO_JSON_RESOURCE_PATH, providerTypes.getClass().getName());
            } else {
                return new HashSet<>((Collection<String>)providerTypes);
            }
        }
        return Collections.emptySet();
    }

}