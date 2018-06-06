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
package org.apache.sling.resource.filter.impl.predicates;

import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.resource.filter.impl.FilterParserConstants;

public class ComparisonPredicateFactory {

    public static Predicate<Resource> toPredicate(int kind, Function<Resource, Object> leftValue,
            Function<Resource, Object> rightValue) {
        switch (kind) {
        case FilterParserConstants.EQUAL:
            return ComparisonPredicates.is(leftValue, rightValue);
        case FilterParserConstants.NOT_EQUAL:
            return ComparisonPredicates.isNot(leftValue, rightValue);
        case FilterParserConstants.GREATER_THAN:
            return ComparisonPredicates.gt(leftValue, rightValue);
        case FilterParserConstants.GREATER_THAN_OR_EQUAL:
            return ComparisonPredicates.gte(leftValue, rightValue);
        case FilterParserConstants.LESS_THAN:
            return ComparisonPredicates.lt(leftValue, rightValue);
        case FilterParserConstants.LESS_THAN_OR_EQUAL:
            return ComparisonPredicates.lte(leftValue, rightValue);
        case FilterParserConstants.LIKE:
            return ComparisonPredicates.like(leftValue, rightValue);
        case FilterParserConstants.LIKE_NOT:
            return ComparisonPredicates.like(leftValue, rightValue).negate();
        case FilterParserConstants.CONTAINS:
            return ComparisonPredicates.contains(leftValue, rightValue);
        case FilterParserConstants.CONTAINS_NOT:
            return ComparisonPredicates.contains(leftValue, rightValue).negate();
        case FilterParserConstants.CONTAINS_ANY:
            return ComparisonPredicates.containsAny(leftValue, rightValue);
        case FilterParserConstants.CONTAINS_NOT_ANY:
            return ComparisonPredicates.containsAny(leftValue, rightValue).negate();
        case FilterParserConstants.IN:
            return ComparisonPredicates.in(leftValue, rightValue);
        case FilterParserConstants.NOT_IN:
            return ComparisonPredicates.in(leftValue, rightValue).negate();
        }
        return null;
    }

}