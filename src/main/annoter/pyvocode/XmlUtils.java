package main.annoter.pyvocode;

import java.io.StringWriter;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

import javax.xml.parsers.*;

public class XmlUtils {
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
}
