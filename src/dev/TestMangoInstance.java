package dev;

import java.io.IOException;

import main.annoter.dm.MangoInstance;
import main.annoter.mivot.MappingError;

public class TestMangoInstance {

	public static void main(String[] args) throws IOException, MappingError {
		MangoInstance mi = new MangoInstance("dmtype");
		System.out.println(mi.xmlString());
	}

}
