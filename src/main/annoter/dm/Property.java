package main.annoter.dm;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import main.annoter.cache.Cache;
import main.annoter.meta.UtypeDecoder;
import main.annoter.mivot.FrameHolder;
import main.annoter.mivot.MappingError;
import main.annoter.mivot.MivotInstance;

public class Property extends MivotInstance {

	private static Class<?>[] paramTypes = new Class<?>[] {
        List.class,     // List<UtypeDecoder>
        String.class,   // tableName
        List.class ,     // List<FrameHolder>
        List.class // List<Constant>
	};

    public Property(String dmtype) throws MappingError {
        super(dmtype, null, null);
    }
    
	public Property(List<UtypeDecoder> utypeDecoders,
			String tableName,
			List<FrameHolder> frameHolders,
			List<String> constants) throws MappingError {
        super(null, null, null);
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
    
	    public static Property getInstance(String className,
	    		List<UtypeDecoder> utds,
	    		String table,
	    		List<FrameHolder> frameHolders,
	    		List<String> constants) throws Exception {
		Class<?> cls = Cache.getPropertyClass(className);
		Constructor<?> constructor = cls.getConstructor(paramTypes);
		return (Property) constructor.newInstance(utds, table, frameHolders, constants);
	}
}
