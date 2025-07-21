package main.annoter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


import tap.metadata.TAPColumn;
import utils.FileGetter;

/**
 * class modeling the JSON profile for a particular table
 */
public class JsonProfile {

	public String identifier = "NoIdentifier";
	public List<Property> properties = new ArrayList();
	public String tableName;
	public JSONObject globalConfigJson;
	
	
	public JsonProfile(String service, List<TAPColumn> tapColumns) throws Exception {
		this.globalConfigJson = (JSONObject) new JSONObject();
		this.globalConfigJson.put("properties", new JSONArray());
		JSONArray properties = (JSONArray) this.globalConfigJson.get("properties");


		for( TAPColumn tapColumn : tapColumns) {
			System.out.println(tapColumn.getUtype());
			//String utype = tapColumn.getUtype();
			RoleDecoder roleDecoder = new RoleDecoder(tapColumn.getUtype());
			
			System.out.println(roleDecoder);
			
			JSONObject snippet_object = this.addSnippetIfNotExists(roleDecoder.getSnippet());
				
			JSONObject attribute = new JSONObject();
			attribute.put("value", "@" + tapColumn.getADQLName() );
			attribute.put("unit", tapColumn.getUnit() );
			
			JSONObject attribute_object = new JSONObject();
			attribute_object.put(roleDecoder.getRole(), attribute);
			snippet_object.put(roleDecoder.getRole(), attribute);
			
			for( String key : roleDecoder.getFrames().keySet()) {
				attribute = new JSONObject();
				attribute.put("value",  roleDecoder.getFrames().get(key) );
				snippet_object.put(roleDecoder.getSnippet() + "." + key, attribute);

			}
			for( String key : roleDecoder.getConstants().keySet()) {
				String snippet = roleDecoder.getSnippet();
				String role = roleDecoder.getRole();
				int pos = role.lastIndexOf(".");
				String dmr = role.substring(pos+1);
				attribute = new JSONObject();
				attribute.put("value", roleDecoder.getConstants().get(key));
				snippet_object.put(role.replace(dmr, key), attribute);

				System.out.println("======= " + snippet + " " + role.replace(dmr, key) + " " + dmr);
			}
				
			
		}
	    this.cleanupKeys();
		this.setProperties();

        org.json.JSONObject orgJson = new org.json.JSONObject(this.globalConfigJson.toJSONString());
		System.out.println(orgJson.toString(4));

	}
	
	private void setProperties() {
		for(Iterator iterator = globalConfigJson.keySet().iterator(); iterator.hasNext();) {
		    String key = (String) iterator.next();
		    Object value = globalConfigJson.get(key);
		    if( key.equals( "mango:MangoObject.identifier")) {
		    	this.identifier = (String)value;
		    } else if( key.equals( "properties")) {
		    	JSONArray jproperties = (JSONArray)value;
				for( int i=0; i<jproperties.size(); i++) {
					Object property = jproperties.get(i);
			    	this.properties.add(new Property((JSONObject)property));

		    	}
		    }
		}
	}
	
	private void cleanupKeys() {
		JSONArray properties = (JSONArray) this.globalConfigJson.get("properties");
		for( int i=0; i<properties.size(); i++) {
			JSONObject property = (JSONObject) properties.get(i);
			for(Iterator iterator = property.keySet().iterator(); iterator.hasNext();) {
			    String key = (String) iterator.next();
			    if( key.startsWith("#")) {
			    	String new_key = key.replaceAll("#[0-9]*-", "");
			    	property.put(new_key, property.get(key));
			    	property.remove(key);
			    	//this.renameTaggedKey((JSONObject) property.get(new_key));
			    }
			}
		}
	}

	public static void renameTaggedKey(JSONObject object) {
    	Set<String> pks = ((JSONObject) object).keySet();
		for( String key : pks) {
			JSONObject property = (JSONObject) object.get(key);
		    if( key.startsWith("#")) {
		    	String new_key = key.replaceAll("#[0-9]*-", "");
		    	object.put(new_key, object.get(key));
		    	object.remove(key);
		    }
		}
	}
			

	
	private JSONObject addSnippetIfNotExists(String snippet) {
		System.out.println(snippet);
		JSONArray properties = (JSONArray) this.globalConfigJson.get("properties");
		JSONObject item_here = null;

		for( int i=0; i<properties.size(); i++) {
			JSONObject property = (JSONObject) properties.get(i);
			if( (item_here = (JSONObject) property.getOrDefault(snippet, null)) != null ) {
				System.out.println("found " +snippet );
				System.out.println(item_here );
				return item_here;			
				}
		}
		JSONObject property = new JSONObject();
		property.put(snippet, new JSONObject());
		properties.add(property);

		return (JSONObject) property.get(snippet);
		
	}
	
	public class RoleDecoder{
		private String utype;
		private String snippet;
		private String role;
		private  Map<String, String> frames = new HashMap<String, String>();
		private  Map<String, String> constants = new HashMap<String, String>();
		
		public RoleDecoder(String utype) {
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
	/**
	 * Inner class carrying the mapping of a particular MANGO property
	 */
	public class Property{
		/**
		 * model dmtpype of the property which is also the key within the mapping property
		 */
		public String dmtype;
		/**
		 * Name of the xml snippet that must be used as MIVOT counterpart of the property
		 */
		public String snippetName;
		/**
		 * JSON mapping of the property as copied from the mapping profile
		 */
		public JSONObject mapping;
		
		/**
		 * @param jelement: property mapping block
		 */
		public Property(JSONObject jelement) {
			Set<String>keys =  jelement.keySet();
			for( String key : keys) {
				this.dmtype = key;
				// This a filename: no ":"
				this.snippetName = key.replace(":", ".");
				this.mapping = (JSONObject) jelement.get(key);
				break;
			}
			/*
			 * This case is no longer used but
			 * it can revive
			 */
			String[] pc = this.dmtype.split("\\[");
			if( pc.length == 2 ) {
				this.dmtype = pc[0];
				this.snippetName = pc[0].replace(":", ".") + "." + pc[1].replace("]", "");
				}
			}
		
		/**
		 * @param dmrole: searched dmrole (without host role prefix)
		 * @return: the mapping of the property component which role
		 *          (possibly prefixed with host role) match the searched dmrole
		 */
		public JSONObject getDmrole(String dmrole) {
			Set<String> keySet =  this.mapping.keySet();
			for( String key: keySet ) {
				if( key.indexOf("/") != -1 && key.endsWith(dmrole)) {
					return (JSONObject) this.mapping.get(key);
				}				
			}
			return (JSONObject) this.mapping.get(dmrole);
		}
		
		
		/**
		 * @param dmrole: searched dmrole (without host role prefix)
		 * @return: the host role prefix if it exists, null otherwise
		 */
		public String getHostDmrole(String dmrole) {
			Set<String> keySet =  this.mapping.keySet();
			for( String key: keySet ) {
				if( key.indexOf("/") != -1 && key.endsWith(dmrole)) {
					return key.split("/")[0];
				}				
			}
			return null;
		}
	}
}

