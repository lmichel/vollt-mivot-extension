package main.annoter.meta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import adql.db.DefaultDBTable;
import tap.metadata.TAPColumn;

/**
 * Collect along with the Tomcat time of life all mapped columns adn provides methods telling
 * whether selected columns are mapped or not and which aare their related UTypes
 */
public class MappingCache {
	private Map<String, Map<String, UtypeDecoder>> utypeMap;
    private static MappingCache CACHE;
    
    
	private MappingCache() {
		this.utypeMap = new LinkedHashMap<String, Map<String, UtypeDecoder>>();
	}
	
	public static MappingCache getCache(){
		if( CACHE == null ) {
			CACHE = new MappingCache();
		}
		return CACHE;
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
	
	public List<UtypeDecoder> getTableMapping(String adqlTableName, String hostClass, List<String> selectedColumns){
		List<UtypeDecoder> tableMapping = new ArrayList<UtypeDecoder>();
		if( this.getTableMapping(adqlTableName) == null ) {
			return tableMapping;
		}
		for( UtypeDecoder utypeDecoder: this.getTableMapping(adqlTableName).values()) {
			if( utypeDecoder.getHostClass().equals(hostClass)) {
				if( selectedColumns.contains(utypeDecoder.getTapColumn().getADQLName()) ) {
					tableMapping.add(utypeDecoder);
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
	private void getFakeMappingCache() {
		List<TAPColumn> tapColumns = new ArrayList<TAPColumn>();

		tapColumns.add(new TAPColumn("main_id", "description", "", "ucd",
				"mango:MangoObject.identifier"));
		tapColumns.add(new TAPColumn("dec", "description", "deg", "ucd",
				"mango:EpochPosition.latitude[CS.spaceSys=ICRS]"));
		tapColumns.add(new TAPColumn("ra", "description", "deg", "ucd",
				"mango:EpochPosition.longitude[CS.spaceSys=ICRS]"));
		tapColumns.add(new TAPColumn("pmdec", "description", "mas / yr", "ucd",
				"mango:EpochPosition.pmLatitude"));
		tapColumns.add(new TAPColumn("pmra", "description", "mas / yr", "ucd",
				"mango:EpochPosition.pmLongitude"));
		tapColumns.add(new TAPColumn("parallax", "description", "mas", "ucd",
				"mango:EpochPosition.parallax"));
		tapColumns.add(new TAPColumn("rvz_radvel", "description", "km / s", "ucd",
				"mango:EpochPosition.radialVelocity"));
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

		MappingCache cache = MappingCache.getCache();
		for (TAPColumn tapColumn : tapColumns) {
			tapColumn.setTable(new DefaultDBTable("basic", "basic"));
			cache.addTAPColumn(tapColumn);
		}
	}
}
