package main.annoter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.DocumentTraversal;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;
import org.xml.sax.SAXException;

import main.utils.XMLUtils;

public class SimpleAnnotationBuilder {
	
	public String tableName;
	List<String> columnNames;
	Set<String> frames = new TreeSet<String>();
	String xmlContent = null;
	Writer votableStream = null;
	
	public SimpleAnnotationBuilder(String tableName,
			List<String> columnNames, 
			BufferedWriter votableStream) throws Exception{
		this.tableName = tableName;
		this.columnNames = columnNames;
		this.votableStream = votableStream;
	}

	public void buildMivotBlock()  throws Exception {

		this.xmlContent = "XMLUtils.toPrettyString(finalString, 2)";
	}

	public void writeAnnotations() throws IOException {
		if( this.votableStream == null ) {
			System.out.println(XMLUtils.toPrettyString(this.xmlContent, 2));
		} else {
			this.votableStream.write(this.xmlContent);
		}
	}
	
	public void setAnnotationsAsFailed(String message) {
		this.xmlContent = getFailedAnnotations(message);
	}
	
	public static String  getFailedAnnotations(String message) {
		return  "<RESOURCE type=\"meta\">" +
			    "  <VODML xmlns=\"http://ivoa.net/xml/merged-synthax\">\n" +
		        "     <REPORT status=\"KO\">\n"+
		        "     Annotation process failed: " +message +"\n" +
        		"    </REPORT>\n" + 
		        "  </VODML>\n" +
                " </RESOURCE>\n";
	}
	
	public void addFramesInGlobals(Document templateDoc, TreeWalker  walker) throws Exception {
		walker.setCurrentNode(walker.getRoot());
		Element globalsNode = null;
		while (walker.nextNode() != null) {
			Element currentElement = (Element) walker.getCurrentNode();
			if( currentElement.getNodeName() == "GLOBALS" ) {
				globalsNode = currentElement;
			} 
		}

	}
	
	public List<Element> processTable() throws Exception {
		return null;
	}

}
