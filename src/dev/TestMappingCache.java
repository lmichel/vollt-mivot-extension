package dev;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import adql.db.DefaultDBTable;
import main.annoter.UtypeDecoder;
import main.annoter.pyvocode.MappingCache;
import tap.metadata.TAPColumn;

public class TestMappingCache {

	public static void main(String[] args) throws IOException {

		List<TAPColumn> tapColumns = new ArrayList();

		tapColumns.add(new TAPColumn("columnName_1",
				"description", "unit", "ucd",
				"mango:EpochPosition.errors"));
		tapColumns.add(new TAPColumn("columnName_2",
				"description", "unit", "ucd",
				"mango:EpochPosition.errors.position/mango:error.PErrorSym2D.sigma1"));
		
		tapColumns.add(new TAPColumn("columnName_3",
				"description", "unit", "ucd",
				"mango:EpochPosition.errors.position/mango:error.PErrorSym2D.sigma1[CS.spaceSys=ICRS]"));
		
		tapColumns.add(new TAPColumn("columnName_4",
				"description", "unit", "ucd",
				"mango:EpochPosition.longitude[CS.spaceSys=ICRS]"));
		
		
		MappingCache mappingCache = new MappingCache();
		for( TAPColumn tapColumn: tapColumns) {
			tapColumn.setTable(new DefaultDBTable("ma_table", "ma_table"));
			mappingCache.addTAPColumn(tapColumn);
		}
		System.out.println(mappingCache.getTableMapping("ma_table", "mango:EpochPosition"));
	}
}
