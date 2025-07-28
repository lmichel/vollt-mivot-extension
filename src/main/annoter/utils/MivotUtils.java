package main.annoter.utils;

import main.annoter.mivot.MappingError;

public class MivotUtils {

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

