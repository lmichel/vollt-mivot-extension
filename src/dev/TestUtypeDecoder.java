package dev;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import main.annoter.UtypeDecoder;
import tap.metadata.TAPColumn;

public class TestUtypeDecoder {
	
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
		
		for( TAPColumn tapColumn: tapColumns) {
			UtypeDecoder utd = new UtypeDecoder(tapColumn);
			System.out.println(utd);
		}
	}



}
