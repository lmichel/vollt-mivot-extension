package dev;



<<<<<<< HEAD
import main.annoter.mivot.PhotCalFactory;
=======
import main.annoter.dm.PhotCalFactory;
>>>>>>> branch 'main' of git@github.com:lmichel/vollt-mivot-extension.git

public class TestPhotCal {

	public static void main(String[] args) throws Exception {

		//System.out.println(PhotCal.addPhotCal("G"));
		PhotCalFactory fcf = new PhotCalFactory();
		System.out.println(fcf.getMivotPhotFilter("SLOAN/SDSS.u/Vega"));
		System.out.println(fcf.getMivotPhotCal("SLOAN/SDSS.u/Vega", "calid", "filterid"));
		System.out.println("--------------");
    }

	
}
