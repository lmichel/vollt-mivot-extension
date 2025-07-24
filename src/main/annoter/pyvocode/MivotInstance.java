package main.annoter.pyvocode;

import java.util.ArrayList;
import java.util.List;

import main.annoter.pyvocode.MappingError;
import main.annoter.pyvocode.MivotUtils;

/**
 * Java equivalent of the Python class MivotInstance.
 * Represents an <INSTANCE> element in a MIVOT annotation.
 */
public class MivotInstance {
    private String dmtype;
    private String dmrole;
    private String dmid;
    private final List<String> content;

    public MivotInstance(String dmtype) throws MappingError {
        this(dmtype, null, null);
    }
    
    public MivotInstance(String dmtype, String dmrole, String dmid) throws MappingError {
        if (dmtype == null || dmtype.isEmpty()) {
            throw new MappingError("Cannot build an instance without dmtype");
        }
        this.dmtype = dmtype;
        this.dmrole = dmrole;
        this.dmid = MivotUtils.formatDmid(dmid);
        this.content = new ArrayList<>();
    }

    public String getDmid() {
        return dmid;
    }

    public void addAttribute(String dmtype, String dmrole, String value, String unit) throws MappingError {
        if (dmtype == null || dmtype.isEmpty()) {
            throw new MappingError("Cannot add an attribute without dmtype");
        }
        if (dmrole == null || dmrole.isEmpty()) {
            throw new MappingError("Cannot add an attribute without dmrole");
        }

        String[] refOrLiteral = MivotUtils.getRefOrLiteral(value);
        String ref = refOrLiteral[0];
        String literal = refOrLiteral[1];

        if ((ref == null || ref.isEmpty()) && (value == null || value.isEmpty())) {
            throw new MappingError("Cannot add an attribute without ref or value");
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<ATTRIBUTE dmtype=\"").append(dmtype)
           .append("\" dmrole=\"").append(dmrole).append("\" ");
        if (unit != null && !"None".equals(unit)) {
            xml.append("unit=\"").append(unit).append("\" ");
        }

        if (literal != null) {
            xml.append("value=\"").append(literal).append("\" ");
        } else {
            xml.append("ref=\"").append(ref).append("\" ");
        }
        xml.append("/>");
        content.add(xml.toString());
    }

    public void addReference(String dmrole, String dmref) throws MappingError {
        if (dmref == null || dmref.isEmpty()) {
            throw new MappingError("Cannot add a reference without dmref");
        }
        if (dmrole == null || dmrole.isEmpty()) {
            throw new MappingError("Cannot add a reference without dmrole");
        }
        content.add("<REFERENCE dmrole=\"" + dmrole + "\" dmref=\"" + dmref + "\" />");
    }

    public void addInstance(MivotInstance instance) throws MappingError {
        if (instance == null) {
            throw new MappingError("Instance added must be of type MivotInstance");
        }
        content.add(instance.xmlString());
    }

    public void addCollection(String dmrole, List<MivotInstance> instances) throws MappingError {
        StringBuilder collection = new StringBuilder();
        collection.append("<COLLECTION");
        if (dmrole != null && !dmrole.isEmpty()) {
            collection.append(" dmrole=\"").append(dmrole).append("\"");
        }
        collection.append(">\n");

        for (MivotInstance instance : instances) {
            collection.append(instance.xmlString()).append("\n");
        }

        collection.append("</COLLECTION>");
        content.add(collection.toString());
    }

    public String xmlString() throws MappingError {
        StringBuilder xml = new StringBuilder();
        xml.append("<INSTANCE dmtype=\"").append(dmtype).append("\" ");
        if (dmrole != null) {
            xml.append("dmrole=\"").append(dmrole).append("\" ");
        }
        if (dmid != null && dmid.length() > 0 ) {
            xml.append("dmid=\"").append(dmid).append("\" ");
        }
        xml.append(">\n");

        for (String element : content) {
            xml.append(element).append("\n");
        }

        xml.append("</INSTANCE>\n");
        return XmlUtils.prettyString(xml.toString());
    }
}
