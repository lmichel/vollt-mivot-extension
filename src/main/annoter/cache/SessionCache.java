package main.annoter.cache;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import main.annoter.mivot.FrameHolder;
import tap.log.TAPLog;

public class SessionCache {
	private Map<String, String> referencedModels = new LinkedHashMap<String, String>();
	/** Collected frame IDs already created in the current query processing (prevents duplicates). */
	private Set<String> globalsIds = new HashSet<String>();
	
	public String getReferencedModel(String referencedModel) throws ClassNotFoundException {
		
		if( this.referencedModels.containsKey(referencedModel) ) {
			return this.referencedModels.get(referencedModel);
		}
		return null;
	}
	
	public String storeReferencedModel(String modelPrefix, String modelUrl) {
 		if( this.referencedModels.containsKey(modelPrefix) == false ) {
 			this.referencedModels.put(modelPrefix, modelUrl);
 		}
 		return this.referencedModels.get(modelPrefix);
	}
	
	public Set<String> getReferencedModelList() {
		return this.referencedModels.keySet();
	}
	public boolean containsGlobalsId(String globalsId) {
		return this.globalsIds.contains(globalsId);
	}
	public void storeGlobalsId(String globalsId) {
		this.globalsIds.add(globalsId);
	}
}
