package main.annoter.cache;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import main.annoter.mivot.FrameHolder;

public class Cache {
	static private Map<String, Class<?> > propertyInstanceCache = new LinkedHashMap<String, Class<?> >();
	static private Map<String, FrameHolder> frameCache = new LinkedHashMap<String, FrameHolder>();
	static private Map<String, String> referencedModels = new LinkedHashMap<String, String>();
	/** Collected frame IDs already created (prevents duplicates). */
	static private Set<String> globalsIds = new HashSet<String>();

	
	public static void resetSession() {
		Cache.referencedModels = new LinkedHashMap<String, String>();
		Cache.globalsIds = new HashSet<String>();
	}
	
	public static Class<?> getPropertyClass(String propertyClassName) throws ClassNotFoundException{
		
		if( Cache.propertyInstanceCache.containsKey(propertyClassName) == false ) {
			Cache.propertyInstanceCache.put(
					propertyClassName,
					Class.forName("main.annoter.dm." + propertyClassName));
		}
		return  Cache.propertyInstanceCache.get(propertyClassName);
	}
	
	public static String getReferencedModel(String referencedModel) throws ClassNotFoundException {
		
		if( Cache.referencedModels.containsKey(referencedModel) ) {
			return Cache.referencedModels.get(referencedModel);
		}
		return null;
	}
	
	public static String storeReferencedModel(String modelPrefix, String modelUrl) {
 		if( Cache.referencedModels.containsKey(modelPrefix) == false ) {
 			Cache.referencedModels.put(modelPrefix, modelUrl);
 		}
 		return Cache.referencedModels.get(modelPrefix);
	}
	
	public static Set<String> getReferencedModelList() {
		return Cache.referencedModels.keySet();
	}
	public static FrameHolder getFrameHolder(String frameId) {
 		if( Cache.frameCache.containsKey(frameId) ) {
 			return Cache.frameCache.get(frameId);
 		}
 		return null;

	}
	public static FrameHolder storeFrameHolder(FrameHolder frameHolder) {
		String frameId = frameHolder.frameId;
 		if( Cache.frameCache.containsKey(frameId) == false ) {
 			Cache.frameCache.put(frameId, frameHolder);
 		}
 		return Cache.frameCache.get(frameId);
	}
	
	public static boolean containsGlobalsId(String globalsId) {
		return Cache.globalsIds.contains(globalsId);
	}
	public static void storeGlobalsId(String globalsId) {
		Cache.globalsIds.add(globalsId);
	}
}
