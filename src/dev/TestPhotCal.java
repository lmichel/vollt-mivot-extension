package dev;



import main.annoter.dm.PhotCal;

public class TestPhotCal {

	public static void main(String[] args) throws Exception {

		//System.out.println(PhotCal.addPhotCal("G"));
		System.out.println(PhotCal.getMivotPhotFilter("SLOAN/SDSS.u/Vega"));
		System.out.println(PhotCal.getMivotPhotCal("SLOAN/SDSS.u/Vega"));
		System.out.println("--------------");
		
		System.out.println(PhotCal.getMivotPhotCal("uAA"));

		//System.out.println(PhotCal.addPhotCal("K"));
		
		//String xmlString = PhotCal.addPhotCal("u");
		
		//System.out.println(xmlString);

    }

	
}
