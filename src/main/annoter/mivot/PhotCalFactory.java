package main.annoter.mivot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import main.annoter.meta.Glossary;
import main.annoter.utils.MivotUtils;
import main.annoter.utils.XmlUtils;

public class PhotCalFactory {
    private static final Logger LOGGER = Logger.getLogger(MivotAnnotations.class.getName());
    private  List<String> PHOTCAL_IDS = new ArrayList<String>();
    private  List<String> PHOTFILTER_IDS = new ArrayList<String>();
    private static Map<String, String> PHOTCAL_CACHE = new LinkedHashMap<String, String>();
    private static Map<String, String> FILTER_CACHE = new LinkedHashMap<String, String>();
    

    
    public String getMivotPhotCal(String filterName, String photcalId, String filterId) throws Exception {
    	
    	if (PHOTCAL_CACHE.containsKey(photcalId) ) {
    		System.out.println("========= PHOTCAL_CACHE " + photcalId);
    		return PHOTCAL_CACHE.get(photcalId);
    	}
    	String svoId = getSVOId(filterName);
              
        if( PHOTCAL_IDS.contains(photcalId) ) {
			return "";
		}

        String response = getFPSResponse(svoId);
 
        // fix some FPS tweaks
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
        
        PHOTCAL_IDS.add(photcalId);
        response = splitFilterReference(response, filterId);
        
        String prettyString =  XmlUtils.prettyString(response);
        System.out.println("put " + photcalId);
        PHOTCAL_CACHE.put(photcalId, prettyString);
        return prettyString;
    }
    
   public String getMivotPhotFilter(String filterName) throws Exception {
    	
    	String svoId = PhotCalFactory.getSVOId(filterName);
        String calId = MivotUtils.formatDmid(filterName);
        
        String filterId = "_photfilter_" + calId;
    	if (FILTER_CACHE.containsKey(filterId)  ) {
    		System.out.println("========= FILTER_CACHE "+ filterId);
    		return FILTER_CACHE.get(filterId);
    	}
        
        if( PHOTFILTER_IDS.contains(filterId) ) {
			return "";
		}

        String response = getFPSResponse(svoId);
 
        // fix some FPS tweaks
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
		PHOTFILTER_IDS.add(filterId);
		String prettyString = XmlUtils.nodeToString(findPhotometryFilter(doc)).replace(
				" dmrole=\"" + Glossary.FILTER_ROLE + "\"",
				" dmid=\"" + filterId + "\"");
		FILTER_CACHE.put(filterId, prettyString);
        return prettyString;
    }
    
    private String splitFilterReference(String xmlString, String filterId) throws Exception {

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		InputSource is = new InputSource(new StringReader(xmlString.replace("<?xml version=\"1.0\"?>", "")));
		Document doc = db.parse(is);
		Node photometryFilterNode = findPhotometryFilter(doc);
		
		// Remove the photometry filter node
		Node parentNode = photometryFilterNode.getParentNode();
		parentNode.removeChild(photometryFilterNode);

		// Create a REFERENCE node
		Element referenceNode = doc.createElement("REFERENCE");
		referenceNode.setAttribute("dmid", filterId);
		referenceNode.setAttribute("dmrole", "Phot:PhotCal.photometryFilter");
        doc.getElementsByTagName("INSTANCE").item(0).appendChild(referenceNode);

		// Append the REFERENCE node to the parent
        Comment comment = doc.createComment(" WARNING The photometric system may vary from a data row to another");
		parentNode.insertBefore(comment, parentNode.getFirstChild());
		
        if( PHOTFILTER_IDS.contains(filterId) ) {
        	return  XmlUtils.nodeToString(parentNode);
        } else {
			PHOTFILTER_IDS.add(filterId);
			return  XmlUtils.nodeToString(parentNode) +
				"\n" +
				XmlUtils.nodeToString(photometryFilterNode).replace(
						" dmrole=\"" + Glossary.FILTER_ROLE + "\"",
						" dmid=\"" + filterId + "\"");
        }
	}
    
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
    
    private static String extractFPSErrorDescription(String xml) {
        // Simple regex extraction in Java, or XML parsing.
        int start = xml.indexOf("<DESCRIPTION>") + 13;
        int end = xml.indexOf("</DESCRIPTION>");
        return xml.substring(start, end).trim();
    }

    private static String getSVOId(String filterName) throws MappingError {
    	if(filterName.length() == 1) {
    		String filter = Glossary.Filters.map.get(filterName);
        	if( filter == null || filter.length() == 0 ) {
        		throw new MappingError("No filter name found for abreviation " + filterName);
        	}
        	return filter;
		} else {
			return filterName;
		}
	}
    
    public static String getFPSResponse(String svoId) throws MalformedURLException, IOException, MappingError {
    	
        String fpsUrl = Glossary.Url.FPS + svoId;
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

        if (response.contains("<INFO name=\"QUERY_STATUS\" value=\"ERROR\">")) {
            String message = extractFPSErrorDescription(response);
            throw new MappingError("FPS service error: " + message);
        }
        return response;
    }
}
