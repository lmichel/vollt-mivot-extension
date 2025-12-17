package main.annoter.dm;

import java.util.ArrayList;
import java.util.List;

import main.annoter.meta.Glossary;
import main.annoter.mivot.MappingError;
import main.annoter.mivot.MivotInstance;
import main.annoter.utils.MivotUtils;

public class MangoInstance extends MivotInstance{

    private final List<Property> properties = new ArrayList<>();
    private final String dmid;
    private boolean withOrigin=false;

    public MangoInstance(String dmid) throws MappingError {
    	super("mango:MangoObject");
        this.dmid = dmid;
    }

    public List<Property>getMangoProperties(){
    	return this.properties;
    }
    
    public void addMangoProperties(Property property){
    	this.properties.add(property);
    }
    
    public void addOrigin() {
    	this.withOrigin = true;
    }
    
    public String xmlString() throws MappingError {
        MivotInstance mangoObject = new MivotInstance("mango:MangoObject", null, null);

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
