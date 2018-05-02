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
package org.apache.sling.resource.stream;

import java.io.ByteArrayInputStream;
import java.util.function.Predicate;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.resource.stream.api.Context;
import org.apache.sling.resource.stream.api.impl.ComparisonVisitor;
import org.apache.sling.resource.stream.api.impl.DefaultContext;
import org.apache.sling.resource.stream.api.impl.LogicVisitor;
import org.apache.sling.resource.stream.impl.FilterParser;
import org.apache.sling.resource.stream.impl.ParseException;
import org.apache.sling.resource.stream.impl.node.Node;

public class ResourceFilter implements Predicate<Resource> {

	private Predicate<Resource> parsedPredicate;
	
	private Context context;
	
	public ResourceFilter(String filter) throws ParseException {
		Node rootNode = new FilterParser(new ByteArrayInputStream(filter.getBytes())).parse();
		this.parsedPredicate = rootNode.accept(getContext().getLogicVisitor());
	}

	@Override
	public boolean test(Resource resource) {
		return parsedPredicate.test(resource);
	}
	
	public Context getContext() {
		if (context == null) {
			context = new DefaultContext();
			new LogicVisitor(context);
			new ComparisonVisitor(context);
		}
		return context;
	}
	
	public void setContext(Context context) {
		this.context = context;
	}
	
}
