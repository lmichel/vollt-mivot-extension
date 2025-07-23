package main.annoter.pyvocode;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

public class InstancesFromModels {

    private static final Logger LOGGER = Logger.getLogger(InstancesFromModels.class.getName());

    private MangoInstance mangoInstance;
    private MivotAnnotations annotation;

    public InstancesFromModels(String dmid) {
        this.mangoInstance = new MangoInstance(dmid);
        this.annotation = new MivotAnnotations();

        this.annotation.addModel("ivoa", Glossary.VodmlUrl.IVOA);
    }

    public String getMivotBlock() {
        return annotation.getMivotBlock();
    }

    private void checkValueConsistency(String word, List<String> suggestedWords) {
        String cleanWord = word.replace("*", "");
        if (!suggestedWords.contains(cleanWord)) {
            LOGGER.warning(String.format("Ref frame %s is not in %s, check for typos", word, suggestedWords));
        }
    }

    public String addPhotCal(String filterName) throws Exception {
        String fpsUrl = Glossary.Url.FPS + filterName;
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

        String calId = MivotUtils.formatDmid(filterName);
        String photcalId = "_photcal_" + calId;
        String filterId = "_photfilter_" + calId;

        if (annotation.containsDmid(calId)) {
            LOGGER.warning("Dmid already in GLOBALS: " + calId);
            return photcalId;
        }
        annotation.addDmid(calId);

        
        // Find filter block, manipulate XML tree
        // ... similar XPath or DOM traversal needed here

        annotation.addModel("ivoa", Glossary.VodmlUrl.IVOA);
        annotation.addModel("Phot", Glossary.VodmlUrl.PHOT);

        // Add manipulated blocks to GLOBALS
        annotation.addGlobals(XmlUtils.prettyString(response));
        // filterBlock too...

        return photcalId;
    }

    private String extractDescription(String xml) {
        // Simple regex extraction in Java, or XML parsing.
        int start = xml.indexOf("<DESCRIPTION>") + 13;
        int end = xml.indexOf("</DESCRIPTION>");
        return xml.substring(start, end).trim();
    }

    public String addSimpleSpaceFrame(Map<String, String> mapping) throws Exception {
        String spaceRefFrame = mapping.getOrDefault("spaceRefFrame", "ICRS");
        String refPosition = mapping.getOrDefault("refPosition", "BARYCENTER");
        String equinox = mapping.get("equinox");
        String epoch = mapping.get("epoch");

        String dmid = "_spaceframe_" + spaceRefFrame + "_" + refPosition;
        if (equinox != null) dmid += "_" + equinox;

        if (annotation.containsDmid(dmid)) {
            LOGGER.warning("Space frame already exists: " + dmid);
            return dmid;
        }
        annotation.addDmid(dmid);

        annotation.addModel(Glossary.ModelPrefix.IVOA, Glossary.VodmlUrl.IVOA);
        annotation.addModel(Glossary.ModelPrefix.COORDS, Glossary.VodmlUrl.COORDS);


        MivotInstance spaceSys = new MivotInstance(Glossary.ModelPrefix.COORDS + ":SpaceSys", null, dmid);
        MivotInstance spaceFrame = new MivotInstance(Glossary.ModelPrefix.COORDS + ":SpaceFrame",
        		Glossary.ModelPrefix.COORDS + ":PhysicalCoordSys.frame", null);

        spaceFrame.addAttribute(Glossary.IvoaType.STRING, Glossary.ModelPrefix.COORDS + ":SpaceFrame.spaceRefFrame", spaceRefFrame, null);

        if (equinox != null) {
            spaceFrame.addAttribute(Glossary.ModelPrefix.COORDS + ":Epoch",
            		Glossary.ModelPrefix.COORDS + ":SpaceFrame.equinox", equinox, null);
        }

        MivotInstance refLoc = (epoch != null)
                ? new MivotInstance(Glossary.ModelPrefix.COORDS + ":CustomRefLocation",
                		Glossary.ModelPrefix.COORDS + ":SpaceFrame.refPosition", null)
                : new MivotInstance(Glossary.ModelPrefix.COORDS + ":StdRefLocation",
                		Glossary.ModelPrefix.COORDS + ":SpaceFrame.refPosition", null);

        refLoc.addAttribute(Glossary.IvoaType.STRING, Glossary.ModelPrefix.COORDS + ":StdRefLocation.position", refPosition, null);
        if (epoch != null) {
            refLoc.addAttribute("coords:Epoch", Glossary.ModelPrefix.COORDS + ":CustomRefLocation.epoch", epoch, null);
        }

        spaceFrame.addInstance(refLoc);
        spaceSys.addInstance(spaceFrame);
        annotation.addGlobals(spaceSys);

        return dmid;
    }

    public String addSimpleTimeFrame(Map<String, String> mapping) throws Exception {
        String timescale = mapping.getOrDefault("timescale", "TCB");
        String refPosition = mapping.getOrDefault("refPosition", "BARYCENTER");

        String dmid = "_timeframe_" + timescale + "_" + refPosition;
        dmid = MivotUtils.formatDmid(dmid);

        if (annotation.containsDmid(dmid)) {
            LOGGER.warning("Time frame already exists: " + dmid);
            return dmid;
        }

        annotation.addModel(Glossary.ModelPrefix.IVOA, Glossary.VodmlUrl.IVOA);
        annotation.addModel(Glossary.ModelPrefix.COORDS, Glossary.VodmlUrl.COORDS);

        checkValueConsistency(timescale, Glossary.CoordSystems.TIME_FRAMES);
        checkValueConsistency(refPosition, Glossary.CoordSystems.REF_POSITIONS);

        MivotInstance timeSys = new MivotInstance(Glossary.ModelPrefix.COORDS + ":TimeSys", dmid, null);
        MivotInstance timeFrame = new MivotInstance(Glossary.ModelPrefix.COORDS + ":TimeFrame",
        		Glossary.ModelPrefix.COORDS + ":PhysicalCoordSys.frame", null);

        timeFrame.addAttribute(Glossary.IvoaType.STRING, Glossary.ModelPrefix.COORDS + ":TimeFrame.timescale", timescale, null);

        MivotInstance refLoc = new MivotInstance(Glossary.ModelPrefix.COORDS + ":StdRefLocation",
        		Glossary.ModelPrefix.COORDS + ":TimeFrame.refPosition", null);
        refLoc.addAttribute(Glossary.IvoaType.STRING, Glossary.ModelPrefix.COORDS + ":StdRefLocation.position", refPosition, null);

        timeFrame.addInstance(refLoc);
        timeSys.addInstance(timeFrame);

        annotation.addGlobals(timeSys);

        return dmid;
    }
    
    public void packIntoVotable(String reportMsg, boolean sparse) throws Exception {
        // Default to empty string if reportMsg is null
        if (reportMsg == null) {
            reportMsg = "";
        }

        // Set the report tag
        annotation.setReport(true, reportMsg);

        if (sparse) {
            // Add each individual property to the TEMPLATES block
            for (MivotInstance prop : mangoInstance.getMangoProperties()) {
                annotation.addTemplates(prop.xmlString());
            }
        } else {
            // Add the packed MangoObject to the TEMPLATES block
            boolean withOrigin = annotation.getDmids().contains("_origin");
            annotation.addTemplates(mangoInstance.getMangoObject(withOrigin));
        }

        // Finalize the MIVOT block and insert into the VOTable
        annotation.buildMivotBlock("", false);
    }

}
