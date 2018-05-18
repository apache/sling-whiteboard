/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.resource.stream.api.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.resource.stream.api.Context;
import org.apache.sling.resource.stream.api.Visitor;
import org.apache.sling.resource.stream.impl.FilterParserConstants;
import org.apache.sling.resource.stream.impl.node.Node;
import org.apache.sling.resource.stream.impl.predicates.Null;

public class ComparisonVisitor implements Visitor<Function<Resource, Object>> {

    private Context context;

    public ComparisonVisitor(Context context) {
        this.context = context;
        context.setComparionVisitor(this);
    }

    @Override
    public Function<Resource, Object> visit(Node node) {
        switch (node.kind) {
        case FilterParserConstants.FUNCTION_NAME:
            // will only get here in the case of the 'FUNCTION' switch case
            switch (node.text) {
            case "name":
                return Resource::getName;
            case "path":
                return Resource::getPath;
            default:
                Optional<BiFunction<List<Function<Resource, Object>>, Resource, Object>> temp = context
                        .getFunction(node.text);
                if (temp.isPresent()) {
                    final List<Function<Resource, Object>> arguments = node.visitChildren(this);
                    return resource -> temp.get().apply(arguments, resource);
                }
            }
            break;
        case FilterParserConstants.NULL:
            return resource -> new Null();
        case FilterParserConstants.NUMBER:
            Number numericValue = null; {
            String numberText = node.text;
            try {
                numericValue = Integer.valueOf(numberText);
            } catch (NumberFormatException nfe1) {
                try {
                    numericValue = new BigDecimal(numberText);
                } catch (NumberFormatException nfe2) {
                    // swallow
                }
            }
        }
            final Number numericReply = numericValue;
            return resource -> numericReply;
        case FilterParserConstants.OFFSETDATETIME:
            return resource -> OffsetDateTime.parse(node.text).toInstant();
        case FilterParserConstants.DATETIME:
            return resource -> LocalDateTime.parse(node.text).atOffset(ZoneOffset.UTC).toInstant();
        case FilterParserConstants.DATE:
            return resource -> LocalDate.parse(node.text).atStartOfDay(ZoneOffset.UTC).toInstant();
        case FilterParserConstants.PROPERTY:
            return resource -> {
                Object value = valueMapOf(resource).get(node.text);
                if (value instanceof Boolean) {
                    return value.toString();
                }
                if (value instanceof Calendar) {
                    return ((Calendar) value).toInstant();
                }
                return value;
            };
        default:
            return resource -> node.text;
        }
        return null;
    }

    private ValueMap valueMapOf(Resource resource) {
        if (resource == null || ResourceUtil.isNonExistingResource(resource)) {
            return ValueMap.EMPTY;
        }
        return resource.adaptTo(ValueMap.class);
    }

}
