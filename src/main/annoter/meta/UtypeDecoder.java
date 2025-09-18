package main.annoter.meta;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private  List<String> frames = new ArrayList<String>();
	private  List<String> constants = new ArrayList<String>();
	
	String CLASS_NAME = "[A-Z][\\w]+";
	String PACKAGE= "[a-z][a-z0-9_]+";
	String FIELD = "[a-z][a-zA-Z0-9_]+";
	String SIMPLE_ROLE = "(" + PACKAGE + ":" + "(?:" + PACKAGE + "\\.)?" + CLASS_NAME + ")\\.(" + FIELD + ")";
	String COMPOUND =  "^" + SIMPLE_ROLE + "\\.(" + FIELD + ")\\/" + SIMPLE_ROLE + "$";

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
        } else {
            return false;
        }
        return true;
    }

	private void extractConstantsAndFrames() {
        String input = "prefix text [CS.name=John CT.age=30]";
        input = this.utype;
        String regex = "([^\\[]*)(\\[(C(?:S|T)\\.\\w+=\\w+\\s?)+\\])?";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.matches()) {
            this.utype = matcher.group(1).trim();

            String bracketedBlock = matcher.group(2);
            if (bracketedBlock != null) {
                Matcher innerMatcher = Pattern.compile("C(?:S|T)\\.\\w+=\\w+").matcher(bracketedBlock);
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
            System.out.println("No match for input: " + input);
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
	public List<String> getConstants() {
		return this.constants;
	}
	public TAPColumn getTapColumn() {
		return this.tapColumn;
	}
	public String getUtype() {
		return this.utype;
	}
	public String toString() {
		return "utype=" + this.utype + "\n instanceNumber=" + this.instanceNumber 
				+ "\n hostClass=" + this.hostClass + "\n hostAttribute=" + this.hostAttribute 
				+ "\n innerRole=" + this.innerRole + "\n   innerClass=" + this.innerClass + "\n   innerAttribute=" + this.innerAttribute
				+ "\n frames=" + this.frames + "\n constants=" + this.constants;
	}

}
