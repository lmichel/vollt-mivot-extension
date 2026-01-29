package main.annoter.mivot;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import main.annoter.cache.Cache;
import main.annoter.cache.SessionCache;
import main.annoter.meta.Glossary;

/**
 * Factory responsible for creating FrameHolder objects from UCD/UTD coordinate
 * system descriptor strings.
 *
 * Purpose:
 * - Parse compact frame descriptors (e.g. "space=ICRS(2000)") and produce
 *   fully populated {@link FrameHolder} instances containing the frame XML
 *   (as a string) and model metadata.
 * - Reuse previously generated frames via a shared Cache to avoid duplicate
 *   work. Note: this class itself does not implement a JVM-level singleton;
 *   callers typically obtain an instance via {@link #getInstance(SessionCache)}
 *   to propagate session-scoped model references.
 *
 * Important behavior:
 * - Files for local frames are read from the classpath under
 *   "snippets/mango.frame.<type>.xml".
 * - Generated frames are stored in a shared Cache if their XML is non-null.
 */
public class FrameFactory {
	
	private PhotCalFactory photCalFactory = new PhotCalFactory();
	private SessionCache sessionCache;
	/**
	 * Return an instance of the factory bound to a SessionCache.
	 *
	 * The SessionCache lets the factory record which models a request references
	 * so the calling code can include appropriate MODEL declarations in the
	 * final MIVOT output.
	 *
	 * @param sessionCache session-scoped cache for referenced models and IDs
	 * @return a FrameFactory instance that uses the provided SessionCache
	 */
	public static FrameFactory getInstance(SessionCache sessionCache) {
		return new FrameFactory(sessionCache);
	}
	
	/**
	 * Construct a new FrameFactory bound to the provided SessionCache.
	 *
	 * @param sessionCache session-scoped cache used to record referenced models
	 */
	private FrameFactory(SessionCache sessionCache) {
		this.sessionCache = sessionCache;
	}
	
	/**
	 * Create a FrameHolder from a combined system=frameType string.
	 *
	 * Behavior summary:
	 * - The method splits the input on the first '='; the left part is the
	 *   system class (e.g. "space", "photCal", "local") and the right part
	 *   is the frame descriptor.
	 * - If the frame id has already been recorded in the session cache the
	 *   returned FrameHolder will have a null frameXml (indicating the frame is
	 *   already emitted elsewhere and only a reference is required).
	 * - If the frame is present in the shared Cache, the cached FrameHolder is
	 *   returned and its model references are registered in the session cache.
	 * - Otherwise the appropriate builder is invoked depending on the system
	 *   class.
	 *
	 * @param utdCS string in the form "system=frameType" (for example
	 *              "space=ICRS(2000)")
	 * @return a FrameHolder containing the constructed frame; frameXml may be
	 *         null to signal the frame was already recorded
	 * @throws Exception on parsing or mapping errors (MappingError for unknown system)
	 */
	public FrameHolder createFrame(String utdCS) throws Exception {
		
		String[] parts = utdCS.split("=");
		String systemClass = parts[0];
		String frameType = parts[1];

		String frameId = this.buildID("_".concat(systemClass), frameType);
		FrameHolder frameHolder = new FrameHolder(systemClass, frameId, null, null);
				
		// If the session already recorded the id as global, we don't need to
		// produce the frame XML again; return a holder with null frameXml.
		if( this.sessionCache.containsGlobalsId(frameId)) {
			frameHolder.frameXml = null;
			return frameHolder;
		}
		
		// If another request already created and cached the frame, reuse it.
		if( (frameHolder = Cache.getFrameHolder(frameId)) != null ) {
			/*
			 * Although the frame is already in cache, we must reference the 
			 * related model in the annotations so callers can emit MODEL entries.
			 */
			if( frameHolder.modelPrefix != null ) {
				this.sessionCache.storeReferencedModel(frameHolder.modelPrefix, frameHolder.modelUrl);
			}
			return frameHolder;
		}
				
		switch(systemClass) {
		case "space":
		case Glossary.CSClass.SPACE:
			// Mark the ID as global in the session and build the space frame
			this.sessionCache.storeGlobalsId(frameId);
			frameHolder = this.buildSpaceFrame(systemClass, frameType, frameId);
			this.storeInCache(frameHolder);
			return frameHolder;
		case Glossary.CSClass.PHOTCAL:
			// Photometric calibration frames come with an associated filter
			this.sessionCache.storeGlobalsId(frameId);
			String filterId = frameId.replace("photCal", "photFilter");
			this.sessionCache.storeGlobalsId(filterId);
			frameHolder = this.buildPhotCal(frameType, frameId, filterId);
			this.storeInCache(frameHolder);
			return frameHolder;
		case Glossary.CSClass.FILTER_HIGH:
		case Glossary.CSClass.FILTER_LOW:
			// These are filter-only descriptors; build the photcal and adjust ids
			this.sessionCache.storeGlobalsId(frameId);
			filterId = frameId.replace("photCal", "photFilter");
			this.sessionCache.storeGlobalsId(filterId);
			frameHolder = this.buildPhotCal(frameType, frameId, filterId);
			// The produced holder corresponds to the filter id rather than the
			// photcal id (buildPhotCal uses photcalId internally).
			frameHolder.frameId = filterId;
			// restore the correct system class (squashed by buildPhotCal)
 			frameHolder.systemClass = systemClass;
			this.storeInCache(frameHolder);
			return frameHolder;
		case Glossary.CSClass.LOCAL:
			// Local frames are loaded from classpath snippets
			this.sessionCache.storeGlobalsId(frameId);
			frameHolder = this.buildLocalFrame(systemClass, frameType, frameId);
			this.storeInCache(frameHolder);
			return frameHolder;
		default:
			throw new MappingError("reading CS: Unknown frame type: " + systemClass);
		}
	}
	
	/**
	 * Store the given FrameHolder in the shared Cache if it contains XML.
	 *
	 * The Cache is used to avoid reconstructing identical frames across
	 * different requests.
	 */
	private void storeInCache(FrameHolder frameHolder) {
		if(frameHolder.frameXml != null) {
			Cache.storeFrameHolder(frameHolder);
		}
	}
	
	/**
	 * Build a FrameHolder from a local snippet file located in the classpath.
	 *
	 * The file name convention is: snippets/mango.frame.<frameType>.xml
	 *
	 * @param systemClass typically Glossary.CSClass.LOCAL
	 * @param frameType name of the snippet file without extension
	 * @param frameId id to assign to the resulting FrameHolder
	 * @return FrameHolder with its frameXml filled from the snippet
	 * @throws IOException when reading the resource fails
	 * @throws MappingError when the snippet is missing
	 */
	private FrameHolder buildLocalFrame(String systemClass, String frameType, String frameId) throws IOException, MappingError {
		
		InputStream is = getClass().getClassLoader()
		        .getResourceAsStream("snippets/mango.frame." + frameType + ".xml");
		if( is == null) {
			throw new MappingError("cannot find local MIVOT snippet: snippets/mango.frame." + frameType + ".xml");
		}
		byte[] bytes;
		try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
		    int n;
		    byte[] data = new byte[4096];
		    while ((n = is.read(data)) != -1) {
		        buffer.write(data, 0, n);
		    }
		    bytes = buffer.toByteArray();
		}       
		FrameHolder frameHolder = new FrameHolder(Glossary.CSClass.LOCAL, frameId, null, null);
		frameHolder.setFrame(new String(bytes, StandardCharsets.UTF_8));
		return frameHolder;

	}
	
	/**
	 * Build a SPACE FrameHolder from a frameType string.
	 *
	 * The frameType is expected to contain the space reference frame and an
	 * optional equinox in parentheses, e.g. "ICRS(2000, BARYCENTER)" or
	 * "GALACTIC". The method parses these tokens, constructs the appropriate
	 * MivotInstance objects and registers the COORDS model if not already present.
	 *
	 * @param frameType descriptor of the space frame
	 * @param frameId identifier to assign to the constructed frame
	 * @return populated FrameHolder for the space frame
	 * @throws MappingError on invalid input or mapping problems
	 */
	private FrameHolder buildSpaceFrame(String systemClass, String frameType, String frameId) throws MappingError {
		
		String[] parts = frameType.split("\\(");
		String spaceRefFrame = parts[0].trim();
        String equinox = (parts.length > 1)? parts[1].replace(")", "").trim(): null;
		String refPosition = "BARYCENTER";
		if( equinox != null )  {
			parts = equinox.split(",");
			equinox = parts[0].trim();
			refPosition = (parts.length > 1)? parts[1].replace(")", "").trim(): "BARYCENTER";
		}


        MivotInstance spaceSys = new MivotInstance(Glossary.ModelPrefix.COORDS + ":SpaceSys", null, frameId);
        MivotInstance spaceFrame = new MivotInstance(Glossary.ModelPrefix.COORDS + ":SpaceFrame",
					Glossary.ModelPrefix.COORDS + ":PhysicalCoordSys.frame", null);

        spaceFrame.addAttribute(Glossary.IvoaType.STRING,
				Glossary.ModelPrefix.COORDS + ":SpaceFrame.spaceRefFrame",
				"*" + spaceRefFrame, null);

        if (equinox != null) {
            spaceFrame.addAttribute(Glossary.ModelPrefix.COORDS + ":Epoch",
					Glossary.ModelPrefix.COORDS + ":SpaceFrame.equinox", equinox, null);
        }

        MivotInstance refLoc = new MivotInstance(
				Glossary.ModelPrefix.COORDS + ":StdRefLocation",
				Glossary.ModelPrefix.COORDS + ":SpaceFrame.refPosition", null);

        refLoc.addAttribute(Glossary.IvoaType.STRING,
				Glossary.ModelPrefix.COORDS + ":StdRefLocation.position",
				"*" + refPosition, null);
   
        spaceFrame.addInstance(refLoc);
        spaceSys.addInstance(spaceFrame);
		FrameHolder frameHolder = new FrameHolder(Glossary.CSClass.SPACE, frameId, Glossary.ModelPrefix.COORDS, Glossary.VodmlUrl.COORDS);
		frameHolder.setFrame(spaceSys);
		
		// Register the COORDS model as referenced for the current session so
		// buildMivotBlock will include a MODEL declaration.
		this.sessionCache.storeReferencedModel(Glossary.ModelPrefix.COORDS, Glossary.VodmlUrl.COORDS);
		return frameHolder;
	}
	
	/**
	 * Build a PHOTCAL FrameHolder from a frameType string.
	 *
	 * The frameType is expected to contain the photometric calibration type,
	 * e.g. "ABMAG" or "VEGAMAG". The method constructs the appropriate
	 * MivotInstance object and registers the PHOT model if not already present.
	 *
	 * @param frameType descriptor of the photometric calibration
	 * @param photcalId identifier to assign to the constructed frame
	 * @param filterId identifier to assign to the associated filter
	 * @return populated FrameHolder for the photometric calibration
	 * @throws Exception on mapping problems
	 */
	private FrameHolder buildPhotCal(String frameType, String photcalId, String filterId) throws Exception {
		FrameHolder frameHolder = new FrameHolder(Glossary.CSClass.PHOTCAL, photcalId, Glossary.ModelPrefix.PHOT, Glossary.VodmlUrl.PHOT);
		String photCalString = null;
		try {
			photCalString = this.photCalFactory.getMivotPhotCal(frameType, photcalId, filterId);
		} catch( MappingError me) {
			// If remote FPS mapping fails, fall back to a local snippet
			photCalString =  buildLocalFrame(Glossary.CSClass.PHOTCAL, frameType, photcalId).frameXml;
		}
		// Simplify the PhotCal XML to remove verbose elements before storing
		frameHolder.setFrame(PhotCalFactory.getSimplifiedPhotCal(photCalString));
		this.sessionCache.storeReferencedModel(Glossary.ModelPrefix.PHOT, Glossary.VodmlUrl.PHOT);
		
		return frameHolder;
	}
	/**
	 * Build a unique ID for a frame based on its prefix and type.
	 *
	 * The method removes spaces and replaces parentheses and commas
	 * with underscores to ensure a valid ID format.
	 *
	 * @param prefix prefix for the ID (e.g. "_space" or "_photCal")
	 * @param frameType descriptor of the frame
	 * @return constructed unique ID
	 */
	private String buildID(String prefix, String frameType) {
		return prefix + "_" 
			+ frameType.replace(" ", "")
			           .replace("(", "_")
			           .replace(")", "")
			           .replace(",", "_");
	}
}