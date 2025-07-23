package dev;

import main.annoter.pyvocode.MangoInstance;
import main.annoter.pyvocode.MivotAnnotations;
import main.annoter.pyvocode.MivotInstance;

public class TestMivotAnnotations {

	public static void main(String[] args) throws Exception {
		MangoInstance mi = new MangoInstance("dmtype");
		System.out.println(mi.xmlString(false));
		
		MivotAnnotations mivotAnnotation = new MivotAnnotations();
		mivotAnnotation.addTemplates(mi);
		mivotAnnotation.buildMivotBlock("", false);
		System.out.println(mivotAnnotation.mivotBlock);
	}

}
