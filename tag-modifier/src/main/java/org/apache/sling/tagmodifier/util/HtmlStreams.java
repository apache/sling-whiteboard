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
package org.apache.sling.tagmodifier.util;

import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.sling.tagmodifier.AttrValue;
import org.apache.sling.tagmodifier.Element;
import org.apache.sling.tagmodifier.impl.tag.StartTag;

public class HtmlStreams {
    
    private HtmlStreams() {
    }

    public static final  Function<Element, String> TO_HTML = element ->{
        StringBuilder sb = new StringBuilder();
        switch (element.getType()) {
        case COMMENT:
            sb.append("<!--");
            sb.append(element.getValue());
            sb.append("-->");
            break;
        case DOCTYPE:
            sb.append("<!");
            sb.append(element.getValue());
            sb.append(">");
            break;
        case END_TAG:
            sb.append("</");
            sb.append(element.getValue());
            sb.append('>');
            break;
        case EOF:
            break;
        case START_TAG:
            sb.append('<');
            sb.append(element.getValue());
            StartTag tag = (StartTag) element;
            if (tag.hasAttributes()) {
                sb.append(' ');
                sb.append(tag.getAttributes().entrySet().stream().map(entry -> {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append(entry.getKey());
                    AttrValue value = entry.getValue();
                    if (!value.isEmpty()) {
                        sb2.append("=");
                        sb2.append(value.quoteIfNeeded());
                    } 
                    return sb2.toString();
                }).collect(Collectors.joining(" ")));
            }
            sb.append('>');
            break;
        case TEXT:
            sb.append(element.toString());
        }
        return sb.toString();
    };
}
