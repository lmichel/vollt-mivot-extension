package dev;

import main.annoter.dm.EpochPosition;
import main.annoter.dm.Brightness;
import main.annoter.dm.MangoInstance;
import main.annoter.meta.MappingCache;
import main.annoter.meta.UtypeDecoder;
import main.annoter.mivot.MivotAnnotations;
import tap.metadata.TAPColumn;
import tap.metadata.TAPTable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
		
		tapColumns.add(new TAPColumn("magnitude_k", "description", "mag", "ucd",
				"mango:Brightness.value[CS.photcal=K]"));
		tapColumns.add(new TAPColumn("mag_k_error", "description", "mag", "ucd",
				"mango:Brightness.error/mango:error.PErrorSym1D.sigma[CS.photcal=K]"));
		
		tapColumns.add(new TAPColumn("magnitude_u", "description", "mag", "ucd",
				"mango:Brightness.value[CS.photcal=u]"));
		tapColumns.add(new TAPColumn("mag_u_errorup", "description", "mag", "ucd",
				"mango:Brightness.error/mango:error.PErrorASym1D.high[CS.photcal=u]"));
		tapColumns.add(new TAPColumn("mag_u_errordown", "description", "mag", "ucd",
				"mango:Brightness.error/mango:error.PErrorASym1D.low[CS.photcal=u]"));
		
		tapColumns.add(new TAPColumn("color_uv", "description", "mag", "ucd",
				"mango:Color.value[CS.high=u CS.low=v CT.mode=color]"));

		MAPPING_CACHE = MappingCache.getCache();
        final TAPTable basicTable = new TAPTable("basic", TAPTable.TableType.table);
		for (TAPColumn tapColumn : tapColumns) {
			basicTable.addColumn(tapColumn);
			MAPPING_CACHE.addTAPColumn(tapColumn);
		}
	}

	private static String getFromTable() {
		return "basic";

	}

	private static List<String> getSelectedColumns() {
		//return Arrays.asList("main_id", "ra", "dec", "coo_err_maja", "coo_err_mina", "coo_err_angle", "pmra", "pmdec",
		//		"pm_err_maja", "pm_err_mina", "pm_err_angle", "parallax", "rvz_radvel");
		//return Arrays.asList("magnitude_u", "mag_error_u", "magnitude_k", "mag_k_error");
		//return Arrays.asList("magnitude_u", "mag_u_errordown", "mag_u_errorup");
		return Arrays.asList("magnitude_u", "mag_u_errorup");
		//return Arrays.asList("color_uv");

	}

	public static void main(String[] args) throws Exception {
		// to be done at service startup
		buildCache();

		// get metadata from the parser
		String table = getFromTable();
		List<String> selectedcColumns = getSelectedColumns();

		// Build the annotations
		MivotAnnotations mivotAnnotation = new MivotAnnotations();		
		
		// Build the MANGO instance with the column used as identifier
		MangoInstance mi = new MangoInstance(MAPPING_CACHE.getUtypeMappedColumn(table, "mango:MangoObject.identifier"));

		// Look for mapping rules for EpochPosition in the current table
		if (MAPPING_CACHE.getTableMapping(table, "mango:EpochPosition", selectedcColumns).isEmpty() == false) {
			EpochPosition epochPosition = new EpochPosition(table, selectedcColumns);
			mi.addMangoProperties(epochPosition);
			mivotAnnotation.addModel(
					"mango",
					"https://ivoa.net/xml/MANGO/MANGO-V1.vodml.xml");
			mivotAnnotation.addSpaceSys(epochPosition.csSpaceSys);
		} else {
			mivotAnnotation.setReport(false, "No mapping rules for the EpochPosition in table " + table);
		}
		
		Map<String, List<UtypeDecoder>> brightnessMapping = MAPPING_CACHE.getTableMapping(table, "mango:Brightness", selectedcColumns);
		for( String key : brightnessMapping.keySet()) {
			List<UtypeDecoder> utds = brightnessMapping.get(key);
			System.out.println("Brightness mapping: " + key );
			Brightness brightness = new Brightness(utds, table);
			mivotAnnotation.addPhoCal(brightness.getPhotCalID());
			mi.addMangoProperties(brightness);
			mivotAnnotation.addModel(
					"phtotdm",
					"https://ivoa.net/xml/PHOTDM.vodml.xml");		
		}
		Map<String, List<UtypeDecoder>> colorMapping = MAPPING_CACHE.getTableMapping(table, "mango:Color", selectedcColumns);
		for( String key : colorMapping.keySet()) {
			List<UtypeDecoder> utds = colorMapping.get(key);
			System.out.println("Color mapping for key: " + key);
			for(UtypeDecoder utd: utds) {
				System.out.println("colorMapping mapping: " + key + " -> " + utd.getUtype() + " -> " + utd.getTapColumn().getADQLName());
				System.out.println(utd);
			}
		}
		
		mivotAnnotation.addTemplates(mi);

		mivotAnnotation.buildMivotBlock("");
		System.out.println(mivotAnnotation.mivotBlock);
	}

}
