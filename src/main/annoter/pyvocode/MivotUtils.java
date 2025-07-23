package main.annoter.pyvocode;


import org.w3c.dom.*;
import javax.xml.parsers.*;

import java.lang.reflect.Field;
import java.util.*;
import main.annoter.pyvocode.MappingError;
import main.annoter.pyvocode.Glossary;

public class MivotUtils {

    public static List<Map.Entry<String, String>> validMappedDmRoles(Map<String, String> mappedRoles, String className) throws MappingError, IllegalArgumentException, IllegalAccessException {
    	Field field = null;
    	try {
    		field = Glossary.Roles.class.getDeclaredField(className);
    	} catch (NoSuchFieldException e) {
            throw new MappingError("Unknown or unimplemented class " + className);
        }

        List<String> modelRoles = (List<String>) field.get(null);
        List<Map.Entry<String, String>> realMapping = new ArrayList<>();

        for (Map.Entry<String, String> entry : mappedRoles.entrySet()) {
            String mappedRole = entry.getKey();
            String column = entry.getValue();

            if ("class".equals(mappedRole)) continue;

            boolean found = false;
            for (String leaf : modelRoles) {
                String fullRole = Glossary.ModelPrefix.MANGO + ":" + className + "." + leaf;
                if (fullRole.toLowerCase().endsWith("." + mappedRole.toLowerCase())) {
                    realMapping.add(new AbstractMap.SimpleEntry<>(fullRole, column));
                    found = true;
                    break;
                }
            }

            if (!found) {
                throw new MappingError("Class " + Glossary.ModelPrefix.MANGO + ":" + className +
                        " has no " + mappedRole + " attribute. Supported roles: " + modelRoles);
            }
        }
        return realMapping;
    }

    public static Map<String, Object> xmlToDict(Element element) {
        Map<String, Object> dict = new HashMap<>();

        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node attr = attributes.item(i);
            dict.put(attr.getNodeName(), attr.getNodeValue());
        }

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element)) continue;
            Element child = (Element) children.item(i);
            String tag = child.getTagName();
            String dmrole = child.getAttribute("dmrole");

            switch (tag) {
                case "ATTRIBUTE":
                    dict.put(dmrole, attributeToDict(child));
                    break;
                case "INSTANCE":
                    dict.put(dmrole, xmlToDict(child));
                    break;
                case "COLLECTION":
                    dict.put(dmrole, collectionToDict(child));
                    break;
            }
        }

        return dict;
    }

    public static Map<String, Object> attributeToDict(Element attr) {
        Map<String, Object> map = new HashMap<>();
        map.put("dmtype", attr.getAttribute("dmtype"));
        map.put("value", castTypeValue(attr.getAttribute("value"), attr.getAttribute("dmtype")));
        map.put("unit", attr.hasAttribute("unit") ? attr.getAttribute("unit") : null);
        map.put("ref", attr.hasAttribute("ref") ? attr.getAttribute("ref") : null);
        return map;
    }

    public static List<Map<String, Object>> collectionToDict(Element coll) {
        List<Map<String, Object>> list = new ArrayList<>();
        NodeList items = coll.getChildNodes();
        for (int i = 0; i < items.getLength(); i++) {
            if (items.item(i) instanceof Element) {
                list.add(xmlToDict((Element) items.item(i)));
            }
        }
        return list;
    }

    public static Object castTypeValue(String value, String dmtype) {
        if (dmtype == null || value == null || value.isEmpty()) return null;

        String ltype = dmtype.toLowerCase();
        String lval = value.toLowerCase();

        if (ltype.contains("bool")) {
            return lval.equals("1") || lval.equals("true");
        } else if (lval.matches("notset|noset|null|none|nan|--")) {
            return null;
        } else if (ltype.contains("float") || ltype.contains("real") || ltype.contains("double")) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return null;
            }
        } else {
            return value;
        }
    }

    public static String formatDmid(String dmid) {
        if (dmid == null) return "";
        return dmid.replace("/", "_").replace(".", "_").replace("-", "_");
    }

    public static String[] getRefOrLiteral(String valueOrRef) throws MappingError {
        if (valueOrRef == null) throw new MappingError("Cannot use null as attribute value");
        if (valueOrRef.startsWith("*")) {
            return new String[] {null, valueOrRef.substring(1)};
        } else {
            return new String[] {valueOrRef, null};
        }
    }

    public static String asLiteral(String identifier) {
        if (identifier != null && !identifier.startsWith("*")) {
            return "*" + identifier;
        }
        return identifier;
    }
}

