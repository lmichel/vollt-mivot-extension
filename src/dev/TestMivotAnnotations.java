package dev;

import main.annoter.dm.EpochPosition;
import main.annoter.dm.FrameFactory;
import main.annoter.dm.FrameHolder;
import main.annoter.dm.Glossary;
import main.annoter.dm.Brightness;
import main.annoter.dm.MangoInstance;
import main.annoter.dm.Property;
import main.annoter.meta.MappingCache;
import main.annoter.meta.UtypeDecoder;
import main.annoter.mivot.MivotAnnotations;
import tap.metadata.TAPColumn;
import tap.metadata.TAPTable;

import java.lang.reflect.Constructor;
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
		tapColumns.add(new TAPColumn("dec", "description", "deg", "ucd",
				"mango:EpochPosition.latitude[CS.spaceSys=ICRS CT.epoch=J2000]"));
		tapColumns.add(new TAPColumn("ra", "description", "deg", "ucd",
				"mango:EpochPosition.longitude[CS.spaceSys=ICRS CT.epoch=J2000]"));
		tapColumns.add(new TAPColumn("pmdec", "description", "mas / yr", "ucd",
				"mango:EpochPosition.pmLatitude[CS.spaceSys=ICRS CT.epoch=J2000]"));
		tapColumns.add(new TAPColumn("pmra", "description", "mas / yr", "ucd",
				"mango:EpochPosition.pmLongitude[CS.spaceSys=ICRS CT.epoch=J2000]"));
		tapColumns.add(new TAPColumn("parallax", "description", "mas", "ucd",
				"mango:EpochPosition.parallax[CS.spaceSys=ICRS CT.epoch=J2000]"));
		tapColumns.add(new TAPColumn("rvz_radvel", "description", "km / s", "ucd",
				"mango:EpochPosition.radialVelocity[CS.spaceSys=ICRS CT.epoch=J2000]"));

		tapColumns.add(new TAPColumn("coo_err_maja", "description", "mas", "ucd",
				"mango:EpochPosition.errors.position/mango:error.PErrorEllipse.majorAxis[CS.spaceSys=ICRS CT.epoch=J2000]"));
		tapColumns.add(new TAPColumn("coo_err_mina", "description", "mas", "ucd",
				"mango:EpochPosition.errors.position/mango:error.PErrorEllipse.minorAxis[CS.spaceSys=ICRS CT.epoch=J2000]"));
		tapColumns.add(new TAPColumn("coo_err_angle", "description", "deg", "ucd",
				"mango:EpochPosition.errors.position/mango:error.PErrorEllipse.angle[CS.spaceSys=ICRS CT.epoch=J2000]"));

		tapColumns.add(new TAPColumn("pm_err_maja", "description", "mas / yr", "ucd",
				"mango:EpochPosition.errors.properMotion/mango:error.PErrorEllipse.majorAxis[CS.spaceSys=ICRS CT.epoch=J2000]"));
		tapColumns.add(new TAPColumn("pm_err_mina", "description", "mas / yr", "ucd",
				"mango:EpochPosition.errors.properMotion/mango:error.PErrorEllipse.minorAxis[CS.spaceSys=ICRS CT.epoch=J2000]"));
		tapColumns.add(new TAPColumn("pm_err_angle", "description", "deg", "ucd",
				"mango:EpochPosition.errors.properMotion/mango:error.PErrorEllipse.angle[CS.spaceSys=ICRS CT.epoch=J2000]"));
		
		tapColumns.add(new TAPColumn("magnitude_k", "description", "mag", "ucd",
				"mango:Brightness.value[CS.photCal=K]"));
		tapColumns.add(new TAPColumn("mag_k_error", "description", "mag", "ucd",
				"mango:Brightness.error/mango:error.PErrorSym1D.sigma[CS.photCal=K]"));
		
		tapColumns.add(new TAPColumn("magnitude_u", "description", "mag", "ucd",
				"mango:Brightness.value[CS.photCal=u]"));
		tapColumns.add(new TAPColumn("mag_u_errorup", "description", "mag", "ucd",
				"mango:Brightness.error/mango:error.PErrorASym1D.high[CS.photCal=u]"));
		tapColumns.add(new TAPColumn("mag_u_errordown", "description", "mag", "ucd",
				"mango:Brightness.error/mango:error.PErrorASym1D.low[CS.photCal=u]"));
		
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
		//sreturn Arrays.asList("magnitude_u", "mag_u_errorup");
		//return Arrays.asList("color_uv");
		return Arrays.asList("main_id", "ra", "dec", "coo_err_maja", "coo_err_mina", "coo_err_angle", "pmra", "pmdec",
				"pm_err_maja", "pm_err_mina", "pm_err_angle", "parallax", "rvz_radvel", 
				"magnitude_u", "mag_u_errorup", "mag_u_errordown", "magnitude_k", "mag_k_error");


	}

	public static void main(String[] args) throws Exception {
		// to be done at service startup
		buildCache();

		// get metadata from the parser
		String table = getFromTable();
		List<String> selectedColumns = getSelectedColumns();

		// Build the annotations
		MivotAnnotations mivotAnnotation = new MivotAnnotations();		
		
		// Build the MANGO instance with the column used as identifier

		MangoInstance mi = new MangoInstance(MAPPING_CACHE.getUtypeMappedColumn(table, "mango:MangoObject.identifier"));
		FrameFactory frameFactory = FrameFactory.getInstance();

		for( String supportedProperty : Glossary.SUPPORTED_PROPERTIES ) {
			// Look for mapping rules for the property in the current table
			Map<String, List<UtypeDecoder>> propertyMapping = MAPPING_CACHE.getTableMapping(
					table,
					"mango:" + supportedProperty, 
					selectedColumns);
			for( String key : propertyMapping.keySet()) {	
				List<FrameHolder> frameHolders = new ArrayList<>();
				System.out.println(supportedProperty + " mapping: " + key );
				List<UtypeDecoder> utds = propertyMapping.get(key);
				for(String cs : utds.get(0).getFrames()) {
					System.out.println(" Creating frame for CS: " + cs);
					FrameHolder fh = frameFactory.createFrame(cs);
					frameHolders.add(fh);	
					mivotAnnotation.addModel(
								fh.systemClass,
								frameFactory.models.get(fh.systemClass));
					
					mivotAnnotation.addGlobals(fh.frameXml);
				}
				Property property = (Property) Property.getInstance(
						supportedProperty,
						utds,
						table,
						frameHolders);
				mi.addMangoProperties(property);
			}
		}

		mivotAnnotation.addModel(
				Glossary.ModelPrefix.IVOA,
				Glossary.VodmlUrl.IVOA);
		mivotAnnotation.addModel(
				Glossary.ModelPrefix.MANGO,
				Glossary.VodmlUrl.MANGO);
		for(String model: FrameFactory.getInstance().models.keySet()) {
			mivotAnnotation.addModel(
					model,
					FrameFactory.getInstance().models.get(model));
		}

		mivotAnnotation.addTemplates(mi);

		mivotAnnotation.buildMivotBlock("");
		System.out.println(mivotAnnotation.mivotBlock);
	}

}
