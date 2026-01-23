package main.annoter.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import main.annoter.mivot.MappingError;
import tap.metadata.TAPColumn;

/**
 * UTypes can have 2 forms: 
 * - hostClass.hostAttribute
 *   - ex mango:EpochPosition.parallax
 *   - the parallax field of the EpochPosition class
 *   
 * - hostClass.hostAttribute.innerRole/innerClass.innerAttribute
 *   - ex mango:EpochPosition.errors.parallax/mango:error.PErrorSym1D.sigma1
 *   - the sigma1 attribute of parallax component of the EpochPosition.error instance.
 *     The parallew component is instance of mango:error.PErrorSym1D.
 * 
 * UTypes can be followed by [CS=id CT=value] where
 * - CS refers to a coordinate systems whichapply to the column.
 *   The interpretation of id is hard coded in the tool. (not implemented yet)
 * - CT refers to a literal value that comes in addition to the filed mapping
 *   ex: CT=year in the case of an ObsDate will make the date representation as a year format
 *   The way to process this extra field is hard coded in the tool. (not implemented yet) 
 * 
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
	
	String CLASS_NAME = "[A-Z][\\w]+";
	String PACKAGE= "[a-z][a-z0-9_]+";
	String FIELD = "[a-z][a-zA-Z0-9_]+";
	String SIMPLE_ROLE = "(" + PACKAGE + ":" + "(?:" + PACKAGE + "\\.)?" + CLASS_NAME + ")\\.(" + FIELD + ")";
	String COMPOUND =  "^" + SIMPLE_ROLE + "\\.(" + FIELD + ")\\/" + SIMPLE_ROLE + "$";
	String SHORT_COMPOUND =  "^" + SIMPLE_ROLE + "\\/" + SIMPLE_ROLE + "$";

	public UtypeDecoder(TAPColumn tapColumn ){
		this.tapColumn = tapColumn;
		this.utype = this.tapColumn.getUtype() ;
		String[] eles = utype.split(":");

		this.extractConstantsAndFrames();
    	if( this.processSimpleRole() == false && this.processCompound() == false ) {
			System.out.println("UTYPE not valid: " + this.utype);
			System.exit(1);
		}  else {
			return;
		}
		
		if( utype.matches("^#\\d\\-.*")) {
			this.instanceNumber = Integer.parseInt(utype.substring(1,2));
			this.utype = this.utype.substring(3);
		}
	
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
		this.extractConstantsAndFrames();

	}
	
	private boolean processSimpleRole() {
        Pattern pattern = Pattern.compile("^" + SIMPLE_ROLE + "$");
        Matcher matcher = pattern.matcher(this.utype);

        if (matcher.find()) {
            this.hostClass = matcher.group(1);
            this.hostAttribute = matcher.group(2);
        } else {
            return false;
        }
        return true;
    }

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

	private void extractConstantsAndFrames() {
        String input = "prefix text [CS.name=John CT.age=30]";
        input = this.utype;
        String cs_regexp =  "[\\:#a-zA-Z0-9\\./]";
        String regex = "([^\\[]*)(\\[(C(?:S|T)\\.\\w+="+ cs_regexp + "+\\s?)+\\])";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.matches()) {
            this.utype = matcher.group(1).trim();

            this.constantAndFrames = matcher.group(2);
            if (this.constantAndFrames != null) {
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

	public void checkInnerClass(List<UtypeDecoder> utypeDecoders) throws MappingError {
		for(UtypeDecoder utypeDecoder: utypeDecoders) {
			if( this.innerClass.equals(utypeDecoder.getInnerClass()) == false ) {
				throw new MappingError("Unconsistant class: " + this.innerClass + " and " + utypeDecoder.getInnerClass());
			}
		}
	}
}
