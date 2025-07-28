package main.annoter.meta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tap.metadata.TAPColumn;

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
		for( UtypeDecoder utypeDecoder: this.getTableMapping(adqlTableName).values()) {
			if( utypeDecoder.getHostClass().equals(hostClass)) {
				tableMapping.add(utypeDecoder);
			}
		}
		return tableMapping;
	}
	
	public  String getUtypeMappedColumn(String adqlTableName, String utype) {
		for( UtypeDecoder utypeDecoder: this.getTableMapping(adqlTableName).values()) {
			if( utypeDecoder.getUtype().equals(utype)) {
				return utypeDecoder.getTapColumn().getADQLName();
			}
		}
		return null;

	}
}
