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
package org.apache.sling.resource.filter.api.impl;

import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.resource.filter.api.Context;
import org.apache.sling.resource.filter.api.Visitor;
import org.apache.sling.resource.filter.impl.FilterParserConstants;
import org.apache.sling.resource.filter.impl.node.Node;
import org.apache.sling.resource.filter.impl.predicates.ComparisonPredicateFactory;

/**
 * Visitor implementation that handles the high level handling of logic between
 * statements that define the comparisons that would be performed.
 * 
 * In practical terms this handles the "and" and "or" predicates, if it
 * encounters a COMPARISON node, it then hands off the internal ValueVisitor
 * 
 */
public class LogicVisitor implements Visitor<Predicate<Resource>> {

    private Context context;

    public LogicVisitor(Context context) {
        this.context = context;
        context.setLogicVisitor(this);
    }

    @Override
    public Predicate<Resource> visit(Node node) {
        switch (node.kind) {
        case FilterParserConstants.AND:
            return createAndPredicate(node);
        case FilterParserConstants.OR:
            return createOrPredicate(node);
        default:
            return createComparisonPredicate(node);
        }
    }

    private Predicate<Resource> createAndPredicate(Node node) {
        return node.children.stream().map(this::visit).reduce(null, (predicate, accumulator) -> {
            if (predicate == null) {
                return accumulator;
            }
            return accumulator.and(predicate);
        });
    }

    /**
     * Returns a predicate which consists of a series of Or statements
     * 
     * @param node
     * @param param
     * @return
     */
    private Predicate<Resource> createOrPredicate(Node node) {
        return node.children.stream().map(this::visit).reduce(null, (predicate, accumulator) -> {
            if (predicate == null) {
                return accumulator;
            }
            return accumulator.or(predicate);
        });
    }

    private Predicate<Resource> createComparisonPredicate(Node comparisonNode) {
        Function<Resource, Object> leftValue = comparisonNode.leftNode.accept(context.getComparisonVisitor());
        Function<Resource, Object> rightValue = comparisonNode.rightNode.accept(context.getComparisonVisitor());
        return ComparisonPredicateFactory.toPredicate(comparisonNode.kind, leftValue, rightValue);
    }

}