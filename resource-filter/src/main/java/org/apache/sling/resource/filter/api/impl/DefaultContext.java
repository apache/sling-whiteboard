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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.resource.filter.api.Context;
import org.apache.sling.resource.filter.api.Visitor;

public class DefaultContext implements Context {

    private Map<String, BiFunction<Object[], Resource, Object>> functions = new HashMap<>();

    private Map<String, Object> arguments = new HashMap<>();

    private Visitor<Predicate<Resource>> logicVisitor;

    private Visitor<Function<Resource, Object>> comparisonVisitor;

    @Override
    public Context addFunction(String name, BiFunction<Object[], Resource, Object> functionImpl) {
        functions.put(name, functionImpl);
        return this;
    }

    @Override
    public Context removeFunction(String name) {
        functions.remove(name);
        return this;
    }

    @Override
    public Context addArgument(String name, Object object) {
        arguments.put(name, object);
        return this;
    }

    @Override
    public Visitor<Predicate<Resource>> getLogicVisitor() {
        return logicVisitor;
    }

    @Override
    public Visitor<Function<Resource, Object>> getComparisonVisitor() {
        return comparisonVisitor;
    }

    @Override
    public void setLogicVisitor(Visitor<Predicate<Resource>> logicVisitor) {
        this.logicVisitor = logicVisitor;

    }

    @Override
    public void setComparionVisitor(Visitor<Function<Resource, Object>> comparisonVisitor) {
        this.comparisonVisitor = comparisonVisitor;

    }

    @Override
    public Optional<BiFunction<Object[], Resource, Object>> getFunction(String text) {
        return Optional.ofNullable(functions.get(text));
    }

    @Override
    public Optional<Object> getArgument(String text) {
        return Optional.ofNullable(arguments.get(text));
    }

}
