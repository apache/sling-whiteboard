/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.apache.sling.models.persist.impl.util;

/**
 * Simple assertion functions.
 *
 * Part of the code of this class has been borrowed from the open-source project
 * <code>jerry-core</code> from https://github.com/sangupta/jerry-core.
 *
 */
public class AssertUtils {
    private AssertUtils() {
        // Utility class cannot be instantiated
    }

    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
    
    public static boolean isNotEmpty(Object[] array) {
        return !isEmpty(array);
    }

}
