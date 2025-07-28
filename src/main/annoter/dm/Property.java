package main.annoter.dm;

import java.util.Map;

import main.annoter.mivot.MappingError;
import main.annoter.mivot.MivotInstance;

public class Property extends MivotInstance {

    public Property(String dmtype) throws MappingError {
        super(dmtype, null, null);
    }

    public Property(String dmtype, String dmrole, String dmid, Map<String, String> semantics) throws MappingError {
        super(dmtype, dmrole, dmid);
        if (semantics.containsKey("description")) {
            this.addAttribute("ivoa:string", "mango:Property.description", "*" + semantics.get("description"), null);
        }

        if (semantics.containsKey("uri") || semantics.containsKey("label")) {
            MivotInstance semanticsInstance = new MivotInstance("mango:VocabularyTerm", "mango:Property.semantics", null);

            if (semantics.containsKey("uri")) {
                semanticsInstance.addAttribute("ivoa:string", "mango:VocabularyTerm.uri", "*" + semantics.get("uri"), null);
            }
            if (semantics.containsKey("label")) {
                semanticsInstance.addAttribute("ivoa:string", "mango:VocabularyTerm.label", "*" + semantics.get("label"), null);
            }

            this.addInstance(semanticsInstance);
        }
    }
}
