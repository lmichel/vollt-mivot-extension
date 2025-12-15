package main.annoter.utils;

import main.annoter.mivot.MappingError;

/**
 * Utility helpers for MIVOT mapping processing.
 *
 * Provides small static helpers used across the annoter codebase for:
 * - casting string values to appropriate Java types according to a DM type
 * - formatting DMIDs into safe identifiers
 * - distinguishing between references and literal values marked with a '*'
 */
public class MivotUtils {

    /**
     * Convert a string value into an appropriate Java type based on the provided
     * DM type hint.
     *
     * Rules:
     * - If either parameter is null or the value is empty the method returns null.
     * - Types that contain "bool" are treated as booleans: returns a Boolean
     *   (true when value equals "1" or "true" ignoring case).
     * - Values matching the literal tokens notset|noset|null|none|nan|-- return null.
     * - Types containing "float", "real" or "double" will be parsed as a Double
     *   (returns null on parse failure).
     * - Otherwise the original String is returned.
     *
     * @param value the string value to cast
     * @param dmtype the DM type hint (may be null)
     * @return a Boolean, Double, String or null depending on the inputs
     */
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

    /**
     * Sanitize a DMID into a safe identifier by replacing path and punctuation
     * characters that may not be allowed in identifiers.
     *
     * Replacements: '/' -> '_', '.' -> '_', '-' -> '_'. If the input is null an
     * empty string is returned.
     *
     * @param dmid the DMID to format (may be null)
     * @return a sanitized DMID string (never null)
     */
    public static String formatDmid(String dmid) {
        if (dmid == null) return "";
        return dmid.replace("/", "_").replace(".", "_").replace("-", "_");
    }

    /**
     * Distinguish between a reference and a literal marker.
     *
     * This method interprets values starting with '*' as an explicit literal marker.
     * It returns a 2-element String array where the first element is the reference
     * (or null) and the second element is the literal (or null):
     * - If input starts with '*': returns { null, valueWithoutLeadingStar }
     * - Otherwise: returns { value, null }
     *
     * @param valueOrRef input value that may be a reference or a literal marker
     * @return a String[2] with [ref, literal]
     * @throws MappingError when the input is null (null is not allowed)
     */
    public static String[] getRefOrLiteral(String valueOrRef) throws MappingError {
        if (valueOrRef == null) throw new MappingError("Cannot use null as attribute value");
        if (valueOrRef.startsWith("*")) {
            return new String[] {null, valueOrRef.substring(1)};
        } else {
            return new String[] {valueOrRef, null};
        }
    }

    /**
     * Ensure an identifier is marked as a literal by prefixing it with '*'
     * when necessary.
     *
     * If the identifier is null it is returned as-is. If it already starts with
     * '*' it is returned unchanged. Otherwise the method returns '*' + identifier.
     *
     * @param identifier the identifier to mark as literal (may be null)
     * @return the literal-marked identifier or null if input was null
     */
    public static String asLiteral(String identifier) {
        if (identifier != null && !identifier.startsWith("*")) {
            return "*" + identifier;
        }
        return identifier;
    }
}