package dev;

import java.io.IOException;

import main.annoter.pyvocode.MappingError;
import main.annoter.pyvocode.MivotInstance;

public class TestMivotInstance {

	public static void main(String[] args) throws IOException, MappingError {
		MivotInstance mi = new MivotInstance("dmtype");
		mi.addAttribute("att_type1", "att_role1", "att_vamue1", "att_unit1"); 
		mi.addAttribute("att_type2", "att_role2", "*att_vamue2", "att_unit2"); 
		System.out.println(mi.xmlString());
	}

}
