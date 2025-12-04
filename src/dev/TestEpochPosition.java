package dev;

import main.annoter.dm.EpochPosition;
import main.annoter.meta.MappingCache;
import main.annoter.mivot.MappingError;
import tap.metadata.TAPColumn;
import tap.metadata.TAPTable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestEpochPosition {

	public static void main(String[] args) throws IOException, MappingError {

		List<TAPColumn> tapColumns = new ArrayList<TAPColumn>();

		tapColumns.add(new TAPColumn("columnName_1",
				"description", "unit", "ucd",
				"mango:EpochPosition.errors"));
		
		tapColumns.add(new TAPColumn("columnName_21",
				"description", "unit", "ucd",
				"mango:EpochPosition.errors.position/mango:error.PErrorEllipse.majorAxis"));
		tapColumns.add(new TAPColumn("columnName_22",
				"description", "unit", "ucd",
				"mango:EpochPosition.errors.position/mango:error.PErrorEllipse.minorAxis"));
		tapColumns.add(new TAPColumn("columnName_23",
				"description", "unit", "ucd",
				"mango:EpochPosition.errors.position/mango:error.PErrorEllipse.angle"));
		
		tapColumns.add(new TAPColumn("columnName_3",
				"description", "unit", "ucd",
				"mango:EpochPosition.errors.position/mango:error.PErrorSym2D.sigma1[CS.spaceSys=ICRS]"));
		
		tapColumns.add(new TAPColumn("columnName_4",
				"description", "deg", "ucd",
				"mango:EpochPosition.latitude[CS.spaceSys=ICRS]"));
		
		tapColumns.add(new TAPColumn("columnName_5",
				"description", "deg", "ucd",
				"mango:EpochPosition.longitude[CS.spaceSys=ICRS]"));
		
		MappingCache mappingCache = MappingCache.getCache();
        final TAPTable table = new TAPTable("ma_table", TAPTable.TableType.table);
		for( TAPColumn tapColumn: tapColumns) {
			table.addColumn(tapColumn);
			mappingCache.addTAPColumn(tapColumn);
		}
		
		EpochPosition epochPosition = new EpochPosition("ma_table",
				Arrays.asList("columnName_21", "columnName_22", "columnName_23", "columnName_5", "columnName_4"));
		System.out.println(epochPosition.xmlString());
	}
}
