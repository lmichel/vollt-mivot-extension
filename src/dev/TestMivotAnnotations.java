package dev;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import adql.db.DefaultDBTable;
import main.annoter.dm.EpochPosition;
import main.annoter.dm.MangoInstance;
import main.annoter.meta.MappingCache;
import main.annoter.meta.UtypeDecoder;
import main.annoter.mivot.MivotAnnotations;
import tap.metadata.TAPColumn;

public class TestMivotAnnotations {
	private static MappingCache MAPPING_CACHE;

	private static void buildCache() {
		List<TAPColumn> tapColumns = new ArrayList<TAPColumn>();

		tapColumns.add(new TAPColumn("main_id", "description", "", "ucd",
				"mango:MangoObject.identifier"));
		tapColumns.add(
				new TAPColumn("dec", "description", "deg", "ucd", "mango:EpochPosition.latitude[CS.spaceSys=ICRS]"));
		tapColumns.add(
				new TAPColumn("ra", "description", "deg", "ucd", "mango:EpochPosition.longitude[CS.spaceSys=ICRS]"));
		tapColumns.add(new TAPColumn("pmdec", "description", "mas / yr", "ucd", "mango:EpochPosition.pmLatitude"));
		tapColumns.add(new TAPColumn("pmra", "description", "mas / yr", "ucd", "mango:EpochPosition.pmLongitude"));
		tapColumns.add(new TAPColumn("parallax", "description", "mas", "ucd", "mango:EpochPosition.parallax"));
		tapColumns
				.add(new TAPColumn("rvz_radvel", "description", "km / s", "ucd", "mango:EpochPosition.radialVelocity"));

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

		MAPPING_CACHE = MappingCache.getCache();
		for (TAPColumn tapColumn : tapColumns) {
			tapColumn.setTable(new DefaultDBTable("basic", "basic"));
			MAPPING_CACHE.addTAPColumn(tapColumn);
		}
	}

	private static String getFromTable() {
		return "basic";

	}

	private static List<String> getSelectedColumns() {
		return Arrays.asList("main_id", "ra", "dec", "coo_err_maja", "coo_err_mina", "coo_err_angle", "pmra", "pmdec",
				"pm_err_maja", "pm_err_mina", "pm_err_angle", "parallax", "rvz_radvel");

	}

	public static void main(String[] args) throws Exception {
		// to be done at service startup
		buildCache();

		// get metadata from the parser
		String table = getFromTable();
		List<String> columns = getSelectedColumns();

		// Build the annotations
		MivotAnnotations mivotAnnotation = new MivotAnnotations();
		// space frame is hard-coed meanwhile knowing how to get it from the TAP_SCHEMA
		mivotAnnotation.addDefaultSpaceFrame();
		
		
		// Build the MANGO instance with the column used as identifier
		MangoInstance mi = new MangoInstance(MAPPING_CACHE.getUtypeMappedColumn(table, "mango:MangoObject.identifier"));

		// Look for mapping rules for EpochPosition in the current table
		if (MAPPING_CACHE.getTableMapping(table, "mango:EpochPosition").isEmpty() == false) {
			EpochPosition epochPosition = new EpochPosition(MAPPING_CACHE, table, columns);
			mi.addMangoProperties(epochPosition);
			mivotAnnotation.addModel(
					"mango",
					"https://ivoa.net/xml/MANGO/MANGO-V1.vodml.xml");
			mivotAnnotation.addTemplates(mi);
		} else {
			mivotAnnotation.setReport(false, "No mapping rules for the EpochPosition in table " + table);
		}

		mivotAnnotation.buildMivotBlock("");
		System.out.println(mivotAnnotation.mivotBlock);
	}

}
