/**
 * Representation of a spatial/temporal location for a mapped row.
 *
 * Responsibilities:
 * - Collect positional attributes (longitude, latitude, parallax, radial
 *   velocity, proper motions) from decoded UTypes and expose them as
 *   MIVOT attributes.
 * - Build and attach any associated error descriptions (position/properMotion/
 *   parallax) using {@link PropertyError} when error-related UTypes exist.
 * - Record references to SPACE and TIME frames provided as {@link FrameHolder}
 *   instances so callers can emit MODEL/TEMPLATES entries as needed.
 *
 * Design notes:
 * - The constructor accepts a pre-decoded list of {@link UtypeDecoder} for
 *   the table; it does not query metadata itself.
 * - No network or IO is performed here — this class only assembles in-memory
 *   MivotInstance fragments.
 */
package main.annoter.dm;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import main.annoter.cache.MappingCache;
import main.annoter.meta.Glossary;
import main.annoter.meta.UtypeDecoder;
import main.annoter.mivot.FrameHolder;
import main.annoter.mivot.MivotInstance;
import tap.metadata.TAPColumn;

/**
 * Representation of a spatial/temporal location for a mapped row.
 *
 * Responsibilities:
 * - Collect positional attributes (longitude, latitude, parallax, radial
 *   velocity, proper motions) from decoded UTypes and expose them as
 *   MIVOT attributes.
 * - Build and attach any associated error descriptions (position/properMotion/
 *   parallax) using {@link PropertyError} when error-related UTypes exist.
 * - Record references to SPACE and TIME frames provided as {@link FrameHolder}
 *   instances so callers can emit MODEL/TEMPLATES entries as needed.
 *
 * Design notes:
 * - The constructor accepts a pre-decoded list of {@link UtypeDecoder} for
 *   the table; it does not query metadata itself.
 * - No network or IO is performed here — this class only assembles in-memory
 *   MivotInstance fragments.
 */
public class EpochPosition extends Property {

	public static final String DMTYPE = "mango:EpochPosition";
	public List<String> frames;
	private String tableName;
	private List<UtypeDecoder> positionErrorUtypes = new ArrayList<>();
	private List<UtypeDecoder> pmErrorUtypes = new ArrayList<>();
	private List<UtypeDecoder> parallaxErrorUtypes = new ArrayList<>();

	/**
	 * Construct an EpochPosition property from decoded UType information.
	 *
	 * Flow summary:
	 * 1. Iterate provided decoders and add scalar (non-inner) attributes as
	 *    RealQuantity attributes pointing to the ADQL column names.
	 * 2. Extract a possible epoch constant (CT:epoch) and add it as an obsDate
	 *    attribute when present.
	 * 3. Build and attach an errors instance if any error UTypes were found in
	 *    the mapping cache for the same table.
	 * 4. Add references to any supplied SPACE/TIME FrameHolders.
	 *
	 * @param utypeDecoders list of decoded UType descriptors for the table
	 * @param tableName the ADQL table name used for later mapping-cache lookups
	 * @param frameHolders list of pre-built frames (e.g. space/time/photCal)
	 * @param constants list of constant qualifiers (unused here but kept for API compatibility)
	 * @throws Exception propagated from nested constructors or builders
	 */
	public EpochPosition(List<UtypeDecoder> utypeDecoders,
					String tableName,
					List<FrameHolder> frameHolders,
					List<String> constants) throws Exception {

		super(DMTYPE, null, null, new HashMap<String, String>() {
			{
			put("description", "6 parameters position");
			put("uri", "https://www.ivoa.net/rdf/uat/2024-06-25/uat.html#astronomical-location");
			put("label", "Astronomical location");
			}
		});
		this.tableName = tableName;
		
		// epoch constant if present (CT:epoch qualifier)
		String epoch = null;
		// Iterate through decoders provided for the current table mapping
		for (UtypeDecoder mappableColumn : utypeDecoders) {
			String attribute = mappableColumn.getHostAttribute();
			TAPColumn tapColumn = mappableColumn.getTapColumn();
			String adqlName = tapColumn.getADQLName();
			// keep any CS/CT frame qualifiers discovered on the decoder
			this.frames = mappableColumn.getFrames();
			// Only add top-level attributes (those without innerRole) as direct
			// RealQuantity attributes. Inner-role attributes represent structured
			// pieces (like errors) which are handled separately.
			if( mappableColumn.getInnerRole() == null ) {
				this.addAttribute("ivoa:RealQuantity", DMTYPE + "." + attribute, adqlName, tapColumn.getUnit());
			}
			// If the decoder carries a CT:epoch constant, record it for later
			if( epoch == null ) {
				epoch = mappableColumn.getConstant(Glossary.CTClass.EPOCH);
			}
		}
		
		// If an epoch constant was found, add it as an observation date attribute
		if( epoch != null ) {
			// strip leading 'J' if present and prefix with '*' which signals a
			// constant value in the Mivot convention used by this project.
			this.addAttribute("year","mango:EpochPosition.obsDate",
					"*" + epoch.replace("J",""),
					null);

		}

		// Build and attach an error instance if error-related UTypes are present
		MivotInstance erri = this.buildEpochErrors();
		if (erri != null) {
			this.addInstance(erri);
		}
		
		// Attach frame references (space/time) so callers can emit MODEL entries
		for (FrameHolder fh : frameHolders) {
			if (fh.systemClass.equals(Glossary.CSClass.SPACE)) {
				this.addReference(DMTYPE + ".spaceSys", fh.frameId);
			}
			if (fh.systemClass.equals(Glossary.CSClass.TIME)) {
				this.addReference(DMTYPE + ".timeSys", fh.frameId);
			}
		}
	}

	/**
	 * Build a composite errors instance for the epoch-position property.
	 *
	 * This method queries the global MappingCache for decoders that match
	 * this DMTYPE and groups error-related decoders by their innerRole
	 * (position, properMotion, parallax). For each group found it creates
	 * a {@link PropertyError} component and aggregates them into a
	 * mango:EpochPositionErrors instance.
	 *
	 * @return a MivotInstance representing the errors, or null when no errors mapped
	 * @throws Exception propagated from inner builders or checks
	 */
	private MivotInstance buildEpochErrors()
				throws Exception {
		MappingCache MAPPING_CACHE = MappingCache.getCache();

		// Retrieve all decoders for this table that claim to map to mango:EpochPosition
		List<UtypeDecoder> mappableColumns = MAPPING_CACHE.getTableMapping(this.tableName, DMTYPE);

		// Group decoders by their inner role (errors.position, errors.properMotion, errors.parallax)
		for (UtypeDecoder mappableColumn : mappableColumns) {
			if( "errors".equals(mappableColumn.getHostAttribute()) ){
				if ("position".equals(mappableColumn.getInnerRole())) {
					// Validate consistency of inner class when adding to the list
					mappableColumn.checkInnerClass(positionErrorUtypes);
					positionErrorUtypes.add(mappableColumn);
				}
				if ("properMotion".equals(mappableColumn.getInnerRole())) {
					mappableColumn.checkInnerClass(pmErrorUtypes);
					pmErrorUtypes.add(mappableColumn);
				}
				if ("parallax".equals(mappableColumn.getInnerRole())) {
					mappableColumn.checkInnerClass(parallaxErrorUtypes);
					parallaxErrorUtypes.add(mappableColumn);
				}
			}
		}

		// Build the top-level EpochPositionErrors container
		MivotInstance errorInstance = new MivotInstance(
			"mango:EpochPositionErrors",
			DMTYPE + ".errors",
			null
		);

		boolean errorMapped = false;
		MivotInstance errorComponent;
		// For each potential error group build a PropertyError component and add it
		if( (errorComponent = this.buildErrorComponent("mango:EpochPositionErrors.position",
					this.positionErrorUtypes) ) != null ) {
			errorMapped = true;
			errorInstance.addInstance(errorComponent);
		}
		if( (errorComponent = this.buildErrorComponent("mango:EpochPositionErrors.properMotion",
					this.pmErrorUtypes) ) != null ) {
			errorMapped = true;
			errorInstance.addInstance(errorComponent);
		}
		if( (errorComponent = this.buildErrorComponent("mango:EpochPositionErrors.parallax",
					this.parallaxErrorUtypes) ) != null ) {
			errorMapped = true;
			errorInstance.addInstance(errorComponent);
		}
		// Return the composed errors instance, or null when nothing was mapped
		return (errorMapped ==  true)? errorInstance: null;
	}
	
	/**
	 * Helper that builds a PropertyError MivotInstance from a list of decoders.
	 *
	 * @param dmrole the dmid/dmrole to use for the error component
	 * @param utypeDecoders list of decoders describing the error fields
	 * @return constructed MivotInstance or null when the decoder list is empty
	 * @throws Exception propagated from PropertyError
	 */
	private MivotInstance buildErrorComponent(String dmrole, List<UtypeDecoder> utypeDecoders)
				throws Exception {		
		if (utypeDecoders.isEmpty()) {
			return null;
		}
		PropertyError errorComponent = new PropertyError(dmrole, null, 0.68, utypeDecoders, null); 				
		return errorComponent.getMivotInstance();
	}
}