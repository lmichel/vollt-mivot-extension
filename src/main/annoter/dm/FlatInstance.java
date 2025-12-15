package main.annoter.dm;

import java.util.List;

import main.annoter.meta.UtypeDecoder;
import main.annoter.mivot.MivotInstance;

public class FlatInstance {
	protected String dmrole;
	protected List<UtypeDecoder> utypeDecoders;
	protected String dmid;
	
	public FlatInstance(String dmrole, List<UtypeDecoder> utypeDecoders, String dmid) {
		this.dmrole = dmrole;
		this.utypeDecoders = utypeDecoders;
		this.dmid = dmid;
	}
	
	public  MivotInstance getMivotInstance() throws Exception {
		
		MivotInstance flatInstance = new MivotInstance(this.utypeDecoders.get(0).getInnerClass(), this.dmrole, this.dmid);
		for (UtypeDecoder mappableColumn : this.utypeDecoders) {
			flatInstance.addAttribute(
				"ivoa:RealQuantity",
				this.utypeDecoders.get(0).getInnerClass() + "." + mappableColumn.getInnerAttribute(),
				mappableColumn.getTapColumn().getADQLName(),
				mappableColumn.getTapColumn().getUnit()
			);
		}
		return flatInstance;
	}

}
