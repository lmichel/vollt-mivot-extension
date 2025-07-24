package dev;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import adql.db.DefaultDBTable;
import main.annoter.UtypeDecoder;
import main.annoter.pyvocode.EpochPosition;
import main.annoter.pyvocode.MangoInstance;
import main.annoter.pyvocode.MappingCache;
import main.annoter.pyvocode.MivotAnnotations;
import main.annoter.pyvocode.MivotInstance;
import tap.metadata.TAPColumn;

public class TestMivotAnnotations {
	private static MappingCache MAPPING_CACHE;

	private static void buildCache() {
		List<TAPColumn> tapColumns = new ArrayList<TAPColumn>();

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

		MAPPING_CACHE = new MappingCache();
		for (TAPColumn tapColumn : tapColumns) {
			tapColumn.setTable(new DefaultDBTable("basic", "basic"));
			MAPPING_CACHE.addTAPColumn(tapColumn);
		}
	}

	private static String getFromTable() {
		return "basic";

	}

	private static List<String> getSelectedColumns() {
		return Arrays.asList("ra", "dec", "coo_err_maja", "coo_err_mina", "coo_err_angle", "pmra", "pmdec",
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
		mivotAnnotation.addDefaultSpaceFrame();
		MangoInstance mi = new MangoInstance("_not_implemented_yet_");

		if (MAPPING_CACHE.getTableMapping(table, "mango:EpochPosition").isEmpty() == false) {
			EpochPosition epochPosition = new EpochPosition(MAPPING_CACHE, table, columns);
			mi.addMangoProperties(epochPosition);
			mivotAnnotation.addModel("mango",
					"https://raw.githubusercontent.com/ivoa-std/MANGO/refs/heads/wd-v1.0/vo-dml/mango.vo-dml.xml");
		}

		mivotAnnotation.addTemplates(mi);
		mivotAnnotation.buildMivotBlock("", false);
		System.out.println(mivotAnnotation.mivotBlock);
	}

}
