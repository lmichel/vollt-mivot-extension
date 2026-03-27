package dev;

import java.util.Map;

import main.annoter.cache.MappingCache;
import main.annoter.meta.UtypeDecoder;

public class BuildSqlUtypeSetters {


	public static void main(String[] args) {
		MappingCache MAPPING_CACHE = MappingCache.getCache();

		MAPPING_CACHE.getFakeMappingCacheForBasic();
		MAPPING_CACHE.getFakeMappingCacheForFlux();
		String[] tables = {"basic", "allfluxes"};
		for( String table: tables) {

			Map<String, UtypeDecoder> map = MAPPING_CACHE.getTableMapping(table);
			for( String column: map.keySet()) {
				String sql = "UPDATE \"TAP_SCHEMA\".columns " + 
						"SET utype='" + map.get(column).getUtype()+map.get(column).getConstantAndFrames() + "' "
						+ "WHERE table_name = '" + table + "' AND column_name = '" + column+ "'";
				System.out.println(sql);
			}
		}

	}
}
