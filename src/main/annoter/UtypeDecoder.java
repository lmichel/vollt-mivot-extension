package main.annoter;

import java.util.HashMap;
import java.util.Map;

public class UtypeDecoder {
	private String utype;
	private String snippet;
	private String role;
	private  Map<String, String> frames = new HashMap<String, String>();
	private  Map<String, String> constants = new HashMap<String, String>();
	
	public UtypeDecoder(String utype) {
		this.utype = utype;
		String[] eles = utype.split(":");
		int bpos;
		if( (bpos = utype.indexOf("[")) > 0) {
			
		    String extension =  utype.substring(bpos + 1).replace("]", "");
			utype = utype.substring(0, bpos);
			String[] extensions = extension.split(" ");
			for( String ext: extensions) {
				if( ext.startsWith("CS.spaceSys") == true) {
					this.frames.put("spaceSys", ext.split("=")[1] );
				}
				else if( ext.startsWith("CS.timeSys") == true) {
					this.frames.put("timeSys", ext.split("=")[1] );
				} 					
				else if( ext.startsWith("CT.representation") == true) {
					this.constants.put("representation", ext.split("=")[1] );

				}

			}
		}

		if( eles.length == 1) {
			System.out.println("wrong format 1");
		} else if( eles.length == 2) {
			int idx = utype.lastIndexOf(".");
			this.snippet = utype.substring(0, idx);
		    this.role = utype.substring(0, idx) + "." +  utype.substring(idx + 1);

		} else if( eles.length > 2) {
			int idx = utype.indexOf("/");
			this.snippet = utype.substring(0, idx-1);
		    this.role =  utype.substring(idx + 1);
		}
	}
	
	public String getSnippet() {
		return this.snippet;
	}
	public String getRole() {
		return this.role;
	}
	public Map<String, String> getFrames() {
		return this.frames;
	}
	public Map<String, String> getConstants() {
		return this.constants;
		
	}
	public String toString() {
		return "utype=" + this.utype + " snippet=" + this.snippet + " role=" + this.role;
	}

}
