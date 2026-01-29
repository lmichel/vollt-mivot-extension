package main.annoter.cache;

import tap.metadata.TAPColumn;
import tap.metadata.TAPTable;
import adql.db.DBColumn;
import adql.db.SearchColumnList;
import adql.query.from.ADQLTable;
import main.annoter.meta.UtypeDecoder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collect along with the Tomcat time of life all mapped columns adn provides methods telling
 * whether selected columns are mapped or not and which aare their related UTypes
 */
public class MappingCache {
	private Map<String, Map<String, UtypeDecoder>> utypeMap;
    private static MappingCache CACHE;
    private List<String> storedTables = new ArrayList<String>();
    
    
	private MappingCache() {
		this.utypeMap = new LinkedHashMap<String, Map<String, UtypeDecoder>>();
	}
	
	public static MappingCache getCache(){
		if( CACHE == null ) {
			CACHE = new MappingCache();
		}
		return CACHE;
	}
	
	public void addTAPTable(TAPTable tapTable) {
		
		String tableName = tapTable.getADQLName();
		if( this.storedTables.contains(tableName)) {
			return;
		}
		this.storedTables.add(tableName);

		Iterator<TAPColumn> it = tapTable.getColumns();
		while (it.hasNext()) {
			TAPColumn tapColumn = (TAPColumn) it.next();
			String uType = tapColumn.getUtype();
			if (uType != null && uType.startsWith("mango:") ){
				this.addTAPColumn(tapColumn);
			}
		}
	}
	
	public void addADQLTable(ADQLTable tapTable) {
		
		String tableName = tapTable.getName();
		if( this.storedTables.contains(tableName)) {
			return;
		}
		this.storedTables.add(tableName);

		SearchColumnList columnList =  tapTable.getDBColumns();
		for(DBColumn column: columnList) {
			if( column instanceof TAPColumn) {
				TAPColumn tapColumn = (TAPColumn)column;
				String uType = tapColumn.getUtype();
				if (uType != null && uType.startsWith("mango:") ){
					this.addTAPColumn(tapColumn);
				}
			}
		}
	}
	
	public void addTAPColumn(TAPColumn tapColumn) {
		if( tapColumn.getUtype() == null) {
			return;
		}
		String tableName = tapColumn.getTable().getADQLName();
		if( this.utypeMap.containsKey(tableName) == false ) {
			this.utypeMap.put(tableName, new LinkedHashMap<String, UtypeDecoder>());
		}
		UtypeDecoder utypeDecoder = new UtypeDecoder(tapColumn);
		this.utypeMap.get(tableName).put(tapColumn.getADQLName(), utypeDecoder);
	}
	
	public Map<String, UtypeDecoder> getTableMapping(String adqlTableName){
		return this.utypeMap.get(adqlTableName);
	}
	
	public List<UtypeDecoder> getTableMapping(String adqlTableName, String hostClass){
		List<UtypeDecoder> tableMapping = new ArrayList<UtypeDecoder>();
		if( this.getTableMapping(adqlTableName) == null ) {
			return tableMapping;
		}
		for( UtypeDecoder utypeDecoder: this.getTableMapping(adqlTableName).values()) {
			if( utypeDecoder.getHostClass().equals(hostClass)) {
				tableMapping.add(utypeDecoder);
			}
		}
		return tableMapping;
	}
	
	public List<UtypeDecoder> getTableMapping(String adqlTableName, String hostClass, String hostAttribute){
		List<UtypeDecoder> tableMapping = new ArrayList<UtypeDecoder>();
		if( this.getTableMapping(adqlTableName) == null ) {
			return tableMapping;
		}
		for( UtypeDecoder utypeDecoder: this.getTableMapping(adqlTableName).values()) {
			if( utypeDecoder.getHostClass().equals(hostClass) && utypeDecoder.getHostAttribute().equals(hostAttribute)) {
				tableMapping.add(utypeDecoder);
			}
		}
		return tableMapping;
	}
	
	public Map<String, List<UtypeDecoder>> getTableMapping(String adqlTableName, String hostClass, List<String> selectedColumns){
		Map<String, List<UtypeDecoder>> tableMapping = new LinkedHashMap<String, List<UtypeDecoder>>();
		if( this.getTableMapping(adqlTableName) == null ) {
			return tableMapping;
		}
		for( UtypeDecoder utypeDecoder: this.getTableMapping(adqlTableName).values()) {
			if( utypeDecoder.getHostClass().equals(hostClass)) {
				if( selectedColumns.contains(utypeDecoder.getTapColumn().getADQLName()) ) {
					String key = utypeDecoder.getConstantAndFrames();
					if( key == null) key = "default";
					if( tableMapping.containsKey(key) == false ) {
						tableMapping.put(key, new ArrayList<UtypeDecoder>());
					}
					tableMapping.get(key).add(utypeDecoder);
				}
			}
		}
		return tableMapping;
	}
	
	public  String getUtypeMappedColumn(String adqlTableName, String utype) {
		if( this.getTableMapping(adqlTableName) == null ) {
			return null;
		}
		for( UtypeDecoder utypeDecoder: this.getTableMapping(adqlTableName).values()) {
			if( utypeDecoder.getUtype() != null && utypeDecoder.getUtype().equals(utype)) {
				return utypeDecoder.getTapColumn().getADQLName();
			}
		}
		return null;
	}
	
	public  String getUtypeMappedColumn(String adqlTableName, String utype, List<String> selectedColumns) {
		if( this.getTableMapping(adqlTableName) == null ) {
			return null;
		}
		for( UtypeDecoder utypeDecoder: this.getTableMapping(adqlTableName).values()) {
			if( utypeDecoder.getUtype() != null && utypeDecoder.getUtype().equals(utype)) {
				String colName = utypeDecoder.getTapColumn().getADQLName();
				return (selectedColumns.contains(colName)?colName: null);
			}
		}
		return null;
	}
	
	/**
	 * for test purpose
	 */
	public void getFakeMappingCacheForBasic() {
		if( this.storedTables.contains("basic")) {
			return;
		}
		this.storedTables.add("basic");

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
		tapColumns.add(new TAPColumn("coo_err_maj", "description", "mas", "ucd",
				"mango:EpochPosition.errors.position/mango:error.PErrorEllipse.semiMajorAxis[CS.spaceSys=ICRS CT.epoch=J2000]"));
		tapColumns.add(new TAPColumn("coo_err_min", "description", "mas", "ucd",
				"mango:EpochPosition.errors.position/mango:error.PErrorEllipse.semiMinorAxis[CS.spaceSys=ICRS CT.epoch=J2000]"));
		tapColumns.add(new TAPColumn("coo_err_angle", "description", "deg", "ucd",
				"mango:EpochPosition.errors.position/mango:error.PErrorEllipse.angle[CS.spaceSys=ICRS CT.epoch=J2000]"));
		tapColumns.add(new TAPColumn("pm_err_maj", "description", "mas / yr", "ucd",
				"mango:EpochPosition.errors.properMotion/mango:error.PErrorEllipse.semiMajorAxis[CS.spaceSys=ICRS CT.epoch=J2000]"));
		tapColumns.add(new TAPColumn("pm_err_min", "description", "mas / yr", "ucd",
				"mango:EpochPosition.errors.properMotion/mango:error.PErrorEllipse.semiMinorAxis[CS.spaceSys=ICRS CT.epoch=J2000]"));
		tapColumns.add(new TAPColumn("pm_err_angle", "description", "deg", "ucd",
				"mango:EpochPosition.errors.properMotion/mango:error.PErrorEllipse.angle[CS.spaceSys=ICRS CT.epoch=J2000]"));
		tapColumns.add(new TAPColumn("otype", "Objet type", "", "",
				"mango:Label.text[CT.vocabulary=https://www.ivoa.net/rdf/uat#classification]"));

		MappingCache cache = MappingCache.getCache();
        final TAPTable basicTable = new TAPTable("basic", TAPTable.TableType.table);
        for (TAPColumn tapColumn : tapColumns) {
			basicTable.addColumn(tapColumn);
			cache.addTAPColumn(tapColumn);
		}
	}
	
	public void getFakeMappingCacheForFlux() {
		if( this.storedTables.contains("allfluxes")) {
			return;
		}
		this.storedTables.add("allfluxes");
		
		List<TAPColumn> tapColumns = new ArrayList<TAPColumn>();

		tapColumns.add(new TAPColumn("U", "description", "", "ucd",
				"mango:Brightness.value[CS.photCal=U]"));
		tapColumns.add(new TAPColumn("B", "description", "", "ucd",
				"mango:Brightness.value[CS.photCal=B]"));
		tapColumns.add(new TAPColumn("V", "description", "", "ucd",
				"mango:Brightness.value[CS.photCal=V]"));
		tapColumns.add(new TAPColumn("G", "description", "", "ucd",
				"mango:Brightness.value[CS.photCal=G]"));
		tapColumns.add(new TAPColumn("R", "description", "", "ucd",
				"mango:Brightness.value[CS.photCal=R]"));
		tapColumns.add(new TAPColumn("I", "description", "", "ucd",
				"mango:Brightness.value[CS.photCal=I]"));
		tapColumns.add(new TAPColumn("J", "description", "", "ucd",
				"mango:Brightness.value[CS.photCal=J]"));
		tapColumns.add(new TAPColumn("H", "description", "", "ucd",
				"mango:Brightness.value[CS.photCal=H]"));
		tapColumns.add(new TAPColumn("K", "description", "", "ucd",
				"mango:Brightness.value[CS.photCal=K]"));
		tapColumns.add(new TAPColumn("F150W", "description", "", "ucd",
				"mango:Brightness.value[CS.photCal=F150W]"));
		tapColumns.add(new TAPColumn("F200W", "description", "", "ucd",
				"mango:Brightness.value[CS.photCal=F200W]"));
		tapColumns.add(new TAPColumn("F444W", "description", "", "ucd",
				"mango:Brightness.value[CS.photCal=F444W]"));
		tapColumns.add(new TAPColumn("u_", "description", "", "ucd",
				"mango:Brightness.value[CS.photCal=u]"));
		tapColumns.add(new TAPColumn("g_", "description", "", "ucd",
				"mango:Brightness.value[CS.photCal=g]"));
		tapColumns.add(new TAPColumn("r_", "description", "", "ucd",
				"mango:Brightness.value[CS.photCal=r]"));
		tapColumns.add(new TAPColumn("z_", "description", "", "ucd",
				"mango:Brightness.value[CS.photCal=z]"));
		tapColumns.add(new TAPColumn("i_", "description", "", "ucd",
				"mango:Brightness.value[CS.photCal=i]"));


		MappingCache cache = MappingCache.getCache();
        final TAPTable basicTable = new TAPTable("allfluxes", TAPTable.TableType.table);
        for (TAPColumn tapColumn : tapColumns) {
			basicTable.addColumn(tapColumn);
			cache.addTAPColumn(tapColumn);
		}
	}
	
	
	
}
