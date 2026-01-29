/**
 * Color property mapping for MIVOT.
 *
 * Responsibilities:
 * - Locate the utype decoder that carries the color value (hostAttribute "value").
 * - Create a RealQuantity attribute pointing to the ADQL column and unit.
 * - Build and attach an associated error instance when available.
 * - Build a mango:ColorDefinition instance which references high/low filter
 *   FrameHolder entries and includes an optional mode constant when provided.
 *
 * Notes:
 * - This class operates purely in-memory using decoded UType descriptors and
 *   pre-built FrameHolder objects; it performs no network I/O.
 */
package main.annoter.dm;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import main.annoter.meta.Glossary;
import main.annoter.meta.UtypeDecoder;
import main.annoter.meta.UtypeDecoderBrowser;
import main.annoter.mivot.FrameHolder;
import main.annoter.mivot.MivotInstance;
import tap.metadata.TAPColumn;

/**
 * Color mapping component used during annotation.
 *
 * The constructor builds the main color attribute, optional error instance
 * and a ColorDefinition instance that references filter frames.
 */
public class Color extends Property {

	// MANGO data type identifier
	public static final String DMTYPE = "mango:Color";

	/** Photometric calibration id (dmid) if available from decoders. */
	public String photcal;
	
	/** Browser helper to query the provided UtypeDecoder list. */
	public UtypeDecoderBrowser utypeBrowser;
	
	/** Decoder that provides the "value" host attribute (color column). */
	public UtypeDecoder valueUtypeDecoder = null; 
	
	/** Error instance (if any) for the color value. */
	public MivotInstance errorInstance = null;

	/**
	 * Construct a Color property from decoded utypes and frame holders.
	 *
	 * Flow:
	 * 1. Wrap provided decoders in a UtypeDecoderBrowser for convenient lookups.
	 * 2. Extract an optional "mode" constant from the constants list (if present)
	 *    and use it to populate the ColorDefinition.definition attribute.
	 * 3. Find the decoder that maps the color "value" and create a RealQuantity
	 *    attribute pointing to the ADQL column and unit.
	 * 4. Build an error instance if error-related utypes are present.
	 * 5. Create a ColorDefinition instance and add references to high/low filters.
	 *
	 * @param utypeDecoders decoders extracted from table metadata
	 * @param tableName unused here but kept for signature consistency
	 * @param frameHolders pre-built FrameHolder objects (used to reference filters)
	 * @param constants list of constant qualifiers (e.g. mode=...)
	 * @throws Exception propagated from nested constructors or builders
	 */
	@SuppressWarnings("serial")
	public Color(List<UtypeDecoder> utypeDecoders,
				String tableName,
				List<FrameHolder> frameHolders,
				List<String> constants) throws Exception {

		super(DMTYPE, null, null, new HashMap<String, String>() {
			{
				put("description", "magnitude value with its photometric system");
				put("uri", "https://www.ivoa.net/rdf/uat/uat.html#magnitude");
				put("label", "magnitude");
			}
		});

		// Wrap decoders for easier queries
		this.utypeBrowser = new UtypeDecoderBrowser(utypeDecoders);

		// Extract optional 'mode' constant from the provided constants list
		String mode = null;
		for( String constant : constants) {
			String[] parts = constant.split("=");
			if (parts.length < 2) continue; // defensive: ignore malformed entries
			String constantClass = parts[0];
			String value = parts[1];
			if( constantClass.equals("mode")) {
				mode = "*" + value; // prefix '*' marks a literal constant in Mivot convention
				break;
			}
		}

		// Find decoder for the color value (hostAttribute == "value")
		this.valueUtypeDecoder = this.utypeBrowser.getUtypeDecoderByHostAttribute("value");
		if( this.valueUtypeDecoder != null ) {
			// Record any photCal frame reference exposed by the decoder
			this.photcal = this.valueUtypeDecoder.getFrame("photcal");
			TAPColumn tapColumn = this.valueUtypeDecoder.getTapColumn();
			if( tapColumn != null ) {
				// Add a RealQuantity attribute that points to the ADQL column and unit
				this.addAttribute("ivoa:RealQuantity",
					DMTYPE + "." + this.valueUtypeDecoder.getHostAttribute(), 
					tapColumn.getADQLName(),
					tapColumn.getUnit());
			}

		}

		// Build an error instance (sigma or high/low) when present
		this.buildErrorInstance();

		// If an error instance exists, attach it to this property
		if( this.errorInstance != null ) {
			this.addInstance(this.errorInstance);
		}

		// Build a ColorDefinition instance and add references for high/low filters
		MivotInstance colorDef = new MivotInstance("mango:ColorDefinition", DMTYPE + ".colorDef", null);
		colorDef.addAttribute("mango.ColorDefinition", "mango:ColorDef.definition", mode, null);
		for (FrameHolder fh : frameHolders) {
			// FrameHolder.systemClass indicates whether this is a high/low filter
			if( fh.systemClass.equals(Glossary.CSClass.FILTER_HIGH) ) {
				colorDef.addReference("mango:ColorDefinition.high", fh.frameId);
			}
			else if( fh.systemClass.equals(Glossary.CSClass.FILTER_LOW) ) {
				colorDef.addReference("mango:ColorDefinition.low", fh.frameId);
			}
		}
		this.addInstance(colorDef);
	}

	/**
	 * Inspect decoders to build an error instance for the color value.
	 *
	 * The method prefers a symmetric 1D error ("sigma"). If not present it
	 * tries to find asymmetric errors ("high","low"). When an error decoder
	 * is found a PropertyError helper is used to create a MivotInstance.
	 *
	 * @throws Exception propagated from PropertyError or instance creation
	 */
	private void buildErrorInstance() throws Exception {
		// Prefer symmetric error "sigma"
		UtypeDecoder errorUtypeDecoder = this.utypeBrowser.getUtypeDecoderByInnerAttribute("sigma");
		if( errorUtypeDecoder != null ) {
			PropertyError errorFlat = new PropertyError(
					"mango:Brightness.error", 
					null,
					0.68,

					Arrays.asList(errorUtypeDecoder),
					null);
			this.errorInstance = errorFlat.getMivotInstance();
			// If photcal wasn't set from the value decoder try to obtain it from the error decoder
			if( this.photcal == null ) {
				this.photcal = errorUtypeDecoder.getFrame("photcal");
			}

		} else {
			// Otherwise look for asymmetric error decoders (high/low)
			List<UtypeDecoder> errorUtypeDecoders = this.utypeBrowser.getUtypeDecodersMatchingInnerAttributes(new String[] {"high", "low"});
			if(errorUtypeDecoders.size() > 0 && this.errorInstance == null ) {
				PropertyError errorFlat = new PropertyError(
						"mango:Brightness.error", 
						null,
						0.68,
					errorUtypeDecoders,
					null);
				this.errorInstance = errorFlat.getMivotInstance();
				if( this.photcal == null ) {
					this.photcal = errorUtypeDecoders.get(0).getFrame("photcal");
				}
			}
		}
	}

	/**
	 * Return the photometric calibration identifier (dmid) associated to
	 * this Color property, or null when none was discovered.
	 *
	 * @return photcal dmid or null
	 */
	public String getPhotCalID() {
		return this.photcal;
	}
}