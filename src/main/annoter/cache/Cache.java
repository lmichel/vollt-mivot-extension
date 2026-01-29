package main.annoter.cache;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import main.annoter.mivot.FrameHolder;
import tap.log.TAPLog;
import uws.service.log.UWSLog.LogLevel;

/**
 * Small application-level cache used by the mapping/annotation pipeline.
 *
 * Responsibilities:
 * - Provide a lookup cache for property implementation classes so reflection
 *   is only done once per property name (see {@link #getPropertyClass}).
 * - Cache constructed {@link FrameHolder} objects so identical frames are
 *   reused across mapping runs and avoid duplicate XML emission.
 * - Provide a simple logging facade that delegates to an optional TAPLog
 *   instance or falls back to System.out when no logger is configured.
 *
 * Thread-safety / lifetime:
 * - The class uses synchronized methods for mutation of the property and
 *   frame caches where needed, but the logger may be set once via
 *   {@link #setLogger}. The cache maps use LinkedHashMap to preserve
 *   deterministic insertion order when iterating (useful for debugging).
 *
 * Note: This is an in-memory, process-local cache intended for short-lived
 * annotation processes; it does not implement eviction and is not suitable
 * for unbounded growth in long-running servers without additional controls.
 */
public class Cache {
    // Map property short-name -> loaded Class object (main.annoter.dm.<Name>)
    static private Map<String, Class<?> > propertyInstanceCache = new LinkedHashMap<String, Class<?> >();
    // Map frameId -> FrameHolder used to share created frames across requests
    static private Map<String, FrameHolder> frameCache = new LinkedHashMap<String, FrameHolder>();
    // Optional external logger (can be set once by the embedding application)
    public static TAPLog logger = null;
    
    /**
     * Configure the TAPLog logger used by the Cache. The method is safe to
     * call multiple times but only the first non-null logger will be kept.
     *
     * @param tapLog external TAPLog instance provided by the hosting app
     */
    public static void setLogger(TAPLog tapLog) {
        if( Cache.logger == null ) {
            Cache.logger = tapLog;
        }
    }
    
    /**
     * Log a debug-level message. Accepts varargs strings which are joined
     * with spaces into the final message. If no TAPLog is configured the
     * message is printed to System.out with a DEBUG prefix.
     */
    public static void logDebug(String... args) {
        List<String> list = Arrays.asList(args);
        String message = String.join(" ", list);
        if( Cache.logger != null ) {
            Cache.logger.log(LogLevel.DEBUG, "MIVOT", message, null);
        } else {
            System.out.println("DEBUG: " + message);
        }
    }

    /**
     * Log an info-level message; falls back to System.out when no TAPLog set.
     */
    public static void logInfo(String... args) {
        List<String> list = Arrays.asList(args);
        String message = String.join(" ", list);
        if( Cache.logger != null ) {
            Cache.logger.log(LogLevel.INFO, "MIVOT", message, null);
        } else {
            System.out.println("INFO: " + message);
        }
    }

    /**
     * Log a warning-level message; falls back to System.out when no TAPLog set.
     */
    public static void logWarning(String... args) {
        List<String> list = Arrays.asList(args);
        String message = String.join(" ", list);
        if( Cache.logger != null ) {
            Cache.logger.log(LogLevel.WARNING, "MIVOT", message, null);
        } else {
            System.out.println("WARNING: " + message);
        }
    }

    /**
     * Log an error-level message; falls back to System.out when no TAPLog set.
     */
    public static void logError(String... args) {
        List<String> list = Arrays.asList(args);
        String message = String.join(" ", list);
        if( Cache.logger != null ) {
            Cache.logger.log(LogLevel.ERROR, "MIVOT", message, null);
        } else {
            System.out.println("ERROR: " + message);
        }
    }
    
    /**
     * Load and cache a property implementation Class by its short name.
     *
     * The cache stores Class objects keyed by the simple property class name
     * (for example "Brightness"). When the class is not yet cached it is
     * loaded via Class.forName("main.annoter.dm." + propertyClassName).
     *
     * This method is synchronized to avoid racing two threads attempting to
     * load the same class simultaneously.
     *
     * @param propertyClassName simple class name (without package)
     * @return the loaded Class object
     * @throws ClassNotFoundException if the class cannot be found
     */
    public static synchronized Class<?> getPropertyClass(String propertyClassName) throws ClassNotFoundException{
        if( Cache.propertyInstanceCache.containsKey(propertyClassName) == false ) {
            Cache.propertyInstanceCache.put(
                    propertyClassName,
                    Class.forName("main.annoter.dm." + propertyClassName));
        }
        return  Cache.propertyInstanceCache.get(propertyClassName);
    }
    
    /**
     * Retrieve a cached FrameHolder by id, or null when not present.
     *
     * This accessor is not synchronized because reads of LinkedHashMap are
     * safe for single operations; however, callers should assume the value may
     * be concurrently inserted by another thread.
     *
     * @param frameId identifier of the frame (dmid)
     * @return cached FrameHolder or null
     */
    public static FrameHolder getFrameHolder(String frameId) {
        if( Cache.frameCache.containsKey(frameId) ) {
            return Cache.frameCache.get(frameId);
        }
        return null;

    }

    /**
     * Store a FrameHolder in the cache if absent and return the canonical
     * cached instance. The method is synchronized to avoid duplicate inserts
     * when multiple threads attempt to store the same id.
     *
     * @param frameHolder instance to store
     * @return the stored (canonical) FrameHolder
     */
    public static  synchronized FrameHolder storeFrameHolder(FrameHolder frameHolder) {
        String frameId = frameHolder.frameId;
        if( Cache.frameCache.containsKey(frameId) == false ) {
            Cache.frameCache.put(frameId, frameHolder);
        }
        return Cache.frameCache.get(frameId);
    }
}