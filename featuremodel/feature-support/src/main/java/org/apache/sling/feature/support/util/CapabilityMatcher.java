/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.feature.support.util;

import static org.osgi.framework.Constants.RESOLUTION_DIRECTIVE;
import static org.osgi.framework.Constants.RESOLUTION_OPTIONAL;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;
import org.osgi.resource.Capability;
import org.osgi.resource.Requirement;

public class CapabilityMatcher
{
    static boolean matches(Capability cap, SimpleFilter sf)
    {
        return matchesInternal(cap, sf) && matchMandatory(cap, sf);
    }

    private static boolean matchesInternal(Capability cap, SimpleFilter sf)
    {
        boolean matched = true;

        if (sf.getOperation() == SimpleFilter.MATCH_ALL)
        {
            matched = true;
        }
        else if (sf.getOperation() == SimpleFilter.AND)
        {
            // Evaluate each subfilter against the remaining capabilities.
            // For AND we calculate the intersection of each subfilter.
            // We can short-circuit the AND operation if there are no
            // remaining capabilities.
            List<SimpleFilter> sfs = (List<SimpleFilter>) sf.getValue();
            for (int i = 0; matched && (i < sfs.size()); i++)
            {
                matched = matchesInternal(cap, sfs.get(i));
            }
        }
        else if (sf.getOperation() == SimpleFilter.OR)
        {
            // Evaluate each subfilter against the remaining capabilities.
            // For OR we calculate the union of each subfilter.
            matched = false;
            List<SimpleFilter> sfs = (List<SimpleFilter>) sf.getValue();
            for (int i = 0; !matched && (i < sfs.size()); i++)
            {
                matched = matchesInternal(cap, sfs.get(i));
            }
        }
        else if (sf.getOperation() == SimpleFilter.NOT)
        {
            // Evaluate each subfilter against the remaining capabilities.
            // For OR we calculate the union of each subfilter.
            List<SimpleFilter> sfs = (List<SimpleFilter>) sf.getValue();
            for (int i = 0; i < sfs.size(); i++)
            {
                matched = !(matchesInternal(cap, sfs.get(i)));
            }
        }
        else
        {
            matched = false;
            Object lhs = cap.getAttributes().get(sf.getName());
            if (lhs != null)
            {
                matched = compare(lhs, sf.getValue(), sf.getOperation());
            }
        }

        return matched;
    }

    private static boolean matchMandatory(Capability cap, SimpleFilter sf)
    {
        Map<String, Object> attrs = cap.getAttributes();
        for (Entry<String, Object> entry : attrs.entrySet())
        {
            if (isAttributeMandatory(cap, entry.getKey())
                    && !matchMandatoryAttribute(entry.getKey(), sf))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean matchMandatoryAttribute(String attrName, SimpleFilter sf)
    {
        if ((sf.getName() != null) && sf.getName().equals(attrName))
        {
            return true;
        }
        else if (sf.getOperation() == SimpleFilter.AND)
        {
            List list = (List) sf.getValue();
            for (int i = 0; i < list.size(); i++)
            {
                SimpleFilter sf2 = (SimpleFilter) list.get(i);
                if ((sf2.getName() != null)
                        && sf2.getName().equals(attrName))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static final Class<?>[] STRING_CLASS = new Class[] { String.class };
    private static final String VALUE_OF_METHOD_NAME = "valueOf";

    private static boolean compare(Object lhs, Object rhsUnknown, int op)
    {
        if (lhs == null)
        {
            return false;
        }

        // If this is a PRESENT operation, then just return true immediately
        // since we wouldn't be here if the attribute wasn't present.
        if (op == SimpleFilter.PRESENT)
        {
            return true;
        }

        //Need a special case here when lhs is a Version and rhs is a VersionRange
        //Version is comparable so we need to check this first
        if(lhs instanceof Version && op == SimpleFilter.EQ)
        {
            Object rhs = null;
            try
            {
                rhs = coerceType(lhs, (String) rhsUnknown);
            }
            catch (Exception ex)
            {
                //Do nothing will check later if rhs is null
            }

            if(rhs != null && rhs instanceof VersionRange)
            {
                return ((VersionRange)rhs).includes((Version)lhs);
            }
        }

        // If the type is comparable, then we can just return the
        // result immediately.
        if (lhs instanceof Comparable)
        {
            // Spec says SUBSTRING is false for all types other than string.
            if ((op == SimpleFilter.SUBSTRING) && !(lhs instanceof String))
            {
                return false;
            }

            Object rhs;
            if (op == SimpleFilter.SUBSTRING)
            {
                rhs = rhsUnknown;
            }
            else
            {
                try
                {
                    rhs = coerceType(lhs, (String) rhsUnknown);
                }
                catch (Exception ex)
                {
                    return false;
                }
            }

            switch (op)
            {
                case SimpleFilter.EQ :
                    try
                    {
                        return (((Comparable) lhs).compareTo(rhs) == 0);
                    }
                    catch (Exception ex)
                    {
                        return false;
                    }
                case SimpleFilter.GTE :
                    try
                    {
                        return (((Comparable) lhs).compareTo(rhs) >= 0);
                    }
                    catch (Exception ex)
                    {
                        return false;
                    }
                case SimpleFilter.LTE :
                    try
                    {
                        return (((Comparable) lhs).compareTo(rhs) <= 0);
                    }
                    catch (Exception ex)
                    {
                        return false;
                    }
                case SimpleFilter.APPROX :
                    return compareApproximate(lhs, rhs);
                case SimpleFilter.SUBSTRING :
                    return SimpleFilter.compareSubstring((List<String>) rhs, (String) lhs);
                default:
                    throw new RuntimeException(
                            "Unknown comparison operator: " + op);
            }
        }
        // Booleans do not implement comparable, so special case them.
        else if (lhs instanceof Boolean)
        {
            Object rhs;
            try
            {
                rhs = coerceType(lhs, (String) rhsUnknown);
            }
            catch (Exception ex)
            {
                return false;
            }

            switch (op)
            {
                case SimpleFilter.EQ :
                case SimpleFilter.GTE :
                case SimpleFilter.LTE :
                case SimpleFilter.APPROX :
                    return (lhs.equals(rhs));
                default:
                    throw new RuntimeException(
                            "Unknown comparison operator: " + op);
            }
        }

        // If the LHS is not a comparable or boolean, check if it is an
        // array. If so, convert it to a list so we can treat it as a
        // collection.
        if (lhs.getClass().isArray())
        {
            lhs = convertArrayToList(lhs);
        }

        // If LHS is a collection, then call compare() on each element
        // of the collection until a match is found.
        if (lhs instanceof Collection)
        {
            for (Iterator iter = ((Collection) lhs).iterator(); iter.hasNext(); )
            {
                if (compare(iter.next(), rhsUnknown, op))
                {
                    return true;
                }
            }

            return false;
        }

        // Spec says SUBSTRING is false for all types other than string.
        if ((op == SimpleFilter.SUBSTRING) && !(lhs instanceof String))
        {
            return false;
        }

        // Since we cannot identify the LHS type, then we can only perform
        // equality comparison.
        try
        {
            return lhs.equals(coerceType(lhs, (String) rhsUnknown));
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    private static boolean compareApproximate(Object lhs, Object rhs)
    {
        if (rhs instanceof String)
        {
            return removeWhitespace((String) lhs)
                    .equalsIgnoreCase(removeWhitespace((String) rhs));
        }
        else if (rhs instanceof Character)
        {
            return Character.toLowerCase(((Character) lhs))
                    == Character.toLowerCase(((Character) rhs));
        }
        return lhs.equals(rhs);
    }

    private static String removeWhitespace(String s)
    {
        StringBuffer sb = new StringBuffer(s.length());
        for (int i = 0; i < s.length(); i++)
        {
            if (!Character.isWhitespace(s.charAt(i)))
            {
                sb.append(s.charAt(i));
            }
        }
        return sb.toString();
    }

    private static Object coerceType(Object lhs, String rhsString) throws Exception
    {
        // If the LHS expects a string, then we can just return
        // the RHS since it is a string.
        if (lhs.getClass() == rhsString.getClass())
        {
            return rhsString;
        }

        // Try to convert the RHS type to the LHS type by using
        // the string constructor of the LHS class, if it has one.
        Object rhs = null;
        try
        {
            // The Character class is a special case, since its constructor
            // does not take a string, so handle it separately.
            if (lhs instanceof Character)
            {
                rhs = new Character(rhsString.charAt(0));
            }
            else if(lhs instanceof Version && rhsString.indexOf(',') >= 0)
            {
                rhs = VersionRange.valueOf(rhsString);
            }
            else
            {
                // Spec says we should trim number types.
                if ((lhs instanceof Number) || (lhs instanceof Boolean))
                {
                    rhsString = rhsString.trim();
                }

                try
                {
                    // Try to find a suitable static valueOf method
                    Method valueOfMethod = lhs.getClass().getDeclaredMethod(VALUE_OF_METHOD_NAME, STRING_CLASS);
                    if (valueOfMethod.getReturnType().isAssignableFrom(lhs.getClass())
                            && ((valueOfMethod.getModifiers() & Modifier.STATIC) > 0))
                    {
                        valueOfMethod.setAccessible(true);
                        rhs = valueOfMethod.invoke(null, new Object[] { rhsString });
                    }
                }
                catch (Exception ex)
                {
                    // Static valueOf fails, try the next conversion mechanism
                }

                if (rhs == null)
                {
                    Constructor ctor = lhs.getClass().getConstructor(STRING_CLASS);
                    ctor.setAccessible(true);
                    rhs = ctor.newInstance(new Object[] { rhsString });
                }
            }
        }
        catch (Exception ex)
        {
            throw new Exception(
                    "Could not instantiate class "
                            + lhs.getClass().getName()
                            + " from string constructor with argument '"
                            + rhsString + "' because " + ex);
        }

        return rhs;
    }

    /**
     * This is an ugly utility method to convert an array of primitives
     * to an array of primitive wrapper objects. This method simplifies
     * processing LDAP filters since the special case of primitive arrays
     * can be ignored.
     * @param array An array of primitive types.
     * @return An corresponding array using pritive wrapper objects.
     **/
    private static List convertArrayToList(Object array)
    {
        int len = Array.getLength(array);
        List list = new ArrayList(len);
        for (int i = 0; i < len; i++)
        {
            list.add(Array.get(array, i));
        }
        return list;
    }


    public static  boolean matches(Capability capability, Requirement requirement) {
        if (requirement.getNamespace().equals(capability.getNamespace())) {
            String filter = requirement.getDirectives().get(Constants.FILTER_DIRECTIVE);
            if (filter != null) {
                return matches(capability, SimpleFilter.parse(filter));
            }
            return true;
        }
        return false;
    }

    public static boolean isOptional(Requirement requirement) {
        return RESOLUTION_OPTIONAL.equals(requirement. getDirectives().get(RESOLUTION_DIRECTIVE));
    }

    public static boolean isAttributeMandatory(Capability capability, String name)
    {
        String value = capability.getDirectives().get(Constants.MANDATORY_DIRECTIVE);
        if (value != null)
        {
            return parseDelimitedString(value, ",").contains(name);
        }
        return false;
    }

    public static List<String> parseDelimitedString(String value, String delim)
    {
        return CapabilityMatcher.parseDelimitedString(value, delim, true);
    }

    /**
     * Parses delimited string and returns an array containing the tokens. This
     * parser obeys quotes, so the delimiter character will be ignored if it is
     * inside of a quote. This method assumes that the quote character is not
     * included in the set of delimiter characters.
     * @param value the delimited string to parse.
     * @param delim the characters delimiting the tokens.
     * @return a list of string or an empty list if there are none.
     **/
    public static List<String> parseDelimitedString(String value, String delim, boolean trim)
    {
        if (value == null)
        {
            value = "";
        }

        List<String> list = new ArrayList<>();

        int CHAR = 1;
        int DELIMITER = 2;
        int STARTQUOTE = 4;
        int ENDQUOTE = 8;

        StringBuffer sb = new StringBuffer();

        int expecting = (CHAR | DELIMITER | STARTQUOTE);

        boolean isEscaped = false;
        for (int i = 0; i < value.length(); i++)
        {
            char c = value.charAt(i);

            boolean isDelimiter = (delim.indexOf(c) >= 0);

            if (!isEscaped && (c == '\\'))
            {
                isEscaped = true;
                continue;
            }

            if (isEscaped)
            {
                sb.append(c);
            }
            else if (isDelimiter && ((expecting & DELIMITER) > 0))
            {
                if (trim)
                {
                    list.add(sb.toString().trim());
                }
                else
                {
                    list.add(sb.toString());
                }
                sb.delete(0, sb.length());
                expecting = (CHAR | DELIMITER | STARTQUOTE);
            }
            else if ((c == '"') && ((expecting & STARTQUOTE) > 0))
            {
                sb.append(c);
                expecting = CHAR | ENDQUOTE;
            }
            else if ((c == '"') && ((expecting & ENDQUOTE) > 0))
            {
                sb.append(c);
                expecting = (CHAR | STARTQUOTE | DELIMITER);
            }
            else if ((expecting & CHAR) > 0)
            {
                sb.append(c);
            }
            else
            {
                throw new IllegalArgumentException("Invalid delimited string: " + value);
            }

            isEscaped = false;
        }

        if (sb.length() > 0)
        {
            if (trim)
            {
                list.add(sb.toString().trim());
            }
            else
            {
                list.add(sb.toString());
            }
        }

        return list;
    }
}
