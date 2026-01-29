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
 * In-memory cache of UType -> column decoders for tables discovered during the
 * webapp (Tomcat) lifetime.
 *
 * Responsibilities:
 * - Collect UType mappings from TAP/ADQL table metadata (TAPTable / ADQLTable).
 * - Provide query helpers that return the available UType decoders for a
 *   given table, optionally filtered by hostClass, hostAttribute or a list of
 *   selected columns.
 * - Expose helpers to find which ADQL column is mapped to a given UType.
 *
 * Implementation notes:
 * - The cache is a simple singleton (see {@link #getCache()}) and is not
 *   synchronized. It is intended to be used in a single-threaded request
 *   mapping context within Tomcat. If you plan to access it concurrently
 *   from multiple threads, add appropriate synchronization.
 * - The primary map structure is: Map<tableName, Map<columnName, UtypeDecoder>>
 * - Methods that accept a list of selected columns use that list to filter
 *   which mappings are returned; this is useful when only a subset of table
 *   columns are available in a specific request.
 */
public class MappingCache {
	private Map<String, Map<String, UtypeDecoder>> utypeMap;
    private static MappingCache CACHE;
    private List<String> storedTables = new ArrayList<String>();
    
    /**
     * Private constructor for singleton.
     */
	private MappingCache() {
		this.utypeMap = new LinkedHashMap<String, Map<String, UtypeDecoder>>();
	}
	
	/**
	 * Return the shared MappingCache singleton (lazily created).
	 *
	 * @return the global MappingCache instance
	 */
	public static MappingCache getCache(){
		if( CACHE == null ) {
			CACHE = new MappingCache();
		}
		return CACHE;
	}
	
	/**
	 * Add all UType-bearing columns from a TAPTable into the cache.
	 *
	 * Columns without a UType or whose UType does not begin with "mango:"
	 * are ignored. The method guards against processing the same table more
	 * than once by recording table names in {@link #storedTables}.
	 *
	 * @param tapTable TAPTable metadata to ingest
	 */
	public void addTAPTable(TAPTable tapTable) {
		
		String tableName = tapTable.getADQLName();
		if( this.storedTables.contains(tableName)) {
			// Table was already processed; skip re-adding
			return;
		}
		this.storedTables.add(tableName);

		Iterator<TAPColumn> it = tapTable.getColumns();
		while (it.hasNext()) {
			TAPColumn tapColumn = (TAPColumn) it.next();
			String uType = tapColumn.getUtype();
			// Only consider mango: UTypes (project convention)
			if (uType != null && uType.startsWith("mango:") ){
				this.addTAPColumn(tapColumn);
			}
		}
	}
	
	/**
	 * Add all UType-bearing columns from an ADQLTable into the cache.
	 *
	 * This mirrors addTAPTable but works with the ADQLTable/DBColumn API.
	 * Only columns that are instances of TAPColumn and whose UType starts with
	 * "mango:" are added.
	 *
	 * @param tapTable ADQLTable to ingest
	 */
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
	
	/**
	 * Add a single TAPColumn's UType decoder to the cache.
	 *
	 * If the table entry does not yet exist in the map it is created.
	 *
	 * @param tapColumn TAPColumn whose UType should be decoded and stored
	 */
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
	
	/**
	 * Return the raw map of columnName -> UtypeDecoder for a table.
	 *
	 * @param adqlTableName table name
	 * @return map of column name to UtypeDecoder, or null if the table is unknown
	 */
	public Map<String, UtypeDecoder> getTableMapping(String adqlTableName){
		return this.utypeMap.get(adqlTableName);
	}
	
	/**
	 * Return all decoders for a table that match a given hostClass.
	 *
	 * @param adqlTableName table name
	 * @param hostClass host class name to filter by (e.g. "mango:EpochPosition")
	 * @return list of UtypeDecoder matching the hostClass (empty when none)
	 */
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
	
	/**
	 * Return all decoders for a table that match both hostClass and hostAttribute.
	 *
	 * @param adqlTableName table name
	 * @param hostClass host class name
	 * @param hostAttribute host attribute name
	 * @return list of matching UtypeDecoder (empty when none)
	 */
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
	
	/**
	 * Return a mapping grouped by the bracketed constant/frames key for all
	 * decoders in the provided table that match the given hostClass and are
	 * present in the selectedColumns list.
	 *
	 * The returned structure is: Map<constantAndFramesKey, List<UtypeDecoder>>.
	 * If a decoder doesn't have a constantAndFrames key it is grouped under
	 * the literal key "default".
	 *
	 * @param adqlTableName table name
	 * @param hostClass host class to filter by
	 * @param selectedColumns list of ADQL column names to consider (filters results)
	 * @return grouped mapping keyed by constant/frames descriptor
	 */
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
				// Only include decoders whose ADQL column name is present in selectedColumns
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
	
	/**
	 * Return the ADQL column name that maps to the requested UType for the
	 * given table, or null when no match is found.
	 *
	 * @param adqlTableName table name
	 * @param utype fully qualified utype string to look for
	 * @return the ADQL column name mapped to the utype, or null
	 */
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
	
	/**
	 * Like {@link #getUtypeMappedColumn(String, String)} but restricts the
	 * result to the provided selectedColumns list. If the mapped column is not
	 * present in selectedColumns null is returned.
	 *
	 * @param adqlTableName table name
	 * @param utype fully qualified utype to look for
	 * @param selectedColumns list of ADQL columns considered available
	 * @return ADQL column name if present in selectedColumns; otherwise null
	 */
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