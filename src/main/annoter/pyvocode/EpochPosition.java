package main.annoter.pyvocode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import main.annoter.UtypeDecoder;
import tap.metadata.TAPColumn;

public class EpochPosition extends Property {
	// public MivotInstance mivotInstance;
	public static String DMTYPE = "mango:EpochPosition";
	public static List<String> ATTRIBUTES = Arrays.asList("latitude", "longitude", "pmLatitude", "pmLongitude",
			"properMotion", "radialVelocity", "parallax");
	public List<String> frames;

	public EpochPosition(MappingCache mappingCache, String tableName, List<String> selectedColumns)
			throws MappingError {
		super(DMTYPE, null, null, new HashMap<String, String>() {
			{
				put("description", "6 parameters position");
				put("uri", "https://www.ivoa.net/rdf/uat/2024-06-25/uat.html#astronomical-location");
				put("label", "Astronomical location");
			}
		});
		List<UtypeDecoder> mappableColumns = mappingCache.getTableMapping(tableName, DMTYPE);
		String spaceSys = null;
		for (UtypeDecoder mappableColumn : mappableColumns) {
			String attribute = mappableColumn.getHostAttribute();
			TAPColumn tapColumn = mappableColumn.getTapColumn();
			String adqlName = tapColumn.getADQLName();
			this.frames = mappableColumn.getFrames();
			if (ATTRIBUTES.contains(attribute) && selectedColumns.contains(adqlName)) {
				this.addAttribute("ivoa:RealQuantity", DMTYPE + "." + attribute, adqlName, tapColumn.getUnit());
				for (String frame : this.frames) {
					if (frame.startsWith("spaceSys")) {
						spaceSys = frame.replace("spaceSys=", "_spaceframe_") + "_BARYCENTER";
					}
				}
			}
		}
		MivotInstance erri = null;
		if ((erri = this.buildEpochErrors(mappingCache, tableName, selectedColumns)) != null) {
			this.addInstance(erri);
		}
		if (spaceSys != null) {
			this.addReference(DMTYPE + ".spaceSys", spaceSys);
		}
	}

	private MivotInstance buildEpochErrors(MappingCache mappingCache, String tableName, List<String> selectedColumns)
			throws MappingError {
		List<UtypeDecoder> mappableColumns = mappingCache.getTableMapping(tableName, DMTYPE);
		List<UtypeDecoder> positionUtypes = new ArrayList<UtypeDecoder>();
		List<UtypeDecoder> pmUtypes = new ArrayList<UtypeDecoder>();
		for (UtypeDecoder mappableColumn : mappableColumns) {
			String attribute = mappableColumn.getHostAttribute();
			TAPColumn tapColumn = mappableColumn.getTapColumn();
			String adqlName = tapColumn.getADQLName();

			if (attribute.equals("errors") && selectedColumns.contains(adqlName)) {
				if (mappableColumn.getInnerRole().equals("position")) {
					positionUtypes.add(mappableColumn);
				}
				if (mappableColumn.getInnerRole().equals("properMotion")) {
					pmUtypes.add(mappableColumn);
				}
			}
		}
		MivotInstance errorInstance = new MivotInstance(positionUtypes.get(0).getInnerClass(),
				DMTYPE + ".errors",
				null);
		boolean mapped = false;
		MivotInstance positionError;
		if (positionUtypes.isEmpty() == false) {
			positionError = this.buildFlatInstance("mango:EpochPositionErrors.position", positionUtypes);
			errorInstance.addInstance(positionError);
			mapped = true;
		}
		MivotInstance pmError;
		if (pmUtypes.isEmpty() == false) {
			pmError = this.buildFlatInstance("mango:EpochPositionErrors.properMotion", pmUtypes);
			errorInstance.addInstance(pmError);
			mapped = true;
		}

		return (mapped) ? errorInstance : null;
	}

	private MivotInstance buildFlatInstance(String role, List<UtypeDecoder> utypeList) throws MappingError {
		MivotInstance flatInstance = new MivotInstance(utypeList.get(0).getInnerClass(), role, null);
		for (UtypeDecoder mappableColumn : utypeList) {
			flatInstance.addAttribute("ivoa:RealQuantity",
					utypeList.get(0).getInnerClass() + "." + mappableColumn.getInnerAttribute(),
					mappableColumn.getTapColumn().getADQLName(),
					mappableColumn.getTapColumn().getUnit());
		}
		return flatInstance;

	}
}
