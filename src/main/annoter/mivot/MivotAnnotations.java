package main.annoter.mivot;

import java.util.*;
import java.util.logging.Logger;

import main.annoter.dm.Glossary;
import main.annoter.utils.XmlUtils;

public class MivotAnnotations {
    private Map<String, String> models;
    private boolean reportStatus;
    private String reportMessage;
    private List<String> globals;
    private List<String> templates;
    private String templatesId;
    private List<String> dmids;
    public String mivotBlock;

    public MivotAnnotations() {
        this.models = new LinkedHashMap<>();
        this.reportStatus = true;
        this.reportMessage = "Generated by the VOLLT Mivot extension";
        this.globals = new ArrayList<>();
        this.templates = new ArrayList<>();
        this.templatesId = "";
        this.dmids = new ArrayList<>();
        this.mivotBlock = "";
    }

    public String getMivotBlock() {
        return this.mivotBlock;
    }

    public boolean containsDmid(String dmid) {
    	return this.dmids.contains(dmid);
    }
    
    public List<String> getDmids() {
    	return this.dmids;
    }
    
    public void addDmid(String dmid) {
    	if( !this.dmids.contains(dmid)) {
    		this.dmids.add(dmid);
    	}
    }
    
    public String addDefaultSpaceFrame() throws Exception {
    	return this.addSimpleSpaceFrame(null, null, null, null);
    }
    public String addSimpleSpaceFrame(String spaceRefFrame, String refPosition, String equinox, String epoch) throws Exception {
        spaceRefFrame = (spaceRefFrame == null)? "ICRS": spaceRefFrame;
        refPosition = (refPosition == null)? "BARYCENTER": refPosition;
 
        String dmid = "_spaceframe_" + spaceRefFrame + "_" + refPosition;
        if (equinox != null) dmid += "_" + equinox;

        if (this.containsDmid(dmid)) {
            System.out.println("Space frame already exists: " + dmid);
            return dmid;
        }
        this.addDmid(dmid);

        this.addModel(Glossary.ModelPrefix.IVOA, Glossary.VodmlUrl.IVOA);
        this.addModel(Glossary.ModelPrefix.COORDS, Glossary.VodmlUrl.COORDS);


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
        this.addGlobals(spaceSys);

        return dmid;
    }
    private String getReport() {
        String status = reportStatus ? "OK" : "FAILED";
        return "<REPORT status=\"" + status + "\">" + reportMessage + "</REPORT>";
    }

    private String getModels() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : models.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                sb.append("<MODEL name=\"").append(entry.getKey())
                  .append("\" url=\"").append(entry.getValue()).append("\" />\n");
            } else {
                sb.append("<MODEL name=\"").append(entry.getKey()).append("\" />\n");
            }
        }
        return sb.toString();
    }

    private String getGlobals() {
        StringBuilder sb = new StringBuilder("<GLOBALS>\n");
        for (String g : globals) {
            sb.append(g).append("\n");
        }
        sb.append("</GLOBALS>\n");
        return sb.toString();
    }

    private String getTemplates() {
        if (templates.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        if (templatesId != null && !templatesId.isEmpty()) {
            sb.append("<TEMPLATES tableref=\"").append(templatesId).append("\">\n");
        } else {
            sb.append("<TEMPLATES>\n");
        }

        for (String t : templates) {
            sb.append(t).append("\n");
        }
        sb.append("</TEMPLATES>\n");
        return sb.toString();
    }

    public void buildMivotBlock(String templatesId) throws Exception {
        if (templatesId != null) {
            this.templatesId = templatesId;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<VODML xmlns=\"http://www.ivoa.net/xml/mivot\">\n");
        sb.append(getReport()).append("\n");
        sb.append(getModels()).append("\n");
        sb.append(getGlobals()).append("\n");
        sb.append(getTemplates()).append("\n");
        sb.append("</VODML>");

        this.mivotBlock = XmlUtils.prettyString(sb.toString()).replaceAll("\n\\s*\n", "\n");
    }

    public void addTemplates(Object instance) throws Exception {
        if (instance instanceof MivotInstance) {
            MivotInstance mi = (MivotInstance) instance;
            templates.add(mi.xmlString());
            if (mi.getDmid() != null) dmids.add(mi.getDmid());
        } else if (instance instanceof String) {
            templates.add((String) instance);
        } else {
            throw new Exception("Invalid type for templates instance: " + instance.getClass());
        }
    }

    public void addGlobals(Object instance) throws Exception {
        if (instance instanceof MivotInstance) {
            MivotInstance mi = (MivotInstance) instance;
            globals.add(mi.xmlString());
            if (mi.getDmid() != null) dmids.add(mi.getDmid());
        } else if (instance instanceof String) {
            globals.add((String) instance);
        } else {
            throw new Exception("Invalid type for globals instance");
        }
    }

    public void addModel(String name, String url) {
        models.put(name, url);
    }

    public void setReport(boolean status, String message) {
        this.reportStatus = status;
        this.reportMessage = message;
        if (!status) {
            globals.clear();
            templates.clear();
        }
    }
}
