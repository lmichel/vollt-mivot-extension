package dev;

import main.annoter.cache.SessionCache;
import main.annoter.mivot.FrameFactory;
import main.annoter.mivot.FrameHolder;
import main.annoter.mivot.MappingError;

public class TestFrameFactory {

	public static void main(String[] args) {
		FrameFactory ff = FrameFactory.getInstance(new SessionCache());
		
		try {
			FrameHolder fh = ff.createFrame("photFilterHigh=u");
			System.out.println("Frame ID: " + fh);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.exit(1);
		
		try {
			FrameHolder fh = ff.createFrame("local=SUM_FLAG");
			System.out.println("Frame ID: " + fh);
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
