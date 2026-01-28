package dev;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import main.annoter.cache.MappingCache;
import main.annoter.mivot.MivotAnnotations;
import tap.metadata.TAPColumn;
import tap.metadata.TAPTable;

public class TestMivotAnnotations {
	private static MappingCache MAPPING_CACHE;
    public static TAPTable basicTable = new TAPTable("basic", TAPTable.TableType.table);
    public static TAPTable photometryTable = new TAPTable("photometry", TAPTable.TableType.table);
    public static TAPTable colorTable = new TAPTable("color", TAPTable.TableType.table);

	private static void buildCache() {
		basicTable.addColumn(new TAPColumn("main_id", "description", "", "ucd",
				"mango:MangoObject.identifier"));
		basicTable.addColumn(new TAPColumn("dec", "description", "deg", "ucd",
				"mango:EpochPosition.latitude[CS.spaceSys=ICRS CT.epoch=J2000]"));
		basicTable.addColumn(new TAPColumn("ra", "description", "deg", "ucd",
				"mango:EpochPosition.longitude[CS.spaceSys=ICRS CT.epoch=J2000]"));
		basicTable.addColumn(new TAPColumn("pmdec", "description", "mas / yr", "ucd",
				"mango:EpochPosition.pmLatitude[CS.spaceSys=ICRS CT.epoch=J2000]"));
		basicTable.addColumn(new TAPColumn("pmra", "description", "mas / yr", "ucd",
				"mango:EpochPosition.pmLongitude[CS.spaceSys=ICRS CT.epoch=J2000]"));
		
		basicTable.addColumn(new TAPColumn("parallax", "description", "mas", "ucd",
				"mango:EpochPosition.parallax[CS.spaceSys=ICRS CT.epoch=J2000]"));
		basicTable.addColumn(new TAPColumn("rvz_radvel", "description", "km / s", "ucd",
				"mango:EpochPosition.radialVelocity[CS.spaceSys=ICRS CT.epoch=J2000]"));

		basicTable.addColumn(new TAPColumn("coo_err_maja", "description", "mas", "ucd",
				"mango:EpochPosition.errors.position/mango:error.PErrorEllipse.majorAxis[CS.spaceSys=ICRS CT.epoch=J2000]"));
		basicTable.addColumn(new TAPColumn("coo_err_mina", "description", "mas", "ucd",
				"mango:EpochPosition.errors.position/mango:error.PErrorEllipse.minorAxis[CS.spaceSys=ICRS CT.epoch=J2000]"));
		basicTable.addColumn(new TAPColumn("coo_err_angle", "description", "deg", "ucd",
				"mango:EpochPosition.errors.position/mango:error.PErrorEllipse.angle[CS.spaceSys=ICRS CT.epoch=J2000]"));

		basicTable.addColumn(new TAPColumn("pm_err_maja", "description", "mas / yr", "ucd",
				"mango:EpochPosition.errors.properMotion/mango:error.PErrorEllipse.majorAxis[CS.spaceSys=ICRS CT.epoch=J2000]"));
		basicTable.addColumn(new TAPColumn("pm_err_mina", "description", "mas / yr", "ucd",
				"mango:EpochPosition.errors.properMotion/mango:error.PErrorEllipse.minorAxis[CS.spaceSys=ICRS CT.epoch=J2000]"));
		basicTable.addColumn(new TAPColumn("pm_err_angle", "description", "deg", "ucd",
				"mango:EpochPosition.errors.properMotion/mango:error.PErrorEllipse.angle[CS.spaceSys=ICRS CT.epoch=J2000]"));
		basicTable.addColumn(new TAPColumn("otype", "Objet type", "", "",
				"mango:Label.text[CT.vocabulary=https://www.ivoa.net/rdf/uat#classification]"));
		
 		

		photometryTable.addColumn(new TAPColumn("magnitude_U", "description", "mag", "ucd",
				"mango:Brightness.value[CS.photCal=U]"));
		photometryTable.addColumn(new TAPColumn("magnitude_k", "description", "mag", "ucd",
				"mango:Brightness.value[CS.photCal=K]"));
		photometryTable.addColumn(new TAPColumn("mag_k_error", "description", "mag", "ucd",
				"mango:Brightness.error/mango:error.PErrorSym1D.sigma[CS.photCal=K]"));		
		photometryTable.addColumn(new TAPColumn("magnitude_u", "description", "mag", "ucd",
				"mango:Brightness.value[CS.photCal=u]"));
		photometryTable.addColumn(new TAPColumn("mag_u_errorup", "description", "mag", "ucd",
				"mango:Brightness.error/mango:error.PErrorASym1D.high[CS.photCal=u]"));
		photometryTable.addColumn(new TAPColumn("mag_u_errordown", "description", "mag", "ucd",
				"mango:Brightness.error/mango:error.PErrorASym1D.low[CS.photCal=u]"));
		
		colorTable.addColumn(new TAPColumn("color_ur", "description", "mag", "ucd",
				"mango:Color.value[CS.photFilterHigh=u CS.photFilterLow=r CT.mode=colorindex]"));

		MAPPING_CACHE = MappingCache.getCache();
		MAPPING_CACHE.addTAPTable(basicTable);
		MAPPING_CACHE.addTAPTable(photometryTable);
		MAPPING_CACHE.addTAPTable(colorTable);

	}

	private static Map<String, Set<String>>  getSelectedColumns() {
		//return Arrays.asList("main_id", "ra", "dec", "coo_err_maja", "coo_err_mina", "coo_err_angle", "pmra", "pmdec",
		//		"pm_err_maja", "pm_err_mina", "pm_err_angle", "parallax", "rvz_radvel");
		//return Arrays.asList("magnitude_u", "mag_error_u", "magnitude_k", "mag_k_error");
		//return Arrays.asList("magnitude_u", "mag_u_errordown", "mag_u_errorup");
		//return Arrays.asList("magnitude_u", "mag_u_errorup");
		//return Arrays.asList("color_ur");
		//return Arrays.asList("main_id", "ra", "dec", "coo_err_maja", "coo_err_mina", "coo_err_angle", "pmra", "pmdec",
		//  	"pm_err_maja", "pm_err_mina", "pm_err_angle", "parallax", "rvz_radvel", 
		//		"magnitude_u", "mag_u_errorup", "mag_u_errordown", "magnitude_k", "mag_k_error");
		//return Arrays.asList("otype");
		Map<String, Set<String>> basicTableMap = new LinkedHashMap<String, Set<String>>();
		basicTableMap.put("basic", new HashSet<String>(
				Arrays.asList("main_id", "ra", "dec", "coo_err_maja", "coo_err_mina", "coo_err_angle", "pmra", "pmdec",
				          	  "pm_err_maja", "pm_err_mina", "pm_err_angle", "parallax", "rvz_radvel")));
		basicTableMap.put("photometry", new HashSet<String>(
				Arrays.asList("magnitude_u", "mag_u_errorup", "mag_u_errordown", "magnitude_k", "mag_k_error")));
		basicTableMap.put("color", new HashSet<String>(
				Arrays.asList("color_ur")));
		return basicTableMap;

	}

	public static void main(String[] args) throws Exception {
		// to be done at service startup
		buildCache();
		// get metadata from the parser
		 Map<String, Set<String>> selectedColumns = getSelectedColumns();

		Instant start = Instant.now();
		String outXml = (new MivotAnnotations()).mapMango(selectedColumns);
		Duration duration = Duration.between(start, Instant.now());
		System.out.println("Elapsed: " + duration.toMillis() + " ms");
		System.out.println("=========================");
		start = Instant.now();
		
		String outXml2 = (new MivotAnnotations()).mapMango(selectedColumns);
		duration = Duration.between(start, Instant.now());
		System.out.println("Elapsed: " + duration.toMillis() + " ms");
		System.out.println("Are both output identical? " + outXml.equals(outXml2));
		diffLines(outXml, outXml2);
	}
	
	public static void diffLines(String a, String b) {
	    String[] A = a.split("\n");
	    String[] B = b.split("\n");

	    int max = Math.max(A.length, B.length);
	    for (int i = 0; i < max; i++) {
	        String la = i < A.length ? A[i] : "<none>";
	        String lb = i < B.length ? B[i] : "<none>";

	        if (!la.equals(lb)) {
	            System.out.println("Line " + (i + 1));
	            System.out.println("- " + la);
	            System.out.println("+ " + lb);
	        }
	    }
	}
}
