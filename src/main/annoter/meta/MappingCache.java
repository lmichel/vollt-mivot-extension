package main.annoter.meta;

import tap.metadata.TAPColumn;
import tap.metadata.TAPTable;
import adql.db.DBColumn;
import adql.db.SearchColumnList;
import adql.query.from.ADQLTable;

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
			System.out.println("@@@ No mapping for table " + adqlTableName);
			return tableMapping;
		}
		System.out.println("@@@ Selected columns: " + selectedColumns);
		for( UtypeDecoder utypeDecoder: this.getTableMapping(adqlTableName).values()) {
			if( utypeDecoder.getHostClass().equals(hostClass)) {
				System.out.println("@@@ Checking " + utypeDecoder.getTapColumn().getADQLName());
				if( selectedColumns.contains(utypeDecoder.getTapColumn().getADQLName()) ) {
					System.out.println("@@@ Mapped " + utypeDecoder.getTapColumn().getADQLName());
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
}
