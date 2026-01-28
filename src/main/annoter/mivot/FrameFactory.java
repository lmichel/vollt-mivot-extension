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
 * system strings. Implements a simple singleton to cache already created IDs
 * and models.
 *
 * Expected input for {@link #createFrame(String)} is a string of the form
 * "system=frameType" (for example "space=ICRS(2000)"). The factory will
 * parse the frameType and build the appropriate MIVOT instances.
 */
public class FrameFactory {
	
	private PhotCalFactory photCalFactory = new PhotCalFactory();
	private SessionCache sessionCache;
	/**
	 * Return the singleton instance of the factory.
	 * @param sessionCache 
	 *
	 * @return shared FrameFactory instance
	 */
	public static FrameFactory getInstance(SessionCache sessionCache) {
		return new FrameFactory(sessionCache);
	}
	
	/**
	 * Private constructor for the singleton.
	 */
	private FrameFactory(SessionCache sessionCache) {
		this.sessionCache = sessionCache;
	}
	
	/**
	 * Create a FrameHolder from a combined system=frameType string.
	 *
	 * The method splits the input on the first '=' character. The left part is
	 * treated as the system class (for example "space" or "photCal") and the
	 * right part is the frame descriptor passed to the specific builder.
	 *
	 * @param utdCS string in the form "system=frameType"
	 * @return a FrameHolder containing the constructed frame or null frameXml when
	 *         the id was already created
	 * @throws Exception on parsing or mapping errors (MappingError for unknown system)
	 */
	public FrameHolder createFrame(String utdCS) throws Exception {
		
		String[] parts = utdCS.split("=");
		String systemClass = parts[0];
		String frameType = parts[1];

		String frameId = this.buildID("_".concat(systemClass), frameType);
		FrameHolder frameHolder = new FrameHolder(systemClass, frameId, null, null);
				
		if( this.sessionCache.containsGlobalsId(frameId)) {
			frameHolder.frameXml = null;
			return frameHolder;
		}
		
		if( (frameHolder = Cache.getFrameHolder(frameId)) != null ) {
			/*
			 * Although the frame is already in cache, we must reference the 
			 * related model in the annotations 
			 */
			if( frameHolder.modelPrefix != null ) {
				this.sessionCache.storeReferencedModel(frameHolder.modelPrefix, frameHolder.modelUrl);
			}
			return frameHolder;
		}
				
		switch(systemClass) {
		case "space":
		case Glossary.CSClass.SPACE:
			this.sessionCache.storeGlobalsId(frameId);
			frameHolder = this.buildSpaceFrame(systemClass, frameType, frameId);
			this.storeInCache(frameHolder);
			return frameHolder;
		case Glossary.CSClass.PHOTCAL:
			this.sessionCache.storeGlobalsId(frameId);
			String filterId = frameId.replace("photCal", "photFilter");
			this.sessionCache.storeGlobalsId(filterId);
			frameHolder = this.buildPhotCal(frameType, frameId, filterId);
			this.storeInCache(frameHolder);
			return frameHolder;
		case Glossary.CSClass.FILTER_HIGH:
		case Glossary.CSClass.FILTER_LOW:
			this.sessionCache.storeGlobalsId(frameId);
			filterId = frameId.replace("photCal", "photFilter");
			this.sessionCache.storeGlobalsId(filterId);
			frameHolder = this.buildPhotCal(frameType, frameId, filterId);
			frameHolder.frameId = filterId;
			// restore the correct system class (squashed by buildPhotCal)
 			frameHolder.systemClass = systemClass;
			this.storeInCache(frameHolder);
			return frameHolder;
		case Glossary.CSClass.LOCAL:
			this.sessionCache.storeGlobalsId(frameId);
			frameHolder = this.buildLocalFrame(systemClass, frameType, frameId);
			this.storeInCache(frameHolder);
			return frameHolder;
		default:
			throw new MappingError("reading CS: Unknown frame type: " + systemClass);
		}
	}
	
	private void storeInCache(FrameHolder frameHolder) {
		if(frameHolder.frameXml != null) {
			Cache.storeFrameHolder(frameHolder);
		}
	}
	
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
	 * @param systemValue descriptor of the photometric calibration
	 * @param photcalId identifier to assign to the constructed frame
	 * @param filterId identifier to assign to the associated filter
	 * @param filterId2 
	 * @return populated FrameHolder for the photometric calibration
	 * @throws Exception on mapping problems
	 */
	private FrameHolder buildPhotCal(String frameType, String photcalId, String filterId) throws Exception {
		FrameHolder frameHolder = new FrameHolder(Glossary.CSClass.PHOTCAL, photcalId, Glossary.ModelPrefix.PHOT, Glossary.VodmlUrl.PHOT);
		String photCalString = null;
		try {
			photCalString = this.photCalFactory.getMivotPhotCal(frameType, photcalId, filterId);
		} catch( MappingError me) {
			photCalString =  buildLocalFrame(Glossary.CSClass.PHOTCAL, frameType, photcalId).frameXml;
		}
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