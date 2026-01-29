package main.annoter.dm;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

import main.annoter.cache.Cache;
import main.annoter.meta.UtypeDecoder;
import main.annoter.mivot.FrameHolder;
import main.annoter.mivot.MappingError;
import main.annoter.mivot.MivotInstance;

/**
 * Base helper class used for all mapped property builders.
 *
 * Responsibilities:
 * - Provide convenience constructors that match the reflection-based factory
 *   used by {@link #getInstance(String, List, String, List, List)}.
 * - Expose common semantics wiring used by concrete Property subclasses via
 *   {@link #setSemantics(Map)}.
 *
 * Implementation notes:
 * - The {@code paramTypes} array defines the constructor signature expected
 *   by the factory. Subclasses used by the mapping engine must provide a
 *   constructor matching these parameter types: (List<UtypeDecoder>, String,
 *   List<FrameHolder>, List<String>).
 * - This class intentionally contains only light helper logic; concrete
 *   properties (Brightness, Color, EpochPosition...) implement mapping
 *   behaviour and call super constructors as needed.
 */
public class Property extends MivotInstance {

	/**
	 * Reflection parameter types expected by property concrete classes.
	 * Order is important and must match constructor signatures used by
	 * property implementations:
	 *  - List<UtypeDecoder>
	 *  - String (table name)
	 *  - List<FrameHolder>
	 *  - List<String> (constants)
	 */
	private static Class<?>[] paramTypes = new Class<?>[] {
        List.class,     // List<UtypeDecoder>
        String.class,   // tableName
        List.class ,     // List<FrameHolder>
        List.class // List<Constant>
	};

    /**
     * Minimal constructor allowing subclasses to provide only a dmtype.
     * Used by some tests and convenience code paths.
     *
     * @param dmtype data model type (e.g. "mango:Brightness")
     * @throws MappingError when underlying MivotInstance construction fails
     */
    public Property(String dmtype) throws MappingError {
        super(dmtype, null, null);
    }
    
	/**
	 * Reflection-compatible constructor used by the mapping factory.
	 *
	 * Concrete property implementations must accept exactly the parameter
	 * types defined in {@link #paramTypes}. This constructor simply delegates
	 * to the empty MivotInstance constructor so subclasses can set their own
	 * dmtype/dmrole/dmid afterwards.
	 *
	 * @param utypeDecoders decoded utype metadata for the table (may be inspected by subclasses)
	 * @param tableName ADQL table name (supplied for context)
	 * @param frameHolders pre-built frames that may be referenced by the property
	 * @param constants list of CT/... qualifiers attached to utypes
	 * @throws MappingError on failure to initialize the base MivotInstance
	 */
	public Property(List<UtypeDecoder> utypeDecoders,
				String tableName,
				List<FrameHolder> frameHolders,
				List<String> constants) throws MappingError {
        super(null, null, null);
	}


    /**
     * Convenience constructor allowing callers to provide dmtype/dmrole/dmid
     * directly. Typical property implementations call this to initialise the
     * underlying MivotInstance state.
     *
     * @param dmtype data model type
     * @param dmrole role within the parent instance
     * @param dmid unique id (may be null)
     * @throws MappingError when MivotInstance creation fails
     */
    public Property(String dmtype, String dmrole, String dmid) throws MappingError {
        super(dmtype, dmrole, dmid);
    }
    
    /**
     * Constructor that also accepts a semantics map which is immediately
     * applied by {@link #setSemantics(Map)}. This helper avoids repeating
     * semantics wiring in many subclasses.
     *
     * @param dmtype data model type
     * @param dmrole role within the parent instance
     * @param dmid unique id (may be null)
     * @param semantics map of semantic keys (description, uri, label)
     * @throws MappingError when MivotInstance creation or semantics wiring fails
     */
    public Property(String dmtype, String dmrole, String dmid, Map<String, String> semantics) throws MappingError {
        super(dmtype, dmrole, dmid);
        this.setSemantics(semantics);
    }
        
    /**
     * Helper that converts a small semantics map into MIVOT vocabulary instances.
     *
     * Supported keys:
     * - description: added as a mango:Property.description attribute (literal)
     * - uri: added to a mango:VocabularyTerm.uri attribute on a nested instance
     * - label: added to a mango:VocabularyTerm.label attribute on the same instance
     *
     * The method is idempotent and safe to call with a null or empty map.
     *
     * @param semantics map containing optional description, uri and label entries
     * @throws MappingError when adding attributes/instances fails
     */
    public void setSemantics(Map<String, String> semantics) throws MappingError {
        if (semantics.containsKey("description")) {
            // description is encoded as a literal attribute (prefixed with '*')
            this.addAttribute("ivoa:string", "mango:Property.description", "*" + semantics.get("description"), null);
        }

        if (semantics.containsKey("uri") || semantics.containsKey("label")) {
            // Create a nested Mango VocabularyTerm instance to hold uri/label semantics
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

    /**
     * Factory that creates a concrete Property subclass using reflection.
     *
     * The method looks up the concrete class using the {@link Cache#getPropertyClass}
     * helper and invokes its constructor with (List<UtypeDecoder>, String,
     * List<FrameHolder>, List<String>) as declared in {@link #paramTypes}.
     *
     * @param className symbolic name of the property class to create
     * @param utds decoded utype descriptors for the table
     * @param table ADQL table name
     * @param frameHolders potential FrameHolder instances to pass to the constructor
     * @param constants list of CT qualifiers associated with the mapping
     * @return constructed Property instance (concrete subclass)
     * @throws Exception propagation of reflection-based constructor errors
     */
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