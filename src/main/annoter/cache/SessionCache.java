package main.annoter.cache;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Session-scoped cache used while processing a single request/mapping run.
 *
 * Responsibilities:
 * - Track which VODML model prefixes have been referenced during processing
 *   so callers can emit appropriate <MODEL> declarations in the final MIVOT
 *   block (returned by {@link main.annoter.mivot.MivotAnnotations}).
 * - Record frame IDs (globalsIds) that were created for the current session
 *   to avoid emitting duplicate GLOBALS entries.
 *
 * Notes:
 * - This cache is intentionally lightweight and kept in memory for the
 *   duration of a single mapping operation (typically one HTTP request).
 * - The implementation is not synchronized. If you access a single
 *   SessionCache instance concurrently from multiple threads, add external
 *   synchronization.
 */
public class SessionCache {
	/**
	 * Map of model prefix -> model VODML URL referenced during the session.
	 * LinkedHashMap preserves insertion order which is convenient for
	 * deterministic output when emitting multiple MODEL declarations.
	 */
	private Map<String, String> referencedModels = new LinkedHashMap<String, String>();

	/**
	 * Collected frame IDs already created in the current query processing
	 * (prevents duplicates when the same frame would otherwise be emitted
	 * multiple times).
	 */
	private Set<String> globalsIds = new HashSet<String>();
	
	/**
	 * Retrieve the URL for a previously stored model prefix.
	 *
	 * @param referencedModel the model prefix (key)
	 * @return the model URL if present, or null if unknown
	 * @throws ClassNotFoundException kept for API compatibility (not thrown)
	 */
	public String getReferencedModel(String referencedModel) throws ClassNotFoundException {
		if( this.referencedModels.containsKey(referencedModel) ) {
			return this.referencedModels.get(referencedModel);
		}
		return null;
	}
	
	/**
	 * Store a referenced model (prefix -> URL) if not already present.
	 *
	 * This method returns the stored value which lets callers obtain a single
	 * canonical URL for the prefix after the call.
	 *
	 * @param modelPrefix canonical prefix used in generated MIVOT (e.g. "coords")
	 * @param modelUrl URL pointing to the model VODML document
	 * @return the URL stored for the prefix
	 */
	public String storeReferencedModel(String modelPrefix, String modelUrl) {
		if( this.referencedModels.containsKey(modelPrefix) == false ) {
			this.referencedModels.put(modelPrefix, modelUrl);
		}
		return this.referencedModels.get(modelPrefix);
	}
	
	/**
	 * Return the set of referenced model prefixes discovered in this session.
	 * The returned set is the live keySet of the internal map; callers should
	 * avoid mutating it directly.
	 *
	 * @return set of model prefixes in insertion order
	 */
	public Set<String> getReferencedModelList() {
		return this.referencedModels.keySet();
	}

	/**
	 * Check whether a global/frame id has already been recorded for this session.
	 *
	 * @param globalsId id to check (e.g. "_space_ICRS_2000")
	 * @return true when the id is already present
	 */
	public boolean containsGlobalsId(String globalsId) {
		return this.globalsIds.contains(globalsId);
	}

	/**
	 * Record a global/frame id for the session so subsequent callers can
	 * detect and skip duplicate frame emission.
	 *
	 * @param globalsId id to store
	 */
	public void storeGlobalsId(String globalsId) {
		this.globalsIds.add(globalsId);
	}
}