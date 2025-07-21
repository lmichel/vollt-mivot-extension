/*This class is used to get files
 * JSON
 * XML
 * and also used to get the path 
 * */

package main.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.net.URL;

public class FileGetter {

	private static String JSON_LOCATION = "mappingComponent/";
	//private static String XML_LOCATION = "mappingComponent/Components/";
	private static String XML_LOCATION = "mappingComponent/";
	
	public static File getJSONFile(String service, String fileName) throws URISyntaxException, FileNotFoundException{
		URL resource;
		String location = JSON_LOCATION + service + "/" + fileName;
		System.out.println(JSON_LOCATION + service + "/" + fileName);
		resource =FileGetter.class.getClassLoader().getResource(location);
		if( resource == null ) {
			throw new FileNotFoundException(location + " not found");
		}
		return(new File(resource.toURI()));	
	}
	
	public static File getXMLFile(String service, String fileName) throws URISyntaxException, FileNotFoundException{
		String location = XML_LOCATION+service + "/"+fileName;
		URL resource = FileGetter.class.getClassLoader().getResource(location);
		if( resource == null ) {
			throw new FileNotFoundException(location + " not found");
		}
		return(new File(resource.toURI()));	
	}
			
}
