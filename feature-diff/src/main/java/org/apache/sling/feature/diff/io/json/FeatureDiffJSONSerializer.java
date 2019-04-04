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
package org.apache.sling.feature.diff.io.json;

import static org.apache.commons.lang3.builder.ToStringStyle.NO_CLASS_NAME_STYLE;
import static java.util.Objects.requireNonNull;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

import javax.json.Json;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.sling.feature.diff.DiffSection;
import org.apache.sling.feature.diff.FeatureDiff;
import org.apache.sling.feature.diff.UpdatedItem;

public final class FeatureDiffJSONSerializer {

    private static final JsonGeneratorFactory GENERATOR_FACTORY = Json.createGeneratorFactory(Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true));

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss Z");

    public static void serializeFeatureDiff(FeatureDiff featureDiff, OutputStream output) {
        requireNonNull(output, "Impossible to serialize Feature Diff to a null stream");
        serializeFeatureDiff(featureDiff, new OutputStreamWriter(output));
    }

    public static void serializeFeatureDiff(FeatureDiff featureDiff, Writer writer) {
        requireNonNull(featureDiff, "Impossible to serialize null Feature Diff");
        requireNonNull(writer, "Impossible to serialize Feature Diff to a null stream");

        JsonGenerator generator = GENERATOR_FACTORY.createGenerator(writer);
        generator.writeStartObject();

        generator.write("vendor", "The Apache Software Foundation");
        generator.write("vendorURL", "http://www.apache.org/");
        generator.write("generator", "Apache Sling Feature Diff tool");
        generator.write("generatorURL", "https://github.com/apache/sling-org-apache-sling-feature-diff");
        generator.write("generatedOn", DATE_FORMAT.format(new Date()));
        generator.write("id", featureDiff.getCurrent().getId().toMvnId());
        generator.write("previousVersion", featureDiff.getPrevious().getId().getVersion());

        for (DiffSection diffSection : featureDiff.getSections()) {
            serializeDiffSection(diffSection, generator);
        }

        generator.writeEnd().close();
    }

    private static void serializeDiffSection(DiffSection diffSection, JsonGenerator generator) {
        generator.writeStartObject(diffSection.getId());

        if (diffSection.hasRemoved()) {
            writeArray("removed", diffSection.getRemoved(), generator);
        }

        if (diffSection.hasAdded()) {
            writeArray("added", diffSection.getAdded(), generator);
        }

        if (diffSection.hasUpdatedItems() || diffSection.hasUpdates()) {
            generator.writeStartObject("updated");

            for (UpdatedItem<?> updatedItem : diffSection.getUpdatedItems()) {
                generator.writeStartObject(updatedItem.getId());
                writeValue("previous", updatedItem.getPrevious(), generator);
                writeValue("current", updatedItem.getCurrent(), generator);
                generator.writeEnd();
            }

            for (DiffSection updatesDiffSection : diffSection.getUpdates()) {
                serializeDiffSection(updatesDiffSection, generator);
            }

            generator.writeEnd();
        }

        generator.writeEnd();
    }

    private static void writeArray(String name, Iterable<String> values, JsonGenerator generator) {
        generator.writeStartArray(name);

        for (String value : values) {
            generator.write(value);
        }

        generator.writeEnd();
    }

    // TODO find a faster and more elegant implementation
    private static <T> void writeValue(String key, T value, JsonGenerator generator) {
        if (value == null) {
            generator.write(key, JsonValue.NULL);
        } else if (value instanceof Boolean) {
            generator.write(key, ((Boolean) value).booleanValue());
        } else if (value instanceof BigDecimal) {
            generator.write(key, (BigDecimal) value);
        } else if (value instanceof BigInteger) {
            generator.write(key, (BigInteger) value);
        } else if (value instanceof Integer) {
            generator.write(key, (Integer) value);
        } else if (value instanceof Long) {
            generator.write(key, (Long) value);
        } else if (value instanceof Double) {
            generator.write(key, (Double) value);
        } else if (value instanceof String) {
            generator.write(key, (String) value);
        } else {
            generator.write(key, ReflectionToStringBuilder.toString(value, NO_CLASS_NAME_STYLE));
        }
    }

    private FeatureDiffJSONSerializer() {
        // this class can not be instantiated
    }

}
