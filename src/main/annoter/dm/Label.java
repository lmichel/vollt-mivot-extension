/**
 * Commented by chatGPT 
 */
package main.annoter.dm;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import main.annoter.meta.Glossary;
import main.annoter.meta.UtypeDecoder;
import main.annoter.meta.UtypeDecoderBrowser;
import main.annoter.mivot.FrameHolder;
import main.annoter.mivot.MivotInstance;
import tap.metadata.TAPColumn;

/**
 * Label property mapping for MIVOT.
 *
 * Purpose:
 * - Extract a textual label from the table mapping (hostAttribute "text").
 * - Collect optional vocabulary information (CT:vocabulary) and use it as
 *   the `uri` semantic; when the URI contains a fragment (#) the fragment
 *   is used as a short label.
 * - Preserve the TAP column description (when available) as the `description`
 *   semantic.
 * - Create a mapping attribute that points to the source ADQL column.
 *
 * Notes:
 * - This class expects a list of pre-decoded {@link UtypeDecoder} objects for
 *   the table and a list of FrameHolder entries (to allow emitting references
 *   when frames are available). No network I/O is performed here.
 */
public class Label extends Property {

	// MANGO data type identifier
	public static final String DMTYPE = "mango:Label";

	/** Browser helper over the provided UtypeDecoder list. */
	public UtypeDecoderBrowser utypeBrowser;
	
	/** Decoder that provides the textual label (hostAttribute == "text"). */
	public UtypeDecoder textUtypeDecoder = null; 
	
	/** Optional error instance placeholder (unused for Label but kept for API parity). */
	public MivotInstance errorInstance = null;
	
	/**
	 * Build a Label property using pre-decoded UType descriptors.
	 *
	 * The constructor performs the following steps:
	 * 1. Wrap the provided decoders in a UtypeDecoderBrowser for convenient lookups.
	 * 2. Find the decoder that carries the textual label (hostAttribute "text").
	 * 3. Extract an optional CT:vocabulary constant and, if present, set it as
	 *    the `uri` semantic; when the URI contains a fragment ("#fragment") the
	 *    fragment is used as the short `label` semantic.
	 * 4. Include the TAP column description (when present) as the `description`
	 *    semantic.
	 * 5. Create an attribute that maps the mango:Label.text role to the ADQL
	 *    column carrying the text.
	 *
	 * @param utypeDecoders pre-decoded UType descriptors for the table
	 * @param tableName unused here but kept for API compatibility
	 * @param frameHolders list of pre-built FrameHolder instances (may be empty)
	 * @param constants list of constant qualifiers (not used directly here)
	 * @throws Exception propagated from underlying getters (rare)
	 */
	public Label(List<UtypeDecoder> utypeDecoders,
				String tableName,
				List<FrameHolder> frameHolders,
				List<String> constants) throws Exception {

		super(DMTYPE, null, null);
		// Wrap decoders for lookup convenience
		this.utypeBrowser = new UtypeDecoderBrowser(utypeDecoders);
		
		// Find the decoder that maps the textual label value
		this.textUtypeDecoder = this.utypeBrowser.getUtypeDecoderByHostAttribute("text");
		String vocab = null;
		String label = null;
		Map<String, String> semantics = new LinkedHashMap<String, String>();

		// Defensive: only proceed when a text decoder was found
		if (this.textUtypeDecoder != null) {
			// Look for a vocabulary constant associated to the decoder (CT:vocabulary)
			vocab = this.textUtypeDecoder.getConstant(Glossary.CTClass.VOCABULARY);
			if (vocab != null && vocab.length() > 0) {
				semantics.put("uri", vocab);
				// If the URI has a fragment (e.g. http://...#Label) use the fragment
				// as a short human-friendly label.
				String[] uri = vocab.split("#");
				if (uri.length == 2 && uri[1].length() > 0) {
					label = uri[1];
				}
			}
			// Use the TAP column description as the semantic description when present
			TAPColumn tapColumn = this.textUtypeDecoder.getTapColumn();
			if (tapColumn != null) {
				String description = tapColumn.getDescription();
				if (description != null && description.length() > 0) {
					semantics.put("description", description);
				}
			}
			// If we derived a short label from the vocabulary, include it
			if (label != null && label.length() > 0) {
				semantics.put("label", label);
			}
			// Store assembled semantics for the property
			this.setSemantics(semantics);
			
			// Finally, create the attribute that binds the mango:Label.text role to the ADQL column
			if (tapColumn != null) {
				this.addAttribute("ivoa:string",
					DMTYPE + "." + this.textUtypeDecoder.getHostAttribute(),
					tapColumn.getADQLName(),
					tapColumn.getUnit());
			}
		}
		
		// Iterate provided FrameHolders: this project historically used frame
		// entries to represent vocabularies; keep a debug print to aid diagnosis
		// when a Vocabulary frame is present (no runtime side-effects).
		for (FrameHolder fh : frameHolders) {
			if (fh.systemClass.equals(Glossary.CTClass.VOCABULARY)) {
				// Intentionally text-only debug output; preserves original behaviour
				System.out.println("Vocabulary frame found: " + fh);
			}
		}
	}

}