package main.annoter.cache;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import main.annoter.mivot.FrameHolder;
import tap.log.TAPLog;
import uws.service.log.UWSLog.LogLevel;

public class Cache {
	static private Map<String, Class<?> > propertyInstanceCache = new LinkedHashMap<String, Class<?> >();
	static private Map<String, FrameHolder> frameCache = new LinkedHashMap<String, FrameHolder>();
    public static TAPLog logger = null;
	
    public static void setLogger(TAPLog tapLog) {
    	if( tapLog == null ) {
    		Cache.logger = tapLog;
    	}
    }
    
    public static void logDebug(String... args) {
    	List<String> list = Arrays.asList(args);
    	String message = String.join(" ", list);
    	if( Cache.logger != null ) {
    		Cache.logger.log(LogLevel.DEBUG, "MIVOT", message, null);
    	} else {
    		System.out.println("DEBUG: " + message);
    	}
    }
    public static void logInfo(String[] args) {
    	List<String> list = Arrays.asList(args);
    	String message = String.join(" ", list);
    	if( Cache.logger != null ) {
    		Cache.logger.log(LogLevel.INFO, "MIVOT", message, null);
    	} else {
    		System.out.println("INFO: " + message);
    	}
    }
    public static void logWarning(String[] args) {
    	List<String> list = Arrays.asList(args);
    	String message = String.join(" ", list);
    	if( Cache.logger != null ) {
    		Cache.logger.log(LogLevel.WARNING, "MIVOT", message, null);
    	} else {
    		System.out.println("WARNING: " + message);
    	}
    }
    public static void logError(String[] args) {
    	List<String> list = Arrays.asList(args);
    	String message = String.join(" ", list);
    	if( Cache.logger != null ) {
    		Cache.logger.log(LogLevel.ERROR, "MIVOT", message, null);
    	} else {
    		System.out.println("ERROR: " + message);
    	}
    }
	
	public static synchronized Class<?> getPropertyClass(String propertyClassName) throws ClassNotFoundException{
		
		if( Cache.propertyInstanceCache.containsKey(propertyClassName) == false ) {
			Cache.propertyInstanceCache.put(
					propertyClassName,
					Class.forName("main.annoter.dm." + propertyClassName));
		}
		return  Cache.propertyInstanceCache.get(propertyClassName);
	}
	
	public static FrameHolder getFrameHolder(String frameId) {
 		if( Cache.frameCache.containsKey(frameId) ) {
 			return Cache.frameCache.get(frameId);
 		}
 		return null;

	}
	public static  synchronized FrameHolder storeFrameHolder(FrameHolder frameHolder) {
		String frameId = frameHolder.frameId;
 		if( Cache.frameCache.containsKey(frameId) == false ) {
 			Cache.frameCache.put(frameId, frameHolder);
 		}
 		return Cache.frameCache.get(frameId);
	}
}
