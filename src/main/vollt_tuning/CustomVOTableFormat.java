package main.vollt_tuning;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import adql.db.DBColumn;
import main.annoter.SimpleAnnotationBuilder;
import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPExecutionReport;
import tap.formatter.VOTableFormat;
import tap.metadata.TAPColumn;
import tap.metadata.TAPCoosys;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOSerializer;
import uk.ac.starlink.votable.VOTableVersion;
import uws.service.log.UWSLog.LogLevel;

public class CustomVOTableFormat extends VOTableFormat {
	
	public CustomVOTableFormat(final ServiceConnection service) throws NullPointerException {
		super(service, DataFormat.TABLEDATA);
	}

	
	private void writeAnnotations(final TAPExecutionReport execReport, final BufferedWriter out) {
		String query = execReport.parameters.getQuery();

		String tableName="";
		List<String> columnNames = new ArrayList<String>();
		try {
		
			tableName = this.service.getFactory().createADQLParser().parseQuery(query).getFrom().getName();
			this.service.getLogger().log(LogLevel.INFO, "MIVOT", "Start writing annotations for table " + tableName, null);

			for(DBColumn col : execReport.resultingColumns) {
				columnNames.add(col.getADQLName());					
			}

			SimpleAnnotationBuilder annotationBuilder = new SimpleAnnotationBuilder(tableName,columnNames,out);
			annotationBuilder.buildMivotBlock();
			this.service.getLogger().log(LogLevel.INFO, "MIVOT", "Annotation process succeded: write MIVOT block in the results VOTable", null);
			annotationBuilder.writeAnnotations();
			out.newLine();

		} catch (Exception e1) {
			this.service.getLogger().log(LogLevel.ERROR, "MIVOT", "Annotation process failed: " + e1, null);
			try { out.write(SimpleAnnotationBuilder.getFailedAnnotations(e1.toString()));}catch(Exception e) {}

		}
		
	}

	@Override
	public void writeHeader(final VOTableVersion votVersion, final TAPExecutionReport execReport, final BufferedWriter out) throws IOException, TAPException {

		/* ******************************************************************
		   *                                                                *
		   * NOTE:                                                          *
		   *   Tout ce qui suit est un copier-coller de la fonction         *
		   *   VOTableFormat.writeHeader(...). A changer selon les besoins  *
		   *   mais attention à respecter le schéma VOTable et les headers  *
		   *   TAP (surtout la description des colonnes). Le plus simple    *
		   *   étant d'ajouter les headers nécessaires à la fin (cf NOTE    *
		   *   plus bas).                                                   *
		   *                                                                *
		   ****************************************************************** */
		out.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		out.newLine();
		out.write("<VOTABLE" + VOSerializer.formatAttribute("version", votVersion.getVersionNumber()) + VOSerializer.formatAttribute("xmlns", votVersion.getXmlNamespace()) + VOSerializer.formatAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance") + VOSerializer.formatAttribute("xsi:schemaLocation", votVersion.getXmlNamespace() + " " + votVersion.getSchemaLocation()) + ">");
		out.newLine();

		out.write("<RESOURCE type=\"results\">");
		out.newLine();
		



		// Indicate that the query has been successfully processed:	[REQUIRED]
		out.write("<INFO name=\"QUERY_STATUS\" value=\"OK\"/>");
		out.newLine();

		// Append the PROVIDER information (if any):	[OPTIONAL]
		if (service.getProviderName() != null) {
			out.write("<INFO name=\"PROVIDER\"" + VOSerializer.formatAttribute("value", service.getProviderName()) + ">" + ((service.getProviderDescription() == null) ? "" : VOSerializer.formatText(service.getProviderDescription())) + "</INFO>");
			out.newLine();
		}

		// Append the ADQL query at the origin of this result:	[OPTIONAL]
		String adqlQuery = execReport.parameters.getQuery();
		if (adqlQuery != null) {
			out.write("<INFO name=\"QUERY\"" + VOSerializer.formatAttribute("value", adqlQuery) + "/>");
			out.newLine();
		}

		// Append the fixed ADQL query, if any:	[OPTIONAL]
		String fixedQuery = execReport.fixedQuery;
		if (fixedQuery != null) {
			out.write("<INFO name=\"QUERY_AFTER_AUTO_FIX\"" + VOSerializer.formatAttribute("value", fixedQuery) + "/>");
			out.newLine();
		}

		
		// Insert the definition of all used coordinate systems:
		HashSet<String> insertedCoosys = new HashSet<String>(10);
		for(DBColumn col : execReport.resultingColumns) {
			// ignore columns with no coossys:
			if (col instanceof TAPColumn && ((TAPColumn)col).getCoosys() != null) {
				// get its coosys:
				TAPCoosys coosys = ((TAPColumn)col).getCoosys();
				// insert the coosys definition ONLY if not already done because of another column:
				if (!insertedCoosys.contains(coosys.getId())) {
					// write the VOTable serialization of this coordinate system definition:
					out.write("<COOSYS" + VOSerializer.formatAttribute("ID", coosys.getId()));
					if (coosys.getSystem() != null)
						out.write(VOSerializer.formatAttribute("system", coosys.getSystem()));
					if (coosys.getEquinox() != null)
						out.write(VOSerializer.formatAttribute("equinox", coosys.getEquinox()));
					if (coosys.getEpoch() != null)
						out.write(VOSerializer.formatAttribute("epoch", coosys.getEpoch()));
					out.write(" />");
					out.newLine();
					// remember this coosys has already been written:
					insertedCoosys.add(coosys.getId());
				}
			}
		}
		this.writeAnnotations(execReport, out);
		out.flush();
	}
	


	/**
	 * @param doc the document to convert
	 * @return a string that matches the content of the document
	 * 
	 * This method is used to convert an xml Document to a String
	 */
	public static String xmlToString(Document doc) {
		
	    String xmlString = null;
	    
	    try {
	        Source source = new DOMSource(doc);
	        StringWriter stringWriter = new StringWriter();
	        Result result = new StreamResult(stringWriter);
	        TransformerFactory factory = TransformerFactory.newInstance();
	        Transformer transformer = factory.newTransformer();
	        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
	        	        
	        transformer.transform(source, result);
	        xmlString = stringWriter.getBuffer().toString();
	    } catch (TransformerConfigurationException e) {
	        e.printStackTrace();
	    } catch (TransformerException e) {
	        e.printStackTrace();
	    }
	    return xmlString;
	}
	
	/**
	 * @param xml the XML string we want to beautify
	 * @param indent the number of indentations we want
	 * @return a beautified XML string
	 */
	public static String toPrettyString(String xml, int indent) {
	    try {
	        // Turn xml string into a document
	        Document document = DocumentBuilderFactory.newInstance()
	                .newDocumentBuilder()
	                .parse(new InputSource(new ByteArrayInputStream(xml.getBytes("utf-8"))));

	        // Remove whitespaces outside tags
	        document.normalize();
	        XPath xPath = XPathFactory.newInstance().newXPath();
	        NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']",
	                                                      document,
	                                                      XPathConstants.NODESET);

	        for (int i = 0; i < nodeList.getLength(); ++i) {
	            Node node = nodeList.item(i);
	            node.getParentNode().removeChild(node);
	        }

	        // Setup pretty print options
	        TransformerFactory transformerFactory = TransformerFactory.newInstance();
	        transformerFactory.setAttribute("indent-number", indent);
	        Transformer transformer = transformerFactory.newTransformer();
	        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
	        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

	        // Return pretty print xml string
	        StringWriter stringWriter = new StringWriter();
	        transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
	        return stringWriter.toString();
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    }
	}
	
}


