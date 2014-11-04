package org.geppetto.model.neuroml.utils.modeltree;

import org.neuroml.model.Base;


public class PopulateGeneralModelTreeUtils {
	
	public static String getUniqueName(String label, Object base){
		String id = "";
		if (base instanceof Base){
			id = ((Base)base).getId();
		}
		else if (base instanceof Integer) {
			id = String.valueOf((Integer)base);
		}
		else{
			id = ((String)base);
		}	
		return label + " - " + id;
	}
	
	/*
    For example, ../Pop0[0] returns 0; ../Gran/0/Granule_98 returns 0; Gran/1/Granule_98 returns 1
*/
	public static String parseCellRefStringForCellNum(String cellRef) {
//	   System.out.println("Parsing for cell num: "+cellRef);
	   if (cellRef.indexOf("[")>=0) {
	       return cellRef.substring(cellRef.indexOf("[")+1, cellRef.indexOf("]"));
	   } else {
	       int loc = cellRef.startsWith("../") ? 2 : 1;
	       String ref = cellRef.split("/")[loc];
	       return ref;
	   }
	}
	

}
