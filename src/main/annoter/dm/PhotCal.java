package main.annoter.dm;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

import main.annoter.mivot.MivotAnnotations;
import main.annoter.utils.MivotUtils;
import main.annoter.utils.XmlUtils;

public class PhotCal {
    private static final Logger LOGGER = Logger.getLogger(MivotAnnotations.class.getName());

    private static String extractDescription(String xml) {
        // Simple regex extraction in Java, or XML parsing.
        int start = xml.indexOf("<DESCRIPTION>") + 13;
        int end = xml.indexOf("</DESCRIPTION>");
        return xml.substring(start, end).trim();
    }

    public static String addPhotCal(String filterSimbadName) throws Exception {
    	
    	String svoId = Glossary.Filters.map.get(filterSimbadName);
    	if( svoId == null || svoId.length() == 0 ) {
    		return "";
    	}
        String fpsUrl = Glossary.Url.FPS + svoId;
        HttpURLConnection connection = (HttpURLConnection) new URL(fpsUrl).openConnection();
        connection.setRequestMethod("GET");

        int httpCode = connection.getResponseCode();
        if (httpCode != 200) {
            LOGGER.severe("FPS service error: " + httpCode);
            return null;
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

        if (response.contains("<INFO name=\"QUERY_STATUS\" value=\"ERROR\">")) {
            String message = extractDescription(response);
            throw new Exception("FPS service error: " + message);
        }

        String calId = MivotUtils.formatDmid(filterSimbadName);
        String photcalId = "_photcal_" + calId;
        String filterId = "_photfilter_" + calId;

        // fix some FPS tweaks
        //response = XmlUtils.prettyString(response);
        response = response.replace(
            "<ATTRIBUTE dmrole=\"Phot:ZeroPoint.softeningParameter\" dmtype=\"ivoa:real\" value=\"\"/>",
            "<!-- ATTRIBUTE dmrole=\"Phot:ZeroPoint.softeningParameter\"" +
            " dmtype=\"ivoa:real\" value=\"\"/ -->");
        response = response.replace("Phot:photometryFilter", "Phot:PhotometryFilter");
        response = response.replace("Phot:PhotCal.photometryFilter.bandwidth",
                                                          "Phot:PhotometryFilter.bandwidth");
        
        response = response.replace(" dmrole=\"\"", "dmid=\"" + photcalId + "\"");
        return XmlUtils.prettyString(response);
    }

}
