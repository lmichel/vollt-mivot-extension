package main.annoter.pyvocode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import main.annoter.UtypeDecoder;
import tap.metadata.TAPColumn;

public class MappingCache {
	Map<String, Map<String, UtypeDecoder>> cache;
	
	public MappingCache() {
		this.cache = new LinkedHashMap<String, Map<String, UtypeDecoder>>();
	}
	public void addTAPColumn(TAPColumn tapColumn) {
		String tableName = tapColumn.getTable().getADQLName();
		if( this.cache.containsKey(tableName) == false ) {
			this.cache.put(tableName, new LinkedHashMap<String, UtypeDecoder>());
		}
		UtypeDecoder utypeDecoder = new UtypeDecoder(tapColumn);
		this.cache.get(tableName).put(tapColumn.getADQLName(), utypeDecoder);
	}
	
	public Map<String, UtypeDecoder> getTableMapping(String adqlTableName){
		return this.cache.get(adqlTableName);
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
}
