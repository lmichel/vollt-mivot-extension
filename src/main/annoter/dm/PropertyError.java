package main.annoter.dm;

import java.util.List;

import main.annoter.meta.UtypeDecoder;
import main.annoter.mivot.MivotInstance;

/**
 * Represents error-related properties for a measured property.
 *
 * <p>This class stores the assumed error distribution and a confidence level for
 * the reported uncertainty. The distribution defaults to "Gaussian" and the
 * confidence level defaults to 0.68 when an invalid value is provided.</p>
 *
 * <p>Note: the field name "distribtion" contains a historical misspelling that
 * is intentionally retained for backward compatibility with existing serialized
 * data and external code that may reference the same property name.</p>
 */
public class PropertyError extends FlatInstance {
	/**
	 * The (historically misspelled) distribution name. Defaults to "Gaussian" when null.
	 */
	private String distribtion;
	
	/**
	 * Confidence level between 0 (exclusive) and 1 (inclusive). Defaults to 0.68 when out of range.
	 */
	private double confidenceLevel;

	/** DM role assigned to the produced error instance. Provided by callers. */
	protected String dmrole;

	/** Fully qualified host class used as attribute prefix for statistical info. */
	protected String hostClass = "mango:error.PropertyError";

	/** Container instance built during getMivotInstance(). */
	protected MivotInstance errorMivotInstance;

	/**
	 * Create a PropertyError with a distribution name and a confidence level.
	 *
	 * @param dmrole role used for the produced error instance (e.g. "mango:...")
	 * @param distribution the name of the error distribution (e.g. "Gaussian"). If null, defaults to "Gaussian".
	 * @param confidenceLevel the confidence level as a fraction (0 < level <= 1). If out of range (<=0 or >1) the default 0.68 is used.
	 * @param utypeDecoders decoders describing the error-related fields (passed to super)
	 * @param dmid optional identifier for the produced instance (may be null)
	 */
	public PropertyError(String dmrole, String distribution,
					double confidenceLevel, List<UtypeDecoder> utypeDecoders, String dmid) {
		super(dmrole, utypeDecoders, dmid);
		this.distribtion = (distribution == null)? "Gaussian" : distribution;
		// Enforce a sensible default when confidence level is outside (0,1]
		this.confidenceLevel = (confidenceLevel <= 0 || confidenceLevel > 1)? 0.68: confidenceLevel;
		this.dmrole = dmrole;
	}

	/**
	 * Return the (possibly defaulted) distribution name.
	 * @return distribution name (note: historically misspelled field name is used).
	 */
	public String getDistribtion() {
		return distribtion;
	}

	/**
	 * Return the confidence level used for the error (fraction between 0 and 1).
	 * @return confidence level (defaults to 0.68 when input was invalid).
	 */
	public double getConfidenceLevel() {
		return confidenceLevel;
	}

	/**
	 * Add statistical information about this error to the provided Mivot instance.
	 *
	 * This method writes two attributes using the host class prefix:
	 * - <hostClass>.distribution : the distribution name (prefixed with '*')
	 * - <hostClass>.confidenceLevel : the confidence level (prefixed with '*')
	 *
	 * @throws Exception if the underlying mivotInstance.addAttribute calls fail
	 */
	public void addStatInfo() throws Exception {
		// Ensure errorMivotInstance is available (created in getMivotInstance)
		this.errorMivotInstance.addAttribute(
				"ivoa:string",
				this.hostClass + ".distribution",
				"*" + this.distribtion,
				null);
		this.errorMivotInstance.addAttribute(
				"ivoa:real",
				this.hostClass + ".confidenceLevel",
				"*" + this.confidenceLevel,
				null);
	}
	
	/**
	 * Build the MivotInstance representing the error and attach statistical info.
	 *
	 * This method delegates to {@link FlatInstance#getMivotInstance()} to build
	 * a flat error instance from the configured UType decoders, then appends
	 * distribution and confidenceLevel attributes.
	 *
	 * @return fully populated MivotInstance for the error
	 * @throws Exception propagated from nested MivotInstance builders
	 */
	public  MivotInstance getMivotInstance() throws Exception {
		this.errorMivotInstance = super.getMivotInstance();
		this.addStatInfo();
		return this.errorMivotInstance;
	}

}