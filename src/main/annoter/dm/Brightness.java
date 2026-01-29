/**
 * Represents a photometric property (Brightness) attached to a mapped row.
 *
 * Responsibilities:
 * - Locate the utype decoder that carries the photometric value (usually
 *   with a hostAttribute named "value").
 * - Build a MIVOT attribute for the magnitude value (RealQuantity) pointing
 *   to the table column.
 * - Detect and build an associated error instance when available (sigma,
 *   low/high asymmetric errors, etc.).
 * - Track the photometric calibration identifier (photcal) discovered from
 *   the associated utypes or error decoders; this id is later used to add
 *   references to PhotCal frames in the final MIVOT output.
 *
 * Notes:
 * - This class is constructed from a list of UtypeDecoder objects (already
 *   decoded for a table) and optional FrameHolder entries that may hold
 *   pre-built photCal frames.
 * - No network or I/O operations are performed here; this class only builds
 *   in-memory MivotInstance fragments.
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
 * Represents a photometric property (Brightness) attached to a mapped row.
 *
 * Responsibilities:
 * - Locate the utype decoder that carries the photometric value (usually
 *   with a hostAttribute named "value").
 * - Build a MIVOT attribute for the magnitude value (RealQuantity) pointing
 *   to the table column.
 * - Detect and build an associated error instance when available (sigma,
 *   low/high asymmetric errors, etc.).
 * - Track the photometric calibration identifier (photcal) discovered from
 *   the associated utypes or error decoders; this id is later used to add
 *   references to PhotCal frames in the final MIVOT output.
 *
 * Notes:
 * - This class is constructed from a list of UtypeDecoder objects (already
 *   decoded for a table) and optional FrameHolder entries that may hold
 *   pre-built photCal frames.
 * - No network or I/O operations are performed here; this class only builds
 *   in-memory MivotInstance fragments.
 */
public class Brightness extends Property {

	// MANGO data type for Brightness
	public static final String DMTYPE = "mango:Brightness";

	/** Photometric calibration id (dmid) if available (may be null). */
	public String photcal;

	/** Utility to query the provided UtypeDecoder list. */
	public UtypeDecoderBrowser utypeBrowser;

	/** Decoder that provides the "value" host attribute (magnitude column). */
	public UtypeDecoder valueUtypeDecoder = null; 

	/** MivotInstance describing the error structure (if any). */
	public MivotInstance errorInstance = null;
	
	/**
	 * Construct a Brightness property from decoded utypes and frame holders.
	 *
	 * Flow:
	 * 1. Wrap the provided decoders in a UtypeDecoderBrowser for convenient lookups.
	 * 2. Attempt to find the decoder whose hostAttribute is "value" and, if
	 *    present, add a RealQuantity attribute mapping that points to the
	 *    ADQL column name and unit.
	 * 3. Build an error instance (if error-related utypes are present).
	 * 4. Add the error instance and any photCal frame references found in the
	 *    provided frameHolders.
	 *
	 * @param utypeDecoders decoders extracted from table metadata
	 * @param tableName unused here but kept for signature consistency
	 * @param frameHolders any already-built FrameHolder objects (used to
	 *                     emit references to external frames, e.g. PhotCal)
	 * @param constants list of constant qualifiers (not directly used here)
	 * @throws Exception propagated from nested constructors or builders
	 */
	@SuppressWarnings("serial")
	public Brightness(List<UtypeDecoder> utypeDecoders,
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
		// Prepare browser for convenient utype lookups
		this.utypeBrowser = new UtypeDecoderBrowser(utypeDecoders);
		
		// Find the decoder that maps the magnitude value (hostAttribute == "value")
		this.valueUtypeDecoder = this.utypeBrowser.getUtypeDecoderByHostAttribute("value");
		if( this.valueUtypeDecoder != null ) {
			// Record photometric calibration reference if present in the decoder
			this.photcal = this.valueUtypeDecoder.getFrame("photcal");
			TAPColumn tapColumn = this.valueUtypeDecoder.getTapColumn();
			if( this.valueUtypeDecoder != null ) {
				// Add a RealQuantity attribute that points to the ADQL column and unit
				this.addAttribute("ivoa:RealQuantity",
					DMTYPE + "." + this.valueUtypeDecoder.getHostAttribute(), 
					tapColumn.getADQLName(),
					tapColumn.getUnit());
			}

		}
		
		// Build an error instance from available decoders, if any
		this.buildErrorInstance();

		// If an error instance exists, attach it to this property
		if( this.errorInstance != null ) {
			this.addInstance(this.errorInstance);
		}
		
		// Add references to any supplied PhotCal frames (FrameHolder entries)
		for (FrameHolder fh : frameHolders) {
			if (fh.systemClass.equals(Glossary.CSClass.PHOTCAL)) {
				this.addReference(DMTYPE + ".photCal", fh.frameId);
			}
		}
	}

	/**
	 * Inspect decoders to build an error instance for the brightness value.
	 *
	 * The method prefers a symmetric 1D error ("sigma"). If not present it
	 * tries to find asymmetric errors ("high","low"). When an error decoder
	 * is found a PropertyError helper is used to create a MivotInstance.
	 *
	 * @throws Exception propagated from PropertyError or instance creation
	 */
	private void buildErrorInstance() throws Exception {
		// Look first for a symmetric error with inner attribute "sigma"
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
	 * this Brightness property, or null when none was discovered.
	 *
	 * @return photcal dmid or null
	 */
	public String getPhotCalID() {
		return this.photcal;
	}
}