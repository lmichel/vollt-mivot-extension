package main.vollt_tuning;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import adql.db.DBColumn;
import adql.db.DefaultDBTable;
import main.annoter.dm.EpochPosition;
import main.annoter.dm.Glossary;
import main.annoter.dm.MangoInstance;
import main.annoter.meta.MappingCache;
import main.annoter.meta.UtypeDecoder;
import main.annoter.mivot.MivotAnnotations;
import tap.ServiceConnection;
import tap.TAPException;
import tap.TAPExecutionReport;
import tap.formatter.VOTableFormat;
import tap.metadata.TAPColumn;
import uk.ac.starlink.votable.DataFormat;
import uk.ac.starlink.votable.VOTableVersion;
import uws.service.log.UWSLog.LogLevel;

public class CustomVOTableFormat extends VOTableFormat {

	public CustomVOTableFormat(final ServiceConnection service) throws NullPointerException {
		super(service, DataFormat.TABLEDATA);
	}

	@Override
	public void writeHeader(final VOTableVersion votVersion, final TAPExecutionReport execReport,
			final BufferedWriter out) throws IOException, TAPException {

		super.writeHeader(votVersion, execReport, out);
		this.writeAnnotations(execReport, out);
		out.flush();
	}

	private void writeAnnotations(final TAPExecutionReport execReport, final BufferedWriter out) {
		String query = execReport.parameters.getQuery();

		String tableName;
		List<String> columnNames = new ArrayList<String>();
		// Build the annotations
		MivotAnnotations mivotAnnotation = new MivotAnnotations();
		try {

			tableName = this.service.getFactory().createADQLParser().parseQuery(query).getFrom().getName();

			if( this.isQueryMappable() == true ) {
				this.service.getLogger().log(LogLevel.INFO, "MIVOT", "Start writing annotations for table " + tableName, null);

				for(DBColumn col : execReport.resultingColumns) {
					columnNames.add(col.getADQLName());					
				}

				MappingCache mappingCache = this.getFakeMappingCache();

				// space frame is hard-coed meanwhile knowing how to get it from the TAP_SCHEMA
				mivotAnnotation.addDefaultSpaceFrame();

				// Build the MANGO instance with the column used as identifier (if any)
				MangoInstance mi = new MangoInstance(mappingCache.getUtypeMappedColumn(tableName,
						"mango:MangoObject.identifier"));

				// Look for mapping rules for EpochPosition in the current table
				if (mappingCache.getTableMapping(tableName, "mango:EpochPosition").isEmpty() == false) {
					EpochPosition epochPosition = new EpochPosition(mappingCache, tableName, columnNames);
					mi.addMangoProperties(epochPosition);
					mivotAnnotation.addModel(
							Glossary.ModelPrefix.MANGO,
							"https://ivoa.net/xml/MANGO/MANGO-V1.vodml.xml");
					mivotAnnotation.addTemplates(mi);
				} else {
					mivotAnnotation.setReport(false, "No mapping rules for the EpochPosition in table " + tableName);
				}
			} else {
				mivotAnnotation.setReport(false, "No mappable column can be identified ");
			}
		} catch (Exception e1) {
			this.service.getLogger().log(LogLevel.ERROR, "MIVOT", "Annotation process failed: " + e1, null);
			mivotAnnotation.setReport(false, "Mapping process failed" + e1.toString());

		}
		try {
			mivotAnnotation.buildMivotBlock("");
			out.write(mivotAnnotation.mivotBlock);
		} catch (Exception e) {
			mivotAnnotation.setReport(false, "Mapping process failed" + e.toString());
		}
	}
	
	private boolean isQueryMappable() {
		return true;
	}

	/**
	 * for test purpose
	 */
	private MappingCache getFakeMappingCache() {
		List<TAPColumn> tapColumns = new ArrayList<TAPColumn>();

		tapColumns.add(new TAPColumn("main_id", "description", "", "ucd",
				"mango:MangoObject.identifier"));
		tapColumns.add(new TAPColumn("dec", "description", "deg", "ucd",
				"mango:EpochPosition.latitude[CS.spaceSys=ICRS]"));
		tapColumns.add(new TAPColumn("ra", "description", "deg", "ucd",
				"mango:EpochPosition.longitude[CS.spaceSys=ICRS]"));
		tapColumns.add(new TAPColumn("pmdec", "description", "mas / yr", "ucd",
				"mango:EpochPosition.pmLatitude"));
		tapColumns.add(new TAPColumn("pmra", "description", "mas / yr", "ucd",
				"mango:EpochPosition.pmLongitude"));
		tapColumns.add(new TAPColumn("parallax", "description", "mas", "ucd",
				"mango:EpochPosition.parallax"));
		tapColumns.add(new TAPColumn("rvz_radvel", "description", "km / s", "ucd",
				"mango:EpochPosition.radialVelocity"));
		tapColumns.add(new TAPColumn("coo_err_maja", "description", "mas", "ucd",
				"mango:EpochPosition.errors.position/mango:error.PErrorEllipse.majorAxis"));
		tapColumns.add(new TAPColumn("coo_err_mina", "description", "mas", "ucd",
				"mango:EpochPosition.errors.position/mango:error.PErrorEllipse.minorAxis"));
		tapColumns.add(new TAPColumn("coo_err_angle", "description", "deg", "ucd",
				"mango:EpochPosition.errors.position/mango:error.PErrorEllipse.angle"));
		tapColumns.add(new TAPColumn("pm_err_maja", "description", "mas / yr", "ucd",
				"mango:EpochPosition.errors.properMotion/mango:error.PErrorEllipse.majorAxis"));
		tapColumns.add(new TAPColumn("pm_err_mina", "description", "mas / yr", "ucd",
				"mango:EpochPosition.errors.properMotion/mango:error.PErrorEllipse.minorAxis"));
		tapColumns.add(new TAPColumn("pm_err_angle", "description", "deg", "ucd",
				"mango:EpochPosition.errors.properMotion/mango:error.PErrorEllipse.angle"));

		MappingCache cache = MappingCache.getCache();
		for (TAPColumn tapColumn : tapColumns) {
			tapColumn.setTable(new DefaultDBTable("basic", "basic"));
			cache.addTAPColumn(tapColumn);
		}
		return cache;
	}
}


