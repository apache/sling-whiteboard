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
package org.apache.sling.resource.filter.impl.node;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.sling.resource.filter.api.Visitor;

/**
 * 
 */
public class Node {

    public String text;
    public Node leftNode;
    public Node rightNode;
    public List<Node> children = Collections.emptyList();
    public int kind;

    /**
     * creates a node which represents a literal
     * 
     * @param value
     */
    public Node(int kind, String text) {
        this.kind = kind;
        this.text = text;
    }

    /**
     * creates a logical node
     * 
     * @param value
     */
    public Node(int kind, List<Node> children) {
        this.kind = kind;
        this.children = children;
    }

    /**
     * Node with children
     * 
     * @param value
     */
    public Node(int kind, String text, List<Node> children) {
        this.kind = kind;
        this.text = text;
        this.children = children;
    }

    /**
     * Node used for comparison
     * 
     * @param kind
     *            nominally of type comparison
     * @param operator
     *            defines the type of comparison
     * @param leftValue
     *            basis of comparison
     * @param rightValue
     *            to be compared to
     */
    public Node(int kind, String operator, Node leftValue, Node rightValue) {
        this.kind = kind;
        this.text = operator;
        this.leftNode = leftValue;
        this.rightNode = rightValue;
    }

    public <R> R accept(Visitor<R> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String toString() {
        return text
                + children.stream().map(Node::toString).collect(Collectors.joining(text, "(", ")"));
    }

    public <R> List<R> visitChildren(Visitor<R> visitor) {
        return children.stream().map(child -> child.accept(visitor)).collect(Collectors.toList());
    }

}
