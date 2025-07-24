package dev;

import java.io.IOException;

import main.annoter.pyvocode.MangoInstance;
import main.annoter.pyvocode.MappingError;

public class TestMangoInstance {

	public static void main(String[] args) throws IOException, MappingError {
		MangoInstance mi = new MangoInstance("dmtype");
		System.out.println(mi.xmlString());
	}

}
