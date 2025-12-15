package main.annoter.utils;

import java.io.StringWriter;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

import javax.xml.parsers.*;

/**
 * XML utility helpers used to pretty-print and convert DOM nodes to strings.
 *
 * Two pretty-printing helpers are provided:
 * - prettyFormat: uses a DOM parser + Transformer to produce indented XML and
 *   returns the formatted string (may throw on parse/transform errors).
 * - prettyString: a lightweight, dependency-free formatting function that
 *   inserts line breaks between tags and indents using simple heuristics.
 *
 * Also provides nodeToString() which converts a DOM Node to a pretty-printed
 * XML string.
 */
public class XmlUtils {
    /**
     * Parse an XML string and return a pretty-printed representation using the
     * XML Transformer pipeline.
     *
     * This method is more robust than {@link #prettyString(String)} because it
     * actually parses the XML into a DOM and relies on the Transformer to
     * indent elements properly. However, it may throw exceptions on invalid
     * XML input or when the transformation fails.
     *
     * @param input raw XML string (UTF-8)
     * @return the pretty-printed XML string with indentation
     * @throws Exception when parsing or transformation fails (parser or transformer issues)
     */
    public static String prettyFormat(String input) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new java.io.ByteArrayInputStream(input.getBytes("UTF-8")));

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(sw));
        return sw.toString();
    }
    
    /**
     * Produce a simple pretty-printed version of an XML string using lightweight
     * heuristics: it inserts line breaks between tags and indents using two
     * spaces per level.
     *
     * This method does not validate or parse the XML; it is fast and useful for
     * generating human-readable output for already well-formed XML snippets.
     * It may produce imperfect results for complex constructs such as comments,
     * processing instructions, CDATA sections or mixed content models.
     *
     * @param xml the XML to pretty-print (assumed to be well-formed)
     * @return a formatted XML string with line breaks and indentation
     */
    public static String prettyString(String xml) {
        xml = xml.replaceAll("><", ">\n<"); // Line break between tags

        String[] lines = xml.split("\n");
        StringBuilder prettyXml = new StringBuilder();
        int indentLevel = 0;
        String indent = "  ";

        for (String line : lines) {
            line = line.trim();

            if (line.matches("</.+>") ) {
                // Closing tag: decrease indent
                indentLevel--;
            }

            for (int i = 0; i < indentLevel; i++) {
                prettyXml.append(indent);
            }
            prettyXml.append(line).append("\n");

            if (line.matches("<[^/?!][^>]*[^/]?>")) {
                // Opening tag (not self-closing): increase indent
                indentLevel++;
            }
            // self-closing: decrease indent
            if( line.indexOf("/>") != -1) {
                indentLevel--;
            }
        }
        return prettyXml.toString();
    }
    
    /**
     * Convert a DOM Node into a pretty-printed XML string.
     *
     * The method uses a Transformer to serialize the node and then applies
     * {@link #prettyString(String)} to produce a human-readable representation.
     *
     * @param node DOM node to convert (Document, Element, etc.)
     * @return pretty-printed XML representation of the node
     * @throws Exception when transformation fails
     */
    public static String nodeToString(Node node) throws Exception {
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(node), new StreamResult(writer));

        return prettyString(writer.toString());
    }

}