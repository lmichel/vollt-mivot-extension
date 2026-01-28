package dev;



import main.annoter.mivot.PhotCalFactory;

public class TestPhotCal {

	public static void main(String[] args) throws Exception {

		//System.out.println(PhotCal.addPhotCal("G"));
		PhotCalFactory fcf = new PhotCalFactory();
		System.out.println(fcf.getMivotPhotFilter("SLOAN/SDSS.u/Vega"));
		System.out.println(fcf.getMivotPhotCal("SLOAN/SDSS.u/Vega", "calid", "filterid"));
		System.out.println("--------------");
		System.out.println(fcf.getMivotPhotCal("V", "calid", "filterid"));
		System.out.println("--------------");
    }

	
}
