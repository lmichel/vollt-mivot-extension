package dev;

import main.annoter.meta.MappingCache;
import tap.metadata.TAPColumn;
import tap.metadata.TAPTable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestMappingCache {

	public static void main(String[] args) throws IOException {

		List<TAPColumn> tapColumns = new ArrayList<TAPColumn>();

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
		
		
		MappingCache mappingCache = MappingCache.getCache();
        final TAPTable table = new TAPTable("ma_table", TAPTable.TableType.table);
        for( TAPColumn tapColumn: tapColumns) {
			table.addColumn(tapColumn);
			mappingCache.addTAPColumn(tapColumn);
		}
		System.out.println(mappingCache.getTableMapping("ma_table", "mango:EpochPosition"));
	}
}
