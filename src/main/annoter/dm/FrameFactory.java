package main.annoter.dm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import main.annoter.mivot.MappingError;
import main.annoter.mivot.MivotInstance;

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
	
	/** Singleton instance. */
	private static FrameFactory instance;

	/** Collected frame IDs already created by this factory (prevents duplicates). */
	private List<String>	ids = new ArrayList<String>();

	/** Map of model prefix -> vodml URL used by created frames. */
	public Map<String, String> models = new LinkedHashMap<String, String>();

	/**
	 * Return the singleton instance of the factory.
	 *
	 * @return shared FrameFactory instance
	 */
	public static FrameFactory getInstance() {
		if( instance == null ) {
			instance = new FrameFactory();
		}
		return instance;
	}
	
	/**
	 * Private constructor for the singleton.
	 */
	private FrameFactory() {		
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
		FrameHolder frameHolder = new FrameHolder(systemClass, frameId);
				
		if( this.ids.contains(frameId)) {
			 frameHolder.frameXml = null;
			 return frameHolder;
		}
		
		switch(systemClass) {
		case "space":
		case Glossary.CSClass.SPACE:
			this.ids.add(frameId);
			return this.buildSpaceFrame(frameType, frameId);
		case Glossary.CSClass.PHOTCAL:
			this.ids.add(frameId);
			String filterId = frameId.replace("photCal", "photFilter");
			this.ids.add(filterId);
			return this.buildPhotCal(frameType, frameId, filterId);
		default:
			throw new MappingError("reading CS: Unknown frame type: " + systemClass);
		}
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
	private FrameHolder buildSpaceFrame(String frameType, String frameId) throws MappingError {
		
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

        spaceFrame.addAttribute(Glossary.IvoaType.STRING, Glossary.ModelPrefix.COORDS + ":SpaceFrame.spaceRefFrame", spaceRefFrame, null);

        if (equinox != null) {
            spaceFrame.addAttribute(Glossary.ModelPrefix.COORDS + ":Epoch",
					Glossary.ModelPrefix.COORDS + ":SpaceFrame.equinox", equinox, null);
        }

        MivotInstance refLoc = new MivotInstance(
				Glossary.ModelPrefix.COORDS + ":StdRefLocation",
				Glossary.ModelPrefix.COORDS + ":SpaceFrame.refPosition", null);

        refLoc.addAttribute(Glossary.IvoaType.STRING, Glossary.ModelPrefix.COORDS + ":StdRefLocation.position", refPosition, null);
   
        spaceFrame.addInstance(refLoc);
        spaceSys.addInstance(spaceFrame);
		FrameHolder frameHolder = new FrameHolder(Glossary.CSClass.SPACE, frameId);
		frameHolder.setFrame(spaceSys);
		
		if( this.models.get(Glossary.ModelPrefix.COORDS) == null ) {
			this.models.put(Glossary.ModelPrefix.COORDS, Glossary.VodmlUrl.COORDS);
		}
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
		FrameHolder frameHolder = new FrameHolder(Glossary.CSClass.PHOTCAL, photcalId);

		frameHolder.setFrame(PhotCal.getMivotPhotCal(frameType, photcalId, filterId));
		if( this.models.get(Glossary.ModelPrefix.PHOT) == null ) {
			this.models.put(Glossary.ModelPrefix.PHOT, Glossary.VodmlUrl.PHOT);
		}

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
	
	/**
	 * Check whether an id was already created by this factory.
	 *
	 * @param id id to check
	 * @return true if the id is known, false otherwise
	 */
	public boolean hasId(String id) {
		return this.ids.contains(id);

	}
}