package model.tests;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import annoter.JsonProfile;
import annoter.SimpleAnnotationBuilder;
import tap.metadata.TAPColumn;

/**
 * @author ierrami
 * this class checks the connection to the database
 */
public class SimpleAnnotationTapSchemaTest {

	public static void main(String[] args) throws IOException {
		
		List<TAPColumn> tapColumns = new ArrayList();
		tapColumns.add(new TAPColumn("columnName",
				"description", "unit", "ucd",
				"#2-mango:EpochPosition.errors.position/mango:error.PErrorSym2D.sigma1"));
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
		tapColumns.add(new TAPColumn("MJD_FIRST",
				"description", "year", "ucd",
				"mango:EpochPosition.obsDate/mango:DateTime.dateTime[CT.representation=year]"));

		try {
			
			JsonProfile jsonProfile = new JsonProfile("simbad", tapColumns);
			
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		SimpleAnnotationBuilder sab=null;
		try {
			sab = new SimpleAnnotationBuilder("xtapdb", tapColumns, null);
			sab.buildMivotBlock();
			
		} catch(Exception e) {
			e.printStackTrace();
			sab.setAnnotationsAsFailed(e.toString());
		}
		sab.writeAnnotations();


	}

}
