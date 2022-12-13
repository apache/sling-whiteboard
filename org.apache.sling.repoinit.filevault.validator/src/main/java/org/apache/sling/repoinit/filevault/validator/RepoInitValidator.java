/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.repoinit.filevault.validator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.felix.cm.json.ConfigurationReader;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.vault.util.DocViewNode2;
import org.apache.jackrabbit.vault.util.DocViewProperty2;
import org.apache.jackrabbit.vault.validation.spi.DocumentViewXmlValidator;
import org.apache.jackrabbit.vault.validation.spi.GenericJcrDataValidator;
import org.apache.jackrabbit.vault.validation.spi.NodeContext;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessage;
import org.apache.jackrabbit.vault.validation.spi.ValidationMessageSeverity;
import org.apache.sling.repoinit.parser.RepoInitParsingException;
import org.apache.sling.repoinit.parser.impl.RepoInitParserService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RepoInitValidator implements DocumentViewXmlValidator, GenericJcrDataValidator {

    private final RepoInitParserService parser;
    
    public RepoInitValidator() {
        parser = new RepoInitParserService();
    }
    enum OsgiConfigurationSerialization {
        CFG,
        CFG_JSON,
        CONFIG
    }

    private static final String OSGI_CONFIG_NAME = "org\\.apache\\.sling\\.jcr\\.repoinit\\.RepositoryInitializer(~|-).*";

    private static final Pattern OSGI_CONFIG_NODE_NAME_PATTERN = Pattern.compile(OSGI_CONFIG_NAME);

    /**
     * https://sling.apache.org/documentation/bundles/configuration-installer-factory.html#configuration-serialization-formats
     */
    private static final Pattern OSGI_CONFIG_FILE_NAME_PATTERN = Pattern.compile(OSGI_CONFIG_NAME + "\\.(config|cfg\\.json|cfg)");

    @Nullable
    public Collection<ValidationMessage> done() {
        return null;
    }

    @Override
    public @Nullable Collection<ValidationMessage> validate(@NotNull DocViewNode2 node, @NotNull NodeContext nodeContext,
            boolean isRoot) {
        if ("sling:OsgiConfig".equals(node.getPrimaryType().orElse("")) && OSGI_CONFIG_NODE_NAME_PATTERN.matcher(Text.getName(nodeContext.getNodePath())).matches()) {
            Optional<DocViewProperty2> scriptsProperty = node.getProperty(NameFactoryImpl.getInstance().create(Name.NS_DEFAULT_URI, "scripts"));
            if (scriptsProperty.isPresent()) {
                try {
                    return validateStatements(scriptsProperty.get().getStringValues());
                } catch (IOException e) {
                    return Collections.singleton(new ValidationMessage(ValidationMessageSeverity.ERROR, "IOException while parsing " + nodeContext.getFilePath() +" : " + e.getMessage(), e));
                }
            }
        }
        return null;
    }


    @Override
    @Nullable
    public Collection<ValidationMessage> validateJcrData(@NotNull InputStream input, @NotNull Path filePath, @NotNull Path basePath,
            @NotNull Map<String, Integer> nodePathsAndLineNumbers) throws IOException {
        Map<String, Object> config = deserializeOsgiConfiguration(getType(filePath.getFileName().toString()), input);
        return validateConfig(config);
    }

    public boolean shouldValidateJcrData(@NotNull Path filePath, @NotNull Path basePath) {
        return isOsgiConfig(filePath);
    }

    private OsgiConfigurationSerialization getType(String fileName) {
        if (fileName.endsWith(".cfg.json")) {
            return OsgiConfigurationSerialization.CFG_JSON;
        } else if (fileName.endsWith(".config")) {
            return OsgiConfigurationSerialization.CONFIG;
        } else if (fileName.endsWith(".cfg")) {
            return OsgiConfigurationSerialization.CONFIG;
        } else {
            throw new IllegalArgumentException("Given file name " + fileName + " does not represent a known OSGi configuration serialization");
        }
    }

    private boolean isOsgiConfig(@NotNull Path filePath) {
        // TODO: check depth of config node
        String fileName = filePath.getFileName().toString();
        return OSGI_CONFIG_FILE_NAME_PATTERN.matcher(fileName).matches();
    }

    Map<String, Object> deserializeOsgiConfiguration(@NotNull OsgiConfigurationSerialization serializationType, @NotNull InputStream input) throws IOException {
        switch(serializationType) {
        case CONFIG:
            Properties properties = new Properties();
            properties.load(input);
            return convertToMap(properties);
        case CFG:
            return convertToMap(org.apache.felix.cm.file.ConfigurationHandler.read(input));
        case CFG_JSON:
            Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8);
            ConfigurationReader configReader = org.apache.felix.cm.json.Configurations.buildReader().build(reader);
            return convertToMap(configReader.readConfiguration());
        }
        return null;
    }

    private Collection<ValidationMessage> validateConfig(Map<String, Object> config) throws IOException {
        // https://sling.apache.org/documentation/bundles/repository-initialization.html#providing-repoinit-statements-from-osgi-factory-configurations
        // only evaluate scripts for now, references might have unresolvable URLs at the time of building (https://sling.apache.org/documentation/bundles/repository-initialization.html#references-to-urls-providing-raw-repoinit-statements)
        Object scripts = config.get("scripts");
        if (scripts == null) {
            return null;
        }
        if (scripts instanceof String[]) {
            return validateStatements(Arrays.asList((String[])scripts));
        } else if (scripts instanceof String) {
            return  validateStatements((String)scripts).map(Collections::singletonList).orElse(null);
        } else {
            return Collections.singletonList(new ValidationMessage(ValidationMessageSeverity.ERROR, "OSGi config property 'scripts' must be of type String or String[]"));
        }
    }

    private Collection<ValidationMessage> validateStatements(Collection<String> scripts) throws IOException {
        List<ValidationMessage> validationMsgs = new ArrayList<>();
        for (String statements : scripts) {
            validateStatements(statements).ifPresent(validationMsgs::add);
        }
        return validationMsgs;
    }

    private Optional<ValidationMessage> validateStatements(String statements) throws IOException {
        try (Reader reader = new StringReader(statements)) {
            parser.parse(reader);
        } catch (RepoInitParsingException e) {
            return Optional.of(new ValidationMessage(ValidationMessageSeverity.ERROR, "Invalid repoinit statement(s) detected: " + e.getMessage(), e));
        }
        return Optional.empty();
    }

    static Map<String, Object> convertToMap(Dictionary<String, ?> dictionary) {
        List<String> keys = Collections.list(dictionary.keys());
        return keys.stream().collect(Collectors.toMap(Function.identity(), dictionary::get)); 
    }

    static Map<String, Object> convertToMap(Properties properties) {
        return properties.entrySet().stream().collect(
                Collectors.toMap(
                  e -> String.valueOf(e.getKey()),
                  e -> String.valueOf(e.getValue()),
                  (prev, next) -> next, HashMap::new
              ));
    }
}
