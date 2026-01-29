package main.annoter.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import main.annoter.mivot.MappingError;
import tap.metadata.TAPColumn;

/**
 * Parser/decoder for UType strings carried on TAP columns.
 *
 * This class extracts the meaningful parts of a UType like:
 *  - hostClass.hostAttribute
 *    e.g. mango:EpochPosition.parallax
 *  - hostClass.hostAttribute.innerRole/innerClass.innerAttribute
 *    e.g. mango:EpochPosition.errors.parallax/mango:error.PErrorSym1D.sigma1
 *
 * Additionally UTypes may include bracketed qualifiers of the form
 *  [CS=name CT=value ...]
 * where CS indicates a coordinate system frame (CS.<name>=<value>) and
 * CT indicates a constant (CT.<name>=<value>). These are parsed into the
 * `frames` and `constants` lists respectively.
 *
 * Notes:
 * - The decoder keeps the original TAPColumn reference for context
 *   and exposes getters for all extracted components.
 * - The implementation uses a mix of regex-based parsing and simple
 *   string operations. It intentionally prints and exits on fatal format
 *   problems (preserving original behaviour), but throws MappingError where
 *   appropriate for mapping-related consistency checks.
 */
public class UtypeDecoder {
	private TAPColumn tapColumn;
	private String utype;
	private String hostClass;
	private int instanceNumber = 0;
	private String hostAttribute;
	private String innerRole;
	private String innerClass;
	private String innerAttribute;
	private String constantAndFrames;
	private List<String> frames = new ArrayList<String>();
	private List<String> constants = new ArrayList<String>();
	
	// Regex fragments used to recognise class/package/field names in UTypes.
	String CLASS_NAME = "[A-Z][\\w]+"; // capitalised class name, e.g. EpochPosition
	String PACKAGE= "[a-z][a-z0-9_]+";   // package prefix, e.g. mango
	String FIELD = "[a-z][a-zA-Z0-9_]+"; // field name starting with lower-case
	
	// SIMPLE_ROLE matches patterns like: mango:EpochPosition.parallax
	String SIMPLE_ROLE = "(" + PACKAGE + ":" + "(?:" + PACKAGE + "\\.)?" + CLASS_NAME + ")\\.(" + FIELD + ")";
	// COMPOUND matches full form: host.attr.innerRole/innerClass.innerAttr
	String COMPOUND =  "^" + SIMPLE_ROLE + "\\.(" + FIELD + ")\\/" + SIMPLE_ROLE + "$";
	// SHORT_COMPOUND matches shortened compound where innerRole repeats hostAttribute
	String SHORT_COMPOUND =  "^" + SIMPLE_ROLE + "\\/" + SIMPLE_ROLE + "$";

	/**
	 * Construct a decoder for the given TAP column.
	 *
	 * The constructor initializes parsing state and attempts to decode the
	 * UType into its components (hostClass, hostAttribute, innerRole, etc.).
	 * It also extracts any bracketed CS/CT qualifiers into `frames` and
	 * `constants`.
	 *
	 * @param tapColumn TAPColumn that carries the utype to decode
	 */
	public UtypeDecoder(TAPColumn tapColumn ){
		this.tapColumn = tapColumn;
		this.utype = this.tapColumn.getUtype() ;
		String[] eles = utype.split(":" );

		// Extract bracketed CS/CT qualifiers (if any) first so the base utype
		// used by subsequent regexes does not contain them.
		this.extractConstantsAndFrames();
		// Try the simpler (single-role) form first, then fall back to compound.
		if( this.processSimpleRole() == false && this.processCompound() == false ) {
			System.out.println("UTYPE not valid: " + this.utype);
			System.exit(1);
		}  else {
			return;
		}
		
		// Legacy handling for prefixed instance numbers (format: #n-...)
		if( utype.matches("^#\\d\\-.*")) {
			this.instanceNumber = Integer.parseInt(utype.substring(1,2));
			this.utype = this.utype.substring(3);
		}
	
		// Fallback parsing when colon-separated elements exist in the utype.
		if( eles.length == 1) {
			System.out.println("wrong format 1");
		} else if( eles.length == 2) {
			int idx = this.utype.lastIndexOf(".");
			this.hostClass = this.utype.substring(0, idx);
		    this.hostAttribute = this.utype.substring(0, idx) + "." +  this.utype.substring(idx + 1);

		} else if( eles.length > 2) {
			int idx = this.utype.indexOf("/");
			this.hostClass = this.utype.substring(0, idx-1);
		    this.hostAttribute =  this.utype.substring(idx + 1);
		}
		// Re-extract bracketed qualifiers in case the utype was modified above.
		this.extractConstantsAndFrames();

	}
	
	/**
	 * Try to parse a simple role form like "mango:Class.field".
	 *
	 * @return true when parsing succeeded and hostClass/hostAttribute were set
	 */
	private boolean processSimpleRole() {
		Pattern pattern = Pattern.compile("^" + SIMPLE_ROLE + "$" );
		Matcher matcher = pattern.matcher(this.utype);

		if (matcher.find()) {
			this.hostClass = matcher.group(1);
			this.hostAttribute = matcher.group(2);
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Try to parse a compound role of the form
	 * host.attr.innerRole/innerClass.innerAttr or the short form
	 * host.attr/innerClass.innerAttr.
	 *
	 * On success the object fields hostClass, hostAttribute, innerRole,
	 * innerClass and innerAttribute will be populated.
	 *
	 * @return true when a compound form was matched
	 */
	public boolean processCompound() {
		Pattern pattern = Pattern.compile(this.COMPOUND);
		Matcher matcher = pattern.matcher(this.utype);
		if (matcher.find()) {
			this.hostClass =  matcher.group(1);
			this.hostAttribute = matcher.group(2);
			this.innerRole = matcher.group(3);
			this.innerClass = matcher.group(4);
			this.innerAttribute =  matcher.group(5);
			return true;
		} else {
			// Try the shortened compound form where innerRole is identical to hostAttribute
			pattern = Pattern.compile(this.SHORT_COMPOUND);
			matcher = pattern.matcher(this.utype);
			if (matcher.find()) {
				this.hostClass =  matcher.group(1);
				this.hostAttribute = matcher.group(2);
				this.innerRole = matcher.group(2);
				this.innerClass = matcher.group(3);
				this.innerAttribute =  matcher.group(4);
				return true;
			}

		}
		return false;
	}

	/**
	 * Extract bracketed CS/CT qualifiers of the form:
	 *   [CS.name=Value CT.foo=Bar ...]
	 *
	 * Found frames are added to `frames` and constants to `constants`.
	 * The main `utype` field is trimmed to remove the bracketed suffix.
	 */
	private void extractConstantsAndFrames() {
		String input = "prefix text [CS.name=John CT.age=30]"; // example used while developing regex
		input = this.utype;
		String cs_regexp =  "[\\:#a-zA-Z0-9\\./]";
		String regex = "([^\\[]*)(\\[(C(?:S|T)\\.\\w+=" + cs_regexp + "+\\s?)+\\])";
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(input);

		if (matcher.matches()) {
			// group(1) is the base utype (without the bracketed qualifiers)
			this.utype = matcher.group(1).trim();

			this.constantAndFrames = matcher.group(2);
			if (this.constantAndFrames != null) {
				// innerMatcher finds individual CS.*=... or CT.*=... entries
				Matcher innerMatcher = Pattern.compile("C(?:S|T)\\.\\w+=[\\:#a-zA-Z0-9\\./]+").matcher(this.constantAndFrames);
				while (innerMatcher.find()) {
					String match = innerMatcher.group();
					if( match.startsWith("CS")) {
						this.frames.add(match.replace("CS.", ""));
					} else if( match.startsWith("CT")) {
						this.constants.add(match.replace("CT.", ""));
					} else {
						System.out.println("Unsupported bracketed element: " + matcher);
					}
				}
			} 
		} else {
			// No bracketed qualifiers present; keep original utype as-is.
			// (Original implementation printed a message; we keep that behaviour.)
			System.out.println("No constant or frame for input: " + input);
		}
	}
	public String getHostClass() {
		return this.hostClass;
	}
	public String getHostAttribute() {
		return this.hostAttribute;
	}
	public String getInnerRole() {
		return this.innerRole;
	}
	public String getInnerClass() {
		return this.innerClass;
	}
	public String getInnerAttribute() {
		return this.innerAttribute;
	}
	public List<String> getFrames() {
		return this.frames;
	}
	public String getFrame(String csClass) {
		for( String frame: this.frames) {
			if( frame.startsWith(csClass + "=") ){
				return(frame);
			}
		}
		return null;
	}
	public List<String> getConstants() {
		return this.constants;
	}
	public String getConstant(String ctClass) {
		for(String constant: this.constants) {
			if( constant.startsWith(ctClass + "=") ){
				return(constant.replace(ctClass + "=", ""));
			}
		}
		return null;
	}
	public TAPColumn getTapColumn() {
		return this.tapColumn;
	}
	public String getUtype() {
		return this.utype;
	}
	public String getConstantAndFrames() {
		return this.constantAndFrames;
	}
	public String toString() {
		return "utype=" + this.utype + "\n instanceNumber=" + this.instanceNumber 
				+ "\n hostClass=" + this.hostClass + "\n hostAttribute=" + this.hostAttribute 
				+ "\n innerRole=" + this.innerRole + "\n   innerClass=" + this.innerClass + "\n   innerAttribute=" + this.innerAttribute
				+ "\n frames=" + this.frames + "\n constants=" + this.constants;
	}

	/**
	 * Verify that the innerClass of this decoder is consistent with the provided
	 * decoded inner classes. This is used when mapping compound utypes where
	 * multiple decoders are expected to reference the same inner class.
	 *
	 * @param utypeDecoders list of other decoders to compare against
	 * @throws MappingError when a mismatch is detected
	 */
	public void checkInnerClass(List<UtypeDecoder> utypeDecoders) throws MappingError {
		for(UtypeDecoder utypeDecoder: utypeDecoders) {
			if( this.innerClass.equals(utypeDecoder.getInnerClass()) == false ) {
				throw new MappingError("Unconsistant class: " + this.innerClass + " and " + utypeDecoder.getInnerClass());
			}
		}
	}
}