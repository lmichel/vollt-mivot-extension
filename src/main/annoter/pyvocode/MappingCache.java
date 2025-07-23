package main.annoter.pyvocode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import main.annoter.UtypeDecoder;
import tap.metadata.TAPColumn;

public class MappingCache {
	Map<String, Map<String, UtypeDecoder>> cache;
	
	public MappingCache() {
		this.cache = new HashMap<String, Map<String, UtypeDecoder>>();
	}
	public void addTAPColumn(TAPColumn tapColumn) {
		String tableName = tapColumn.getTable().getADQLName();
		if( this.cache.containsKey(tableName) == false ) {
			this.cache.put(tableName, new HashMap<String, UtypeDecoder>());
		}
		this.cache.get(tableName).put(tapColumn.getADQLName(), new UtypeDecoder(tapColumn));
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
