package main.annoter.pyvocode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MangoInstance extends MivotInstance{

    private final List<Property> properties = new ArrayList<>();
    private final String dmid;
    private boolean withOrigin=false;

    public MangoInstance(String dmid) throws MappingError {
    	super("mango:MangoObject");
        this.dmid = dmid;
    }

    private MivotInstance getErrorInstance(String className, String dmrole, Map<String, Object> mapping) throws MappingError {
        MivotInstance errorInstance = new MivotInstance(Glossary.ModelPrefix.MANGO + ":error." + className, dmrole, null);
        //MivotUtils.populateInstance(errorInstance, className, mapping, table, IvoaType.RealQuantity, "error");
        return errorInstance;
    }

    private MivotInstance buildEpochCorrelations(Map<String, Object> correlations) throws MappingError {
        MivotInstance epcInstance = new MivotInstance(Glossary.ModelPrefix.MANGO + ":EpochPositionCorrelations",
        		Glossary.ModelPrefix.MANGO + ":EpochPosition.correlations", null);

        //MivotUtils.populateInstance(epcInstance, "EpochPositionCorrelations", correlations, table, IvoaType.real);
        return epcInstance;
    }

    public List<Property>getMangoProperties(){
    	return this.properties;
    }
    
    public void addMangoProperties(Property property){
    	this.properties.add(property);
    }
    private MivotInstance buildEpochErrors(Map<String, Map<String, Object>> errors) throws MappingError {
        MivotInstance errInstance = new MivotInstance(Glossary.ModelPrefix.MANGO + ":EpochPositionErrors",
        		Glossary.ModelPrefix.MANGO + ":EpochPosition.errors", null);

        for (Map.Entry<String, Map<String, Object>> entry : errors.entrySet()) {
            String role = entry.getKey();
            Map<String, Object> mapping = entry.getValue();
            String errorClass = (String) mapping.get("class");

            if (Glossary.Roles.EPOCH_POSITION_ERRORS.contains(role) &&
                    Arrays.asList("PErrorSym2D", "PErrorSym1D", "PErrorAsym1D").contains(errorClass)) {
                errInstance.addInstance(getErrorInstance(errorClass,
                		Glossary.ModelPrefix.MANGO + ":EpochPositionErrors." + role, mapping));
            }
        }
        return errInstance;
    }

    private MivotInstance buildEpochDate(Map<String, Object> mapping) throws MappingError {
        String representation = (String) mapping.get("representation");
        String value = (String) mapping.get("dateTime");

        if (!Glossary.CoordSystems.TIME_FORMATS.contains(representation)) {
            throw new MappingError("epoch representation " + representation + " not supported. Must be one of " + Glossary.CoordSystems.TIME_FORMATS);
        }

        MivotInstance datetimeInstance = new MivotInstance(Glossary.ModelPrefix.MANGO + ":DateTime",
                                                           Glossary.ModelPrefix.MANGO + ":EpochPosition.obsDate",
                                                           null);
        datetimeInstance.addAttribute(Glossary.IvoaType.STRING, Glossary.ModelPrefix.MANGO + ":DateTime.representation",
        		MivotUtils.asLiteral(representation), null);
        datetimeInstance.addAttribute(Glossary.IvoaType.DATETIME, Glossary.ModelPrefix.MANGO + ":DateTime.dateTime", value, null);
        return datetimeInstance;
    }

    public Property addEpochPosition(String spaceFrameId, String timeFrameId,
                                     Map<String, Object> mapping, Map<String, String> semantics) throws MappingError {

        Property epInstance = new Property(Glossary.ModelPrefix.MANGO + ":EpochPosition", null, null, semantics);

        //MivotUtils.populateInstance(epInstance, "EpochPosition", mapping, table, IvoaType.RealQuantity);

        if (mapping.containsKey("obsDate")) {
            epInstance.addInstance(buildEpochDate((Map<String, Object>) mapping.get("obsDate")));
        }

        if (mapping.containsKey("correlations")) {
            epInstance.addInstance(buildEpochCorrelations((Map<String, Object>) mapping.get("correlations")));
        }

        if (mapping.containsKey("errors")) {
            epInstance.addInstance(buildEpochErrors((Map<String, Map<String, Object>>) mapping.get("errors")));
        }

        if (spaceFrameId != null) {
            epInstance.addReference(Glossary.ModelPrefix.MANGO + ":EpochPosition.spaceSys", spaceFrameId);
        }

        if (timeFrameId != null) {
            epInstance.addReference(Glossary.ModelPrefix.MANGO + ":EpochPosition.timeSys", timeFrameId);
        }

        properties.add(epInstance);
        return epInstance;
    }

    public Property addBrightnessProperty(String filterId, Map<String, Object> mapping, Map<String, String> semantics) throws MappingError {
        Property magInstance = new Property(Glossary.ModelPrefix.MANGO + ":Brightness", null, null, semantics);

        //MivotUtils.populateInstance(magInstance, "PhotometricProperty", mapping, table, IvoaType.RealQuantity);

        if (mapping.containsKey("error")) {
            Map<String, Object> errorMapping = (Map<String, Object>) mapping.get("error");
            String errorClass = (String) errorMapping.get("class");
            magInstance.addInstance(getErrorInstance(errorClass,
                    Glossary.ModelPrefix.MANGO + ":PhotometricProperty.error", errorMapping));
        }

        magInstance.addReference(Glossary.ModelPrefix.MANGO + ":Brightness.photCal", filterId);
        properties.add(magInstance);
        return magInstance;
    }

    public Property addColorInstance(String filterLowId, String filterHighId, Map<String, Object> mapping, Map<String, String> semantics) throws MappingError {
        Property magInstance = new Property(Glossary.ModelPrefix.MANGO + ":Color", null, null, semantics);

        MivotInstance coldefInstance = new MivotInstance(Glossary.ModelPrefix.MANGO + ":ColorDef", Glossary.ModelPrefix.MANGO + ":Color.colorDef", null);

        boolean defFound = false;
        for (Map.Entry<String, Object> entry : mapping.entrySet()) {
            String dmrole = entry.getKey();
            if (dmrole.endsWith("definition")) {
                coldefInstance.addAttribute("mango:ColorDefinition", "mango:ColorDef.definition", "*" + entry.getValue(), null);
                defFound = true;
            }
        }

        if (!defFound) {
            throw new MappingError("Missing color definition");
        }

        mapping.remove("definition");

        //MivotUtils.populateInstance(magInstance, "PhotometricProperty", mapping, table, IvoaType.RealQuantity);

        coldefInstance.addReference(Glossary.ModelPrefix.MANGO + ":ColorDef.low", filterLowId);
        coldefInstance.addReference(Glossary.ModelPrefix.MANGO + ":ColorDef.high", filterHighId);

        Map<String, Object> errorMapping = (Map<String, Object>) mapping.get("error");
        String errorClass = (String) errorMapping.get("class");

        magInstance.addInstance(getErrorInstance(errorClass,
                Glossary.ModelPrefix.MANGO + ":PhotometricProperty.error", errorMapping));

        magInstance.addInstance(coldefInstance);
        properties.add(magInstance);
        return magInstance;
    }
    
    public MivotInstance getMangoObject(boolean withOrigin) throws MappingError {
        MivotInstance mangoObject = new MivotInstance("mango:MangoObject", dmid, null);

        if (dmid != null && !dmid.isEmpty()) {
        	String[]  refAndValue = MivotUtils.getRefOrLiteral(dmid);
            String attValue = (refAndValue[0] != null) ? refAndValue[0]  : refAndValue[1] ;

            mangoObject.addAttribute("mango:MangoObject.identifier", Glossary.IvoaType.STRING, attValue, null);
        }

        if (withOrigin) {
            mangoObject.addReference("mango:MangoObject.queryOrigin", "_origin");
        }

        List<MivotInstance> xmlProperties = new ArrayList<>();
        for (Property prop : properties) {
            xmlProperties.add(prop);
        }

        mangoObject.addCollection("mango:MangoObject.propertyDock", xmlProperties);

        return mangoObject;
    }

    public String xmlString() throws MappingError {
        MivotInstance mangoObject = new MivotInstance("mango:MangoObject", null, dmid);

        if (this.dmid != null) {
            String[] refOrVal = MivotUtils.getRefOrLiteral(dmid);
            String value = refOrVal[0] != null ? refOrVal[0] : refOrVal[1];
            mangoObject.addAttribute("mango:MangoObject.identifier", Glossary.IvoaType.STRING, value, null);
        }

        if (this.withOrigin) {
            mangoObject.addReference("mango:MangoObject.queryOrigin", "_origin");
        }

        List<MivotInstance> serialized = new ArrayList<>();
        for (Property prop : properties) {
            serialized.add(prop);
        }

        mangoObject.addCollection("mango:MangoObject.propertyDock", serialized);
        return mangoObject.xmlString();
    }
}
