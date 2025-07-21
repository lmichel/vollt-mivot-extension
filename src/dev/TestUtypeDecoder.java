package dev;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import main.annoter.UtypeDecoder;
import tap.metadata.TAPColumn;

public class TestUtypeDecoder {
	
	public static void main(String[] args) throws IOException {

		List<TAPColumn> tapColumns = new ArrayList();

		tapColumns.add(new TAPColumn("columnName",
				"description", "unit", "ucd",
				"#2-mango:EpochPosition:/mango:EpochPositionErrors.position/mango:error.PErrorSym2D.sigma1"));
		tapColumns.add(new TAPColumn("longitude",
				"description", "unit", "ucd",
				"mango:EpochPosition.longitude[CS.spaceSys=ICRS]"));

		tapColumns.add(new TAPColumn("latitude",
				"description", "unit", "ucd",
				"mango:EpochPosition.latitude[CS.spaceSys=ICRS CS.timeSys=TCB]"));

		tapColumns.add(new TAPColumn("MJD_FIRST",
				"description", "year", "ucd",
				"mango:EpochPosition:/mango:EpochPosition.obsDate/mango:DateTime.dateTime[CT.representation=year]"));
		
		for( TAPColumn tapColumn: tapColumns) {
			UtypeDecoder utd = new UtypeDecoder(tapColumn.getUtype());
			System.out.println(utd);
		}
	}



}
