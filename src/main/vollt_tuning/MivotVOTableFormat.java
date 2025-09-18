package main.vollt_tuning;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import adql.db.DBColumn;
import adql.db.DefaultDBTable;
import adql.query.ADQLQuery;
import main.annoter.dm.EpochPosition;
import main.annoter.dm.Glossary;
import main.annoter.dm.MangoInstance;
import main.annoter.meta.MappingCache;
import main.annoter.mivot.MivotAnnotations;
import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPExecutionReport;
import tap.formatter.VOTableFormat;
import tap.metadata.TAPColumn;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOTableVersion;
import uws.service.log.UWSLog.LogLevel;

/**
 * Overload the VOTableFormat with the capability of generating MIVOT annotations based
 * on the Utypes stored in TAP_SCHMA.columns
 */
public class MivotVOTableFormat extends VOTableFormat {
	
	/**
	 * Force TABLEDATA as output data format
	 * @param service
	 * @throws NullPointerException
	 */
	public MivotVOTableFormat(final ServiceConnection service) throws NullPointerException {
		super(service, DataFormat.TABLEDATA);
	}

	/**
	 * Write annotation just in between the header and the data
	 */
	@Override
	public void writeHeader(final VOTableVersion votVersion, final TAPExecutionReport execReport,
			final BufferedWriter out) throws IOException, TAPException {

		super.writeHeader(votVersion, execReport, out);
		this.writeAnnotations(execReport, out);
		out.flush();
	}

	/**
	 * generate MIVOT annotations based on the Utypes stored in TAP_SCHMA.columns
	 * @param execReport
	 * @param out
	 */
	private void writeAnnotations(final TAPExecutionReport execReport, final BufferedWriter out) {
		String query = execReport.parameters.getQuery();

		String tableName;
		List<String> columnNames = new ArrayList<String>();
		// Build the annotations
		MivotAnnotations mivotAnnotation = new MivotAnnotations();
		try {
			ADQLQuery parsedQuery = this.service.getFactory().createADQLParser().parseQuery(query);
			tableName = parsedQuery.getFrom().getName();
			
			if( this.isQueryMappable(parsedQuery) == true ) {
				this.service.getLogger().log(LogLevel.INFO, "MIVOT", "Start writing annotations for table " + tableName, null);

				MappingCache mappingCache = MappingCache.getCache();
				for(DBColumn col : execReport.resultingColumns) {
					columnNames.add(col.getADQLName());	
					mappingCache.addTAPColumn((TAPColumn)col);
				}

				// space frame is hard-coed meanwhile knowing how to get it from the TAP_SCHEMA
				mivotAnnotation.addDefaultSpaceFrame();
				// Build the MANGO instance with the column used as identifier (if any)
				MangoInstance mi = new MangoInstance(mappingCache.getUtypeMappedColumn(tableName,
						"mango:MangoObject.identifier", columnNames));

				// Look for mapping rules for EpochPosition in the current table
				System.out.println(mappingCache.getTableMapping(tableName, "mango:EpochPosition"));
				if (mappingCache.getTableMapping(tableName, "mango:EpochPosition", columnNames).isEmpty() == false) {
					EpochPosition epochPosition = new EpochPosition(mappingCache, tableName, columnNames);
					mi.addMangoProperties(epochPosition);
					mivotAnnotation.addModel(
							Glossary.ModelPrefix.MANGO,
							"https://ivoa.net/xml/MANGO/MANGO-V1.vodml.xml");
				} else {
					mivotAnnotation.setReport(false, "No mapping rules for the EpochPosition in table " + tableName);
				}
				mivotAnnotation.addTemplates(mi);
			} else {
				mivotAnnotation.setReport(false, "No mappable column can be identified ");
			}
		} catch (Exception e1) {
			this.service.getLogger().log(LogLevel.ERROR, "MIVOT", "Annotation process failed: " + e1, null);
			e1.printStackTrace();
			mivotAnnotation.setReport(false, "Mapping process failed" + e1.toString());

		}
		try {
			// We consider working with the first data table: no need for any table ID
			mivotAnnotation.buildMivotBlock("");
			out.write(mivotAnnotation.mivotBlock);
			out.flush();
		} catch (Exception e) {
			// TODO Put a logger message here
			e.printStackTrace();
			mivotAnnotation.setReport(false, "Mapping process failed: " + e.toString());
		} 
	}
	
	/**
	 * Returns true if the query is considered as providing a mappable result
	 * @TODO refine the criteria 
	 */
	private boolean isQueryMappable(ADQLQuery parsedQuery) {
		return parsedQuery.getFrom().getTables().size() == 1;
	}

}


