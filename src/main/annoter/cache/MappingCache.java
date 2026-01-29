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
 * In-memory mapping registry that collects UType -> column decoders for tables
 * discovered during the lifetime of the webapp (Tomcat).
 *
 * Responsibilities:
 * - Ingest TAP or ADQL table metadata and create UtypeDecoder entries for any
 *   columns that declare a "mango:" utype.
 * - Provide lookup helpers to retrieve decoders for a given table, optionally
 *   filtered by hostClass, hostAttribute or a set of selected columns.
 * - Expose helper methods to obtain the ADQL column name that maps to a
 *   particular UType.
 *
 * Implementation notes:
 * - The cache is a simple singleton (see {@link #getCache()}). It is not
 *   synchronized and intended to be used within a single-threaded request
 *   mapping context. Add synchronization if sharing across threads concurrently.
 * - Primary data structure: Map<tableName, Map<columnName, UtypeDecoder>>
 */
public class MappingCache {
	// Map of ADQL table name -> (ADQL column name -> UtypeDecoder)
	private Map<String, Map<String, UtypeDecoder>> utypeMap;
	private static MappingCache CACHE;
	// Keep track of tables already processed to avoid duplicate ingestion
	private List<String> storedTables = new ArrayList<String>();
	
	private MappingCache() {
		this.utypeMap = new LinkedHashMap<String, Map<String, UtypeDecoder>>();
	}
	
	/**
	 * Return the shared MappingCache singleton (lazy init).
	 *
	 * @return global MappingCache instance
	 */
	public static MappingCache getCache(){
		if( CACHE == null ) {
			CACHE = new MappingCache();
		}
		return CACHE;
	}
	
	/**
	 * Ingest all UType-bearing columns from a TAPTable into the cache.
	 *
	 * Only TAPColumns whose utype starts with "mango:" are considered. The
	 * method is idempotent for a given table name (subsequent calls for the
	 * same table have no effect).
	 *
	 * @param tapTable metadata describing the TAP table to ingest
	 */
	public void addTAPTable(TAPTable tapTable) {
		String tableName = tapTable.getADQLName();
		// Skip tables already processed
		if( this.storedTables.contains(tableName)) {
			return;
		}
		this.storedTables.add(tableName);

		Iterator<TAPColumn> it = tapTable.getColumns();
		while (it.hasNext()) {
			TAPColumn tapColumn = (TAPColumn) it.next();
			String uType = tapColumn.getUtype();
			// Only consider project-specific mango: utypes
			if (uType != null && uType.startsWith("mango:") ){
				this.addTAPColumn(tapColumn);
			}
		}
	}
	
	/**
	 * Ingest UType-bearing columns from an ADQLTable (ADQL API objects).
	 *
	 * Mirrors {@link #addTAPTable(TAPTable)} but works with ADQLTable/DBColumn
	 * structures. Only DBColumns that are instances of TAPColumn are processed.
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
	 * Add a single TAPColumn's decoded UType to the cache.
	 *
	 * If the table entry does not exist it is created. The stored UtypeDecoder
	 * is keyed by the ADQL column name (tapColumn.getADQLName()).
	 *
	 * @param tapColumn TAP column to decode and store
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
	 * Return the raw mapping (columnName -> UtypeDecoder) for a table.
	 *
	 * @param adqlTableName table name
	 * @return map of columnName -> UtypeDecoder or null if the table is unknown
	 */
	public Map<String, UtypeDecoder> getTableMapping(String adqlTableName){
		return this.utypeMap.get(adqlTableName);
	}
	
	/**
	 * Return all decoders for a table that match a given hostClass.
	 *
	 * @param adqlTableName table name
	 * @param hostClass host class name to filter by (e.g. "mango:EpochPosition")
	 * @return list of matching UtypeDecoder (empty list when none)
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
	 * Build a grouped mapping for the given table and hostClass restricted to
	 * the provided selectedColumns list.
	 *
	 * The returned map groups decoders by their bracketed constant/frames
	 * descriptor (utypeDecoder.getConstantAndFrames()). Decoders without such
	 * a descriptor are grouped under the key "default".
	 *
	 * @param adqlTableName table name
	 * @param hostClass host class to filter by
	 * @param selectedColumns list of ADQL column names to include (filters results)
	 * @return LinkedHashMap keyed by constant/frames descriptor with lists of decoders
	 */
	public Map<String, List<UtypeDecoder>> getTableMapping(String adqlTableName, String hostClass, List<String> selectedColumns){
		Map<String, List<UtypeDecoder>> tableMapping = new LinkedHashMap<String, List<UtypeDecoder>>();
		if( this.getTableMapping(adqlTableName) == null ) {
			return tableMapping;
		}
		for( UtypeDecoder utypeDecoder: this.getTableMapping(adqlTableName).values()) {
			if( utypeDecoder.getHostClass().equals(hostClass)) {
				// Only include decoders whose ADQL column name is present in selectedColumns
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
	/**
	 * Populate a small fake mapping used by unit/test or interactive checks.
	 *
	 * The method creates a TAPTable named "basic" and adds several TAPColumn
	 * instances with realistic utypes so mapping logic can be exercised without
	 * connecting to a real TAP service.
	 *
	 * This helper is intended for tests and local development only.
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
	
	/**
	 * Populate a fake mapping for flux/brightness columns (used in tests).
	 */
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