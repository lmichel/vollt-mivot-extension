package main.annoter.mivot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import main.annoter.cache.Cache;
import main.annoter.meta.Glossary;
import main.annoter.utils.MivotUtils;
import main.annoter.utils.XmlUtils;

/**
 * Factory class that retrieves photometric calibration and filter descriptions
 * from the SVO (via the FPS service) and adapts them into the internal
 * MIVOT representation used by the application.
 *
 * Responsibilities:
 * - Fetch FPS (Filter Profile Service) XML for a given filter identifier.
 * - Apply small fixes / normalization to the FPS XML so it matches expected
 *   MIVOT structure.
 * - Split the photometry filter out of a PhotCal instance and replace it with
 *   a REFERENCE node (so the filter can be declared once and referenced).
 * - Produce a "pretty" (indented/cleaned) XML string for downstream usage.
 *
 * Note: This class does not perform network retries or caching itself; the
 * FPS request is performed synchronously and exceptions are propagated as
 * MappingError (custom) or IO/parse exceptions.
 */
public class PhotCalFactory {
    
    /**
     * Retrieve a PhotCal instance adapted for MIVOT from the FPS service.
     *
     * This method performs the following steps:
     * 1. Map the provided short filter name to an SVO id using the project's
     *    glossary.
     * 2. Fetch the raw FPS XML for that SVO id.
     * 3. Apply small textual fixes required by downstream consumers.
     * 4. Split out the photometry filter declaration into a separate block and
     *    replace it in the PhotCal instance with a REFERENCE element that uses
     *    the provided filterId.
     * 5. Return a pretty-printed XML string.
     *
     * @param filterName short filter abbreviation understood by Glossary.Filters
     * @param photcalId the dmid to assign to the PhotCal instance in the output
     * @param filterId the id used to reference the photometry filter
     * @return pretty-printed MIVOT-compatible PhotCal XML string
     * @throws Exception on mapping errors, parsing errors, IO errors or when the
     *                   FPS response cannot be processed
     */
    public String getMivotPhotCal(String filterName, String photcalId, String filterId) throws Exception {
        
        String svoId = PhotCalFactory.getSVOId(filterName);
        String response = PhotCalFactory.getFPSResponse(svoId);
 
        // Apply small textual normalizations to make the FPS XML compatible
        // with our MIVOT code paths. These are intentionally string-based
        // replacements because the incoming FPS payloads are known to slightly
        // differ across versions/providers.
        response = response.replace(
            "<ATTRIBUTE dmrole=\"Phot:ZeroPoint.softeningParameter\" dmtype=\"ivoa:real\" value=\"\"/>",
            "<!-- ATTRIBUTE dmrole=\"Phot:ZeroPoint.softeningParameter\"" +
            " dmtype=\"ivoa:real\" value=\"\"/ -->");
        response = response.replace(
                "Phot:PhotCal.photometryFilter.bandwidth",
                "Phot:PhotometryFilter.bandwidth");        
        response = response.replace(
                " dmrole=\"\"",
                " dmid=\"" + photcalId + "\"");
        
        // Detach the photometry filter and replace it with a REFERENCE.
        response = splitFilterReference(response, filterId);
        
        String prettyString =  XmlUtils.prettyString(response);
        return prettyString;
    }
    
   /**
    * Retrieve a single PhotometryFilter definition for the given filter name
    * transformed to MIVOT format.
    *
    * Returns only the PhotometryFilter element (not the whole PhotCal). The
    * returned element will have its dmrole attribute transformed into a
    * dmid attribute using an internally generated filter id.
    *
    * @param filterName short filter abbreviation understood by Glossary.Filters
    * @return pretty XML string containing the PhotometryFilter element
    * @throws Exception if mapping/fetch/parse operations fail
    */
   public String getMivotPhotFilter(String filterName) throws Exception {
       
       String svoId = PhotCalFactory.getSVOId(filterName);
       String calId = MivotUtils.formatDmid(filterName);
       
       String filterId = "_photfilter_" + calId;
       
       String response = getFPSResponse(svoId);
 
       // Same small textual normalizations as above
       response = response.replace(
           "<ATTRIBUTE dmrole=\"Phot:ZeroPoint.softeningParameter\" dmtype=\"ivoa:real\" value=\"\"/>",
           "<!-- ATTRIBUTE dmrole=\"Phot:ZeroPoint.softeningParameter\"" +
           " dmtype=\"ivoa:real\" value=\"\"/ -->");
       response = response.replace(
               "Phot:PhotCal.photometryFilter.bandwidth",
               "Phot:PhotometryFilter.bandwidth");        
       
       DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
       dbf.setNamespaceAware(true);
       DocumentBuilder db = dbf.newDocumentBuilder();
       InputSource is = new InputSource(new StringReader(response.replace("<?xml version=\"1.0\"?>", "")));
       Document doc = db.parse(is);
       // Convert the photometry filter node into a string and switch its dmrole
       // to a dmid so it can be embedded independently.
       String prettyString = XmlUtils.nodeToString(findPhotometryFilter(doc)).replace(
               " dmrole=\"" + Glossary.FILTER_ROLE + "\"",
               " dmid=\"" + filterId + "\"");
       return prettyString;
   }
    
    /**
     * Remove the PhotometryFilter sub-node from the incoming PhotCal XML and
     * replace its occurrence inside the PhotCal instance with a REFERENCE
     * element. The original PhotometryFilter element is returned after the
     * reference so callers can emit the filter separately (if needed).
     *
     * This method performs DOM parsing and manipulation; it expects a single
     * INSTANCE element that contains the photometry filter with a specific
     * dmrole (Glossary.FILTER_ROLE).
     *
     * @param xmlString full FPS XML string representing a PhotCal instance
     * @param filterId id to use when converting the detached photometry filter
     *                 to a standalone element (placed in the dmid attribute)
     * @return concatenation of the modified parent instance XML and the
     *         detached PhotometryFilter element as a string
     * @throws Exception on parse errors or when the expected nodes aren't found
     */
    private String splitFilterReference(String xmlString, String filterId) throws Exception {

       DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
       dbf.setNamespaceAware(true);
       DocumentBuilder db = dbf.newDocumentBuilder();
       InputSource is = new InputSource(new StringReader(xmlString.replace("<?xml version=\"1.0\"?>", "")));
       Document doc = db.parse(is);
       Node photometryFilterNode = findPhotometryFilter(doc);
       
       // Remove the photometry filter node from its parent so we can replace
       // it by an internal REFERENCE element in the PhotCal instance.
       Node parentNode = photometryFilterNode.getParentNode();
       parentNode.removeChild(photometryFilterNode);

       // Create a REFERENCE node that points to the detached filter
       Element referenceNode = doc.createElement("REFERENCE");
       referenceNode.setAttribute("dmref", filterId);
       referenceNode.setAttribute("dmrole", "Phot:PhotCal.photometryFilter");
       doc.getElementsByTagName("INSTANCE").item(0).appendChild(referenceNode);

       // Add a human-readable comment warning that the photometric system may
       // vary from row to row - this mirrors the original project's behaviour.
       Comment comment = doc.createComment(" WARNING The photometric system may vary from a data row to another");
       parentNode.insertBefore(comment, parentNode.getFirstChild());
       
       // Return the modified parent instance as a string followed by the
       // detached PhotometryFilter element (with its dmrole replaced by a dmid).
       return  XmlUtils.nodeToString(parentNode) +
               "\n" +
               XmlUtils.nodeToString(photometryFilterNode).replace(
                       " dmrole=\"" + Glossary.FILTER_ROLE + "\"",
                       " dmid=\"" + filterId + "\"");
    }
    
    /**
     * Find the photometry filter node inside a parsed Document. The method
     * searches INSTANCE elements for one whose dmrole attribute equals the
     * configured Glossary.FILTER_ROLE.
     *
     * @param doc parsed DOM Document containing FPS response
     * @return the matching Node or null if none found
     * @throws Exception not expected in normal operation but declared for
     *                   callers that treat DOM errors uniformly
     */
    private static Node findPhotometryFilter(Document doc) throws Exception {
        NodeList instanceNodes = doc.getElementsByTagName("INSTANCE");
        for (int i = 0; i < instanceNodes.getLength(); i++) {
            Element el = (Element) instanceNodes.item(i);

            if (el.hasAttribute("dmrole")) {
                String role = el.getAttribute("dmrole");
                if (role.equals(Glossary.FILTER_ROLE)) {
                    return el;
                }
            }
        }
        return null;
    }
    
    /**
     * Extract a short description from an FPS error payload. This is a simple
     * helper that expects the message to be enclosed in <DESCRIPTION>...</DESCRIPTION>.
     * Using string-based extraction here keeps the method lightweight; callers
     * should only invoke it when they know the response contains the tag.
     *
     * @param xml FPS response XML containing a DESCRIPTION element
     * @return trimmed text inside DESCRIPTION, or possibly an empty string if tags missing
     */
    private static String extractFPSErrorDescription(String xml) {
        // Simple, intentionally minimal extraction instead of full XML parsing.
        int start = xml.indexOf("<DESCRIPTION>") + 13;
        int end = xml.indexOf("</DESCRIPTION>");
        return xml.substring(start, end).trim();
    }

    /**
     * Resolve a short filter name to an SVO identifier using the project's
     * Glossary. Throws MappingError if no mapping is present.
     *
     * @param filterName short filter abbreviation (e.g. "V", "B")
     * @return mapped SVO id string
     * @throws MappingError if the filter name is unknown
     */
    private static String getSVOId(String filterName) throws MappingError {
       String filter = Glossary.Filters.map.get(filterName);
       if( filter == null || filter.length() == 0 ) {
           throw new MappingError("No SVO filter identifier found for abreviation " + filterName);
       }
       return filter;
    }
    
    /**
     * Perform a synchronous HTTP GET request to the configured FPS URL for the
     * requested SVO id and return the raw response body as a String. If the
     * service replies with a non-200 status code or indicates a FPS-level
     * error inside the XML response, a MappingError is thrown.
     *
     * Note: This method performs no retries and will fully read the response
     * into memory (which is acceptable for the small responses expected from
     * the FPS service).
     *
     * @param svoId identifier appended to the FPS base URL
     * @return raw FPS XML response
     * @throws MalformedURLException on malformed URL construction
     * @throws IOException on network read errors
     * @throws MappingError on non-200 HTTP status or an FPS ERROR status
     */
    public static String getFPSResponse(String svoId) throws MalformedURLException, IOException, MappingError {
        
        String fpsUrl = Glossary.Url.FPS + svoId;
        Cache.logDebug("Connect " +  fpsUrl);
        HttpURLConnection connection = (HttpURLConnection) new URL(fpsUrl).openConnection();
        connection.setRequestMethod("GET");

        int httpCode = connection.getResponseCode();
        if (httpCode != 200) {
            connection.disconnect(); 
            throw new MappingError("FPS service error: " + httpCode);
        }
        InputStream is = connection.getInputStream();
        StringBuilder responseBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                responseBuilder.append(line);
            }
        }
        String response = responseBuilder.toString();
        connection.disconnect(); 

        // If FPS indicates an ERROR in its INFO block, extract and raise it.
        if (response.contains("<INFO name=\"QUERY_STATUS\" value=\"ERROR\">")) {
            String message = extractFPSErrorDescription(response);
            throw new MappingError("FPS service error: " + message);
        }
        return response;
    }
    
    /**
     * Produce a simplified PhotCal XML by removing verbose/irrelevant
     * INSTANCE elements (transmissionCurve, zeroPoint, magnitudeSystem and
     * bandwidth) and returning a cleaned XML string. The method wraps the
     * provided fragment inside a temporary root element to allow DOM parsing,
     * then strips the wrapper before returning the result.
     *
     * @param xmlPhotCal XML fragment representing a PhotCal
     * @return cleaned XML string
     * @throws TransformerException on XML transform errors
     * @throws ParserConfigurationException on parser configuration errors
     * @throws SAXException on XML parse errors
     * @throws IOException on IO errors when reading strings
     */
    public static String getSimplifiedPhotCal(String xmlPhotCal) throws TransformerException, ParserConfigurationException, SAXException, IOException {
        
        xmlPhotCal = xmlPhotCal.replaceAll("Phot\\:photometryFilter", "Phot:PhotometryFilter");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(
                new InputSource(
                        new StringReader("<BOIBOITE>\n" + xmlPhotCal +"\n</BOIBOITE>\n" )));

        NodeList personList = doc.getElementsByTagName("INSTANCE");
        String[] dmRolesToremove = {"Phot:PhotometryFilter.transmissionCurve",
                "Phot:PhotCal.zeroPoint", "Phot:PhotCal.magnitudeSystem",
                "Phot:PhotometryFilter.bandwidth"};
    
        for (String dmRoleToremove : dmRolesToremove) {
            for (int i = 0; i < personList.getLength(); i++) {
                Node node = personList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element person = (Element) node;
                    String id = person.getAttribute("dmrole");
                    if (dmRoleToremove.equals(id)
                            ) { 
                        person.getParentNode().removeChild(person);
                    }
                 }
            }       
        }
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        String xmlString = writer.toString();
        xmlString = xmlString.replace("<BOIBOITE>\n", "")
                .replace("\n</BOIBOITE>\n", "")
                .replaceAll("<\\?xml.*\\?>", "")
                .replaceAll("\n[ ]*\n", "\n")
                .replaceAll("\n[ ]*\n", "\n");
        return xmlString;
    }
}