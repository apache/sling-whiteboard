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
package org.apache.sling.resource.stream.parser.visitor;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.resource.stream.parser.FilterParserConstants;
import org.apache.sling.resource.stream.parser.api.ResourceFilterFunction;
import org.apache.sling.resource.stream.parser.api.Visitor;
import org.apache.sling.resource.stream.parser.impl.InstantProvider;
import org.apache.sling.resource.stream.parser.node.Node;
import org.apache.sling.resource.stream.parser.predicates.Null;

public class ComparisonVisitor implements Visitor<Function<Resource, Object>> {

	private Map<String, ResourceFilterFunction> functions = new HashMap<>();
	
	private ResourceFilterFunction instant = new InstantProvider();

	@Override
	public Function<Resource, Object> visit(Node node) {
		switch (node.kind) {
		case FilterParserConstants.FUNCTION_NAME:
			break;
		case FilterParserConstants.NULL:
			return resource -> new Null();
		case FilterParserConstants.NUMBER:
			Number numericValue = null;
			{
				String numberText = node.text;
				try {
					numericValue = Integer.valueOf(numberText);
				} catch (NumberFormatException nfe1) {
					try {
						numericValue = new BigDecimal(numberText);
					} catch (NumberFormatException nfe2) {
					 //swallow
					}
				}
			}
			final Number numericReply = numericValue;
			return resource -> numericReply;
		case FilterParserConstants.PROPERTY:
			return resource -> {
				Object value = valueMapOf(resource).get(node.text);
				if (value instanceof Boolean) {
					return value.toString();
				}
				if (value instanceof Calendar){
					return ((Calendar)value).toInstant();
				}
				return value;
			};
		default:
			return resource -> node.text;
		}
		// will only get here in the case of the 'FUNCTION' switch case
		switch (node.text) {
		case "name":
			return resource -> resource.getName();
		case "date":
			return instant.provision(node.visitChildren(this));
		case "path":
			return resource -> resource.getPath();
		default:
			ResourceFilterFunction temp = functions.get(node.text);
			if (temp !=  null){
				return temp.provision(node.visitChildren(this));
			}
			
		}
		return null;
	}

	public ResourceFilterFunction registerFunction(String functionName, ResourceFilterFunction function) {
		return this.functions.put(functionName, function);
	}

	public ResourceFilterFunction removeFunction(String functionName) {
		return this.functions.remove(functionName);
	}
	
	private ValueMap valueMapOf(Resource resource){
		if (resource == null || ResourceUtil.isNonExistingResource(resource)){
			return ValueMap.EMPTY;
		}
		 return resource.adaptTo(ValueMap.class);
	}

}
