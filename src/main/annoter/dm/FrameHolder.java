package main.annoter.dm;

import main.annoter.mivot.MappingError;
import main.annoter.mivot.MivotInstance;

public class FrameHolder {
	
	public String frameId;
	public String frameXml;
	public String systemClass;

	public FrameHolder(String systemClass,  String frameId) {
		this.frameId = frameId;
		this.systemClass = systemClass;
	}
	
	@Override
	public String toString() {
		return this.systemClass + "/" + frameId + "\n" + ((frameXml == null)? "null": frameXml);
	}
	
	
	public void setFrame(Object instance) throws MappingError {
		if( instance instanceof MivotInstance) {
			this.frameXml = ((MivotInstance) instance).xmlString();
		} else if( instance instanceof String) {
			this.frameXml = (String) instance;
		} else {
			throw new MappingError("FrameHolder: Unsupported frame instance type: " + instance.getClass().getName());
		}
	}

}
