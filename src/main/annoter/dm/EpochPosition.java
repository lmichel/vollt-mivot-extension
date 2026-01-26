/**
 * Commented by chatGPT 
 */
package main.annoter.dm;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.annoter.cache.MappingCache;
import main.annoter.meta.Glossary;
import main.annoter.meta.UtypeDecoder;
import main.annoter.mivot.FrameHolder;
import main.annoter.mivot.MivotInstance;
import tap.metadata.TAPColumn;

public class EpochPosition extends Property {

	public static final String DMTYPE = "mango:EpochPosition";
	public List<String> frames;
	private String tableName;
	private List<UtypeDecoder> positionErrorUtypes = new ArrayList<>();
	private List<UtypeDecoder> pmErrorUtypes = new ArrayList<>();
	private List<UtypeDecoder> parallaxErrorUtypes = new ArrayList<>();

	public EpochPosition(List<UtypeDecoder> utypeDecoders,
			String tableName,
			List<FrameHolder> frameHolders,
			List<String> constants) throws Exception {

		super(DMTYPE, null, null, new HashMap<String, String>() {
			{
			put("description", "6 parameters position");
			put("uri", "https://www.ivoa.net/rdf/uat/2024-06-25/uat.html#astronomical-location");
			put("label", "Astronomical location");
			}
		});
		this.tableName = tableName;
		

		String epoch = null;
		for (UtypeDecoder mappableColumn : utypeDecoders) {
			String attribute = mappableColumn.getHostAttribute();
			TAPColumn tapColumn = mappableColumn.getTapColumn();
			String adqlName = tapColumn.getADQLName();
			this.frames = mappableColumn.getFrames();
			if( mappableColumn.getInnerRole() == null ) {
				this.addAttribute("ivoa:RealQuantity", DMTYPE + "." + attribute, adqlName, tapColumn.getUnit());
			}
			if( epoch == null ) {
				epoch = mappableColumn.getConstant(Glossary.CTClass.EPOCH);
			}
		}
	
		if( epoch != null ) {
			this.addAttribute("year","mango:EpochPosition.obsDate",
					"*" + epoch.replace("J",""),
					null);

		}

		MivotInstance erri = this.buildEpochErrors();
		if (erri != null) {
			this.addInstance(erri);
		}
		
		for (FrameHolder fh : frameHolders) {
			if (fh.systemClass.equals(Glossary.CSClass.SPACE)) {
				this.addReference(DMTYPE + ".spaceSys", fh.frameId);
			}
			if (fh.systemClass.equals(Glossary.CSClass.TIME)) {
				this.addReference(DMTYPE + ".timeSys", fh.frameId);
			}
		}
	}


	private MivotInstance buildEpochErrors()
			throws Exception {
		MappingCache MAPPING_CACHE = MappingCache.getCache();

		List<UtypeDecoder> mappableColumns = MAPPING_CACHE.getTableMapping(this.tableName, DMTYPE);

		for (UtypeDecoder mappableColumn : mappableColumns) {
			if( "errors".equals(mappableColumn.getHostAttribute()) ){
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
