package main.vollt_tuning;

import java.io.BufferedWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import adql.db.DBColumn;
import adql.db.DefaultDBTable;
import adql.parser.ParseException;
import adql.query.ADQLQuery;
import adql.query.from.ADQLTable;
import adql.query.from.FromContent;
import main.annoter.cache.Cache;
import main.annoter.cache.MappingCache;
import main.annoter.dm.EpochPosition;
import main.annoter.dm.MangoInstance;
import main.annoter.meta.Glossary;
import main.annoter.mivot.MivotAnnotations;
import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPExecutionReport;
import tap.formatter.VOTableFormat;
import tap.metadata.TAPColumn;
import tap.metadata.TAPTable;
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
		MappingCache MAPPING_CACHE = MappingCache.getCache();

		this.service.getLogger().log(LogLevel.INFO, "MIVOT", "@ MIVOT", null);
		String query = execReport.parameters.getQuery();

		Map<String, Set<String>> columns = new LinkedHashMap<String, Set<String>>();
		
		ADQLQuery parsedQuery = null;
		try {
			parsedQuery = this.service.getFactory().createADQLParser().parseQuery(query);
		} catch (ParseException | TAPException e) {
			e.printStackTrace();
			this.writeMappingError(e.toString(), out);
			return;
		}
		Cache.setLogger(this.service.getLogger());
		
		FromContent from = parsedQuery.getFrom();
		for( ADQLTable tapTable: from.getTables()) {
			//MAPPING_CACHE.addADQLTable(tapTable);
			/*
			 * Use the hard-coded Simbad mapping meanwhile 
			 * pseudo UTypes are not set
			 */
			MAPPING_CACHE.getFakeMappingCacheForBasic();
			MAPPING_CACHE.getFakeMappingCacheForFlux();
		}

		StringBuffer message = new StringBuffer();
		if( this.isQueryMappable(parsedQuery, message) == true ) {
			Instant start = Instant.now();
			

			for(DBColumn col : execReport.resultingColumns) {
				String table = col.getTable().getADQLName();
				if( columns.keySet().contains(table) == false) {
					columns.put(table, new HashSet<String>());
				}
				columns.get(table).add(col.getADQLName());
			}
			Cache.logDebug("Start writing annotations for tables ", columns.keySet().toString());
			MivotAnnotations mivotAnnotations = new MivotAnnotations();
			String outXml = mivotAnnotations.mapMango(columns);
			Duration duration = Duration.between(start, Instant.now());
			Cache.logDebug("Annotations generated in", duration.toMillis() + " ms");
			try {
				out.write(outXml);
				out.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			Cache.logInfo(message.toString());
			this.writeMappingError(message.toString(), out);			
		}
	}
	
	/**
	 * Returns true if the query is considered as providing a mappable result
	 * @TODO refine the criteria 
	 */
	private boolean isQueryMappable(ADQLQuery parsedQuery, StringBuffer message) {
		FromContent from = parsedQuery.getFrom();
		if( from.getTables().size() == 0 ) {
			message.append("Annotation requires at least one table");
			return false;
		}
		if( from.getTables().size() == 1 && from.getTables().get(0).getSchemaName() != null ) {
			String schema =  from.getTables().get(0).getSchemaName().toLowerCase();
			if( schema.indexOf("tap_schema") != -1 ) {
				message.append("Queries on TAP_SCHEMA cannot be annotated");
				return false;
			}
		}

		return true;
	}
	
	private void writeMappingError(String message, BufferedWriter out) {
		MivotAnnotations mivotAnnotations = new MivotAnnotations();
		mivotAnnotations.setReport(false, "Mapping failure: " + message);
		try {
			mivotAnnotations.buildMivotBlock("");
			out.write(mivotAnnotations.mivotBlock);
			out.flush();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}


