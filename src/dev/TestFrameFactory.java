package dev;

import main.annoter.mivot.FrameFactory;
import main.annoter.mivot.FrameHolder;
import main.annoter.mivot.MappingError;

public class TestFrameFactory {

	public static void main(String[] args) {
		FrameFactory ff = FrameFactory.getInstance();
		
		try {
<<<<<<< HEAD
			FrameHolder fh = ff.createFrame("photFilterHigh=u");
=======
			FrameHolder fh = ff.createFrame("filterLow=r");
			System.out.println("Frame ID: " + fh);
			ff.reset();
			fh = ff.createFrame("filterHigh=u");
>>>>>>> branch 'main' of git@github.com:lmichel/vollt-mivot-extension.git
			System.out.println("Frame ID: " + fh);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(1);
		
		try {
			FrameHolder fh = ff.createFrame("local=SUM_FLAG");
			System.out.println("Frame ID: " + fh);
			ff.reset();
			fh = ff.createFrame("local=SUM_FLAG");
			System.out.println("Frame ID: " + fh);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.exit(1);
		
		try {
			FrameHolder fh = ff.createFrame("space=ICRS(J2000)");
			System.out.println("Frame ID: " + fh);
			fh = ff.createFrame("space=ICRS(J2000)");
			System.out.println("Frame ID: " + fh);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			FrameHolder fh = ff.createFrame("photCal=u");
			System.out.println("Frame ID: " + fh);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
