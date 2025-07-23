package main.annoter.pyvocode;

import java.util.Arrays;
import java.util.List;

import main.annoter.UtypeDecoder;
import tap.metadata.TAPColumn;

public class EpochPosition {
	public MivotInstance mivotInstance;
	public static String DMTYPE = "mango:EpochPosition";
	public static List<String> ATTRIBUTES = Arrays.asList("latitude", "longitude");
	
	public EpochPosition(MappingCache mappingCache, String tableName, List<String> selectedColumns) throws MappingError {
		
		this.mivotInstance = new MivotInstance(DMTYPE);
		List<UtypeDecoder> mappableColumns = mappingCache.getTableMapping(tableName, DMTYPE);
		String spaceSys = null;
		for( UtypeDecoder mappableColumn: mappableColumns ) {
			String attribute = mappableColumn.getHostAttribute();
			TAPColumn tapColumn =  mappableColumn.getTapColumn();
			String adqlName = tapColumn.getADQLName();
			List<String> frames = mappableColumn.getFrames();
			if( ATTRIBUTES.contains(attribute) && selectedColumns.contains(adqlName) ){
				this.mivotInstance.addAttribute("ivoa:RealQuantity", DMTYPE + "." + attribute, adqlName, tapColumn.getUnit()); 
				for( String frame: frames) {
					if( frame.startsWith("spaceSys") ){
						spaceSys = frame.replace("spaceSys=", "__spaceSys_");
					}
				}
			}			
		}
		if( spaceSys != null) {
			this.mivotInstance.addReference(DMTYPE + ".spaceSys" , spaceSys);
		}
	}
	
	public String xmlString() {
		return this.mivotInstance.xmlString();
	}
}
