package main.annoter.mivot;

import java.util.ArrayList;
import java.util.List;

import main.annoter.utils.MivotUtils;
import main.annoter.utils.XmlUtils;

/**
 * Java equivalent of the Python class MivotInstance.
 * Represents an <INSTANCE> element in a MIVOT annotation.
 *
 * An instance holds a DM type (dmtype), an optional role (dmrole), an optional
 * identifier (dmid) and a list of inner elements (attributes, references,
 * nested instances and collections) represented as XML fragments.
 */
public class MivotInstance {
    /** The DM type of this instance (required). */
    private String dmtype;

    /** Optional role for the instance within its parent. */
    private String dmrole;

    /** Normalized identifier for this instance (may be empty string). */
    private String dmid;

    /** Ordered list of XML fragment strings making up the INSTANCE body. */
    private final List<String> content;

    /**
     * Create a new instance with only a DM type. dmrole and dmid will be null.
     *
     * @param dmtype the DM type for this instance (must not be null/empty)
     * @throws MappingError when dmtype is null or empty
     */
    public MivotInstance(String dmtype) throws MappingError {
        this(dmtype, null, null);
    }
    
    /**
     * Create a new instance with the given type, role and identifier.
     * The provided dmid will be sanitized using {@link MivotUtils#formatDmid(String)}.
     *
     * @param dmtype the DM type for this instance (must not be null/empty)
     * @param dmrole optional role string (may be null)
     * @param dmid optional identifier (may be null)
     * @throws MappingError when dmtype is null or empty
     */
    public MivotInstance(String dmtype, String dmrole, String dmid) throws MappingError {
        if (dmtype == null || dmtype.isEmpty()) {
            throw new MappingError("Cannot build an instance without dmtype");
        }
        this.dmtype = dmtype;
        this.dmrole = dmrole;
        this.dmid = MivotUtils.formatDmid(dmid);
        this.content = new ArrayList<>();
    }

    /**
     * Return the (sanitized) DM identifier for this instance.
     *
     * @return the dmid string (may be empty)
     */
    public String getDmid() {
        return dmid;
    }

    /**
     * Add an attribute to this instance using string input for the value.
     * The value may be a reference or a literal marker (see {@link MivotUtils#getRefOrLiteral}).
     *
     * @param dmtype attribute DM type (required)
     * @param dmrole attribute DM role (required)
     * @param value string value or reference marker (may be null only if ref is provided)
     * @param unit optional unit string (use "None" to indicate no unit)
     * @throws MappingError on invalid arguments (missing dmtype/dmrole or missing value/ref)
     */
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

    /**
     * Add an attribute with a numeric (Double) value.
     *
     * @param dmtype attribute DM type (required)
     * @param dmrole attribute DM role (required)
     * @param value numeric value to set (may be null)
     * @param unit optional unit string (use "None" to indicate no unit)
     * @throws MappingError on invalid arguments (missing dmtype or dmrole)
     */
    public void addAttribute(String dmtype, String dmrole, Double value, String unit) throws MappingError {
        if (dmtype == null || dmtype.isEmpty()) {
            throw new MappingError("Cannot add an attribute without dmtype");
        }
        if (dmrole == null || dmrole.isEmpty()) {
            throw new MappingError("Cannot add an attribute without dmrole");
        }


        StringBuilder xml = new StringBuilder();
        xml.append("<ATTRIBUTE dmtype=\"").append(dmtype)
           .append("\" dmrole=\"").append(dmrole).append("\" ");
        if (unit != null && !"None".equals(unit)) {
            xml.append("unit=\"").append(unit).append("\" ");
        }

        xml.append("value=\"").append(value).append("\" ");
         xml.append("/>");
        content.add(xml.toString());
    }

    /**
     * Add a reference element to this instance.
     *
     * @param dmrole the role for the reference (required)
     * @param dmref the target reference identifier (required)
     * @throws MappingError when dmrole or dmref are null/empty
     */
    public void addReference(String dmrole, String dmref) throws MappingError {
        if (dmref == null || dmref.isEmpty()) {
            throw new MappingError("Cannot add a reference without dmref");
        }
        if (dmrole == null || dmrole.isEmpty()) {
            throw new MappingError("Cannot add a reference without dmrole");
        }
        content.add("<REFERENCE dmrole=\"" + dmrole + "\" dmref=\"" + dmref + "\" />");
    }

    /**
     * Add a nested MivotInstance as a child of this instance.
     *
     * @param instance non-null MivotInstance to add
     * @throws MappingError when the provided instance is null
     */
    public void addInstance(MivotInstance instance) throws MappingError {
        if (instance == null) {
            throw new MappingError("Instance added must cannot be null");
        }
        content.add(instance.xmlString());
    }

    /**
     * Add a collection element containing the provided instances.
     *
     * @param dmrole optional role of the collection (may be null)
     * @param instances list of instances to include in the collection
     * @throws MappingError not thrown here but kept for API symmetry with other methods
     */
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

    /**
     * Serialize this instance and its collected content into a pretty-printed
     * XML string representing the <INSTANCE> element.
     *
     * @return pretty-printed XML string for this instance
     * @throws MappingError not thrown here but declared for API symmetry
     */
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