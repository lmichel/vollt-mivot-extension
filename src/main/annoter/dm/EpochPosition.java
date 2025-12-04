/**
 * Commented by chatGPT 
 */
package main.annoter.dm;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.annoter.meta.MappingCache;
import main.annoter.meta.UtypeDecoder;
import main.annoter.mivot.MappingError;
import main.annoter.mivot.MivotInstance;
import tap.metadata.TAPColumn;

public class EpochPosition extends Property {

	public static final String DMTYPE = "mango:EpochPosition";
	public List<String> frames;
	public String csSpaceSys;
	public String csTimeSys;
	private String tableName;
	private List<String> selectedColumns;
	private List<UtypeDecoder> positionErrorUtypes = new ArrayList<>();
	private List<UtypeDecoder> pmErrorUtypes = new ArrayList<>();
	private List<UtypeDecoder> parallaxErrorUtypes = new ArrayList<>();

	@SuppressWarnings("serial")
	public EpochPosition(String tableName, List<String> selectedColumns)
			throws Exception {

		super(DMTYPE, null, null, new HashMap<String, String>() {
			{
			put("description", "6 parameters position");
			put("uri", "https://www.ivoa.net/rdf/uat/2024-06-25/uat.html#astronomical-location");
			put("label", "Astronomical location");
			}
		});
		MappingCache MAPPING_CACHE = MappingCache.getCache();
		this.tableName = tableName;
		this.selectedColumns = selectedColumns;
		
		List<UtypeDecoder> mappableColumns = MAPPING_CACHE.getTableMapping(tableName, DMTYPE);
		String spaceSys = null;
		String timeSys = null;

		for (UtypeDecoder mappableColumn : mappableColumns) {
			String attribute = mappableColumn.getHostAttribute();
			TAPColumn tapColumn = mappableColumn.getTapColumn();
			String adqlName = tapColumn.getADQLName();
			this.frames = mappableColumn.getFrames();

			if (Glossary.Roles.EPOCH_POSITION.contains(attribute) && selectedColumns.contains(adqlName)) {
				this.addAttribute("ivoa:RealQuantity", DMTYPE + "." + attribute, adqlName, tapColumn.getUnit());

				for (String frame : this.frames) {
					if (frame.startsWith("spaceSys")) {
						spaceSys = frame.replace("spaceSys=", "_spaceframe_") + "_BARYCENTER";
						this.csSpaceSys = frame.replace("spaceSys=", "");
					}
					if (frame.startsWith("timeSys")) {
						timeSys = frame.replace("timeSys=", "_timeframe_") + "_BARYCENTER";
						this.csTimeSys = frame.replace("timeSys=", "");
					}
				}
			}
		}

		MivotInstance obsDate = new MivotInstance("mango:ObsDate", "mango:EpochPosition.obsDate", null);
		obsDate.addAttribute("ivoa:string","mango:DateTime.representation", "*year", null);
		obsDate.addAttribute("ivoa:datetime","mango:DateTime.dateTime", 2000.0, null);
		this.addInstance(obsDate);

		MivotInstance erri = this.buildEpochErrors();
		if (erri != null) {
			this.addInstance(erri);
		}

		if (spaceSys != null) {
			this.addReference(DMTYPE + ".spaceSys", spaceSys);
		}
		if (timeSys != null) {
			this.addReference(DMTYPE + ".timeSys", spaceSys);
		}
	}


	private MivotInstance buildEpochErrors()
			throws Exception {
		MappingCache MAPPING_CACHE = MappingCache.getCache();

		List<UtypeDecoder> mappableColumns = MAPPING_CACHE.getTableMapping(this.tableName, DMTYPE);

		for (UtypeDecoder mappableColumn : mappableColumns) {
			String attribute = mappableColumn.getHostAttribute();
			TAPColumn tapColumn = mappableColumn.getTapColumn();
			String adqlName = tapColumn.getADQLName();

			if (attribute.equals("errors") && this.selectedColumns.contains(adqlName)) {
				if ("position".equals(mappableColumn.getInnerRole())) {
					mappableColumn.checkInnerClass(positionErrorUtypes);
					positionErrorUtypes.add(mappableColumn);
				}
				if ("properMotion".equals(mappableColumn.getInnerRole())) {
					mappableColumn.checkInnerClass(pmErrorUtypes);
					pmErrorUtypes.add(mappableColumn);
				}
				if ("parallax".equals(mappableColumn.getInnerRole())) {
					mappableColumn.checkInnerClass(parallaxErrorUtypes);
					parallaxErrorUtypes.add(mappableColumn);
				}
			}
		}

		MivotInstance errorInstance = new MivotInstance(
			"mango:EpochPositionErrors",
			DMTYPE + ".errors",
			null
		);

		boolean errorMapped = false;
		MivotInstance errorComponent;
		if( (errorComponent = this.buildErrorComponent("mango:EpochPositionErrors.position",
				this.positionErrorUtypes) ) != null ) {
			errorMapped = true;
			errorInstance.addInstance(errorComponent);
		}
		if( (errorComponent = this.buildErrorComponent("mango:EpochPositionErrors.properMotion",
				this.pmErrorUtypes) ) != null ) {
			errorMapped = true;
			errorInstance.addInstance(errorComponent);
		}
		if( (errorComponent = this.buildErrorComponent("mango:EpochPositionErrors.parallax",
				this.parallaxErrorUtypes) ) != null ) {
			errorMapped = true;
			errorInstance.addInstance(errorComponent);
		}
		return (errorMapped ==  true)? errorInstance: null;
	}
	
	private MivotInstance buildErrorComponent(String dmrole, List<UtypeDecoder> utypeDecoders)
			throws Exception {		
		if (utypeDecoders.isEmpty()) {
			return null;
		}
		PropertyError errorComponent = new PropertyError(dmrole, null, 0.68, utypeDecoders, null);			
		return errorComponent.getMivotInstance();
	}
}
