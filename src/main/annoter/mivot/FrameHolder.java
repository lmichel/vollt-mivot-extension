package main.annoter.mivot;

/**
 * Simple holder object used during mapping to represent a FRAME and its
 * associated metadata.
 *
 * A FrameHolder keeps a frame identifier, the frame XML (as a String), and
 * references to the model that defines the frame. The object is intentionally
 * lightweight and is used by the mapping process to collect frame fragments
 * before they are added to the GLOBALS or TEMPLATES sections of a MIVOT
 * document.
 */
public class FrameHolder {
	
	/** Identifier of the frame within its model. */
	public String frameId;
	
	/** XML serialization of the frame instance. May be null until set. */
	public String frameXml;
	
	/** The system class (e.g. class name or semantic group) associated to this frame. */
	public String systemClass;
	
	/** Model prefix used to reference the model (VODML prefix). */
	public String modelPrefix;
	
	/** URL to the model VODML document (may be null). */
	public String modelUrl;

	/**
	 * Construct a new FrameHolder.
	 *
	 * @param systemClass semantic class or grouping for this frame (may be null)
	 * @param frameId identifier of the frame inside the model
	 * @param modelPrefix VODML model prefix used to reference the frame's model
	 * @param modelUrl URL pointing to the model VODML document (may be null)
	 */
	public FrameHolder(String systemClass,  String frameId, String modelPrefix, String modelUrl) {
		this.frameId = frameId;
		this.systemClass = systemClass;
		this.modelPrefix = modelPrefix;
		this.modelUrl = modelUrl;
	}
	
	/**
	 * Human-friendly string representation useful for debugging.
	 * Shows the system class, the frame id and the serialized frame XML (if set).
	 */
	public String toString() {
		return this.systemClass + "<>" + "->" + frameId + "\n" + ((frameXml == null)? "null": frameXml);
	}
	
	/**
	 * Set the frame XML from either a {@link MivotInstance} or a raw XML String.
	 *
	 * This method accepts the two common representations used by the mapping
	 * pipeline and converts them into a string stored in {@link #frameXml}.
	 *
	 * @param instance either a MivotInstance (preferred) or a String containing
	 *                 the frame XML
	 * @throws MappingError if the provided instance is of an unsupported type
	 */
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