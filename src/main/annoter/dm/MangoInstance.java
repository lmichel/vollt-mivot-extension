package main.annoter.dm;

import java.util.ArrayList;
import java.util.List;

import main.annoter.meta.Glossary;
import main.annoter.mivot.MappingError;
import main.annoter.mivot.MivotInstance;
import main.annoter.utils.MivotUtils;

/**
 * Represents a high-level MANGO object instance that aggregates mapped
 * properties for a single data row.
 *
 * Responsibilities:
 * - Hold a list of mapped {@link Property} instances for a Mango object.
 * - Provide a convenient {@link #xmlString()} serializer that produces the
 *   final MIVOT fragment for the MangoObject (identifier, optional origin,
 *   and the propertyDock collection).
 *
 * Notes:
 * - This is a lightweight wrapper around {@link MivotInstance} that keeps the
 *   Mango-specific assembly logic in one place. No network I/O is performed
 *   here.
 */
public class MangoInstance extends MivotInstance{

    /** Collected property objects to include in the MangoObject.propertyDock. */
    private final List<Property> properties = new ArrayList<>();

    /** Optional identifier (dmid or literal) for this MangoObject. */
    private final String dmid;

    /** When true the instance will include a queryOrigin reference to _origin. */
    private boolean withOrigin=false;

    /**
     * Create a new MangoInstance with the given identifier.
     *
     * @param dmid the identifier to use for the MangoObject (may be null)
     * @throws MappingError if underlying MivotInstance initialization fails
     */
    public MangoInstance(String dmid) throws MappingError {
        super("mango:MangoObject");
        this.dmid = dmid;
    }

    /**
     * Return the list of properties that will be serialized into the propertyDock.
     * The returned list is the live internal list; callers should not modify it
     * unless they intentionally want to mutate the MangoInstance state.
     *
     * @return mutable list of Property objects
     */
    public List<Property>getMangoProperties(){
        return this.properties;
    }
    
    /**
     * Add a mapped Property to this MangoInstance.
     *
     * @param property property instance to append to the MangoObject.propertyDock
     */
    public void addMangoProperties(Property property){
        this.properties.add(property);
    }
    
    /**
     * Mark the MangoInstance as having an origin reference. When set, the
     * generated XML will include a reference to the special _origin instance.
     */
    public void addOrigin() {
        this.withOrigin = true;
    }
    
    /**
     * Produce the final MIVOT XML fragment for this MangoObject, including:
     * - mango:MangoObject.identifier (if dmid provided)
     * - mango:MangoObject.queryOrigin reference when withOrigin is true
     * - mango:MangoObject.propertyDock collection containing all properties
     *
     * The method converts any dmid that represents a reference or literal
     * using {@link MivotUtils#getRefOrLiteral(String)} so callers can pass
     * either forms.
     *
     * @return pretty-serialized XML string representing the MangoObject
     * @throws MappingError when serializing nested instances fails
     */
    public String xmlString() throws MappingError {
        // Build a fresh MivotInstance to hold the MangoObject content. We do
        // not reuse `this` because MivotInstance may carry transient state and
        // the serialization must be independent of the wrapper object.
        MivotInstance mangoObject = new MivotInstance("mango:MangoObject", null, null);

        // Write identifier: support both reference or literal semantics
        if (this.dmid != null) {
            String[] refOrVal = MivotUtils.getRefOrLiteral(dmid);
            String value = refOrVal[0] != null ? refOrVal[0] : refOrVal[1];
            mangoObject.addAttribute(Glossary.IvoaType.STRING, "mango:MangoObject.identifier", value, null);
        }

        // Optionally include a queryOrigin reference to the special _origin instance
        if (this.withOrigin) {
            mangoObject.addReference("mango:MangoObject.queryOrigin", "_origin");
        }

        // Collect property instances: Property extends MivotInstance so we can
        // safely add them to the collection for serialization.
        List<MivotInstance> serialized = new ArrayList<>();
        for (Property prop : properties) {
            serialized.add(prop);
        }

        // Add the property collection and serialize the MangoObject to XML
        mangoObject.addCollection("mango:MangoObject.propertyDock", serialized);
        return mangoObject.xmlString();
    }
}