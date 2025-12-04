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
 * is retained here for backward compatibility with existing serialized data
 * and external code that may reference the same property name.</p>
 */
public class PropertyError extends FlatInstance {
	// The (historically misspelled) distribution name. Defaults to "Gaussian" when null.
	private String distribtion;
	// Confidence level between 0 (exclusive) and 1 (inclusive). Defaults to 0.68 when out of range.
	private double confidenceLevel;
	protected String dmrole;
	protected String hostClass = "mango:error";
;	protected MivotInstance errorMivotInstance;

	/**
	 * Create a PropertyError with a distribution name and a confidence level.
	 *
	 * @param distribution the name of the error distribution (e.g. "Gaussian"). If null, defaults to "Gaussian".
	 * @param confidenceLevel the confidence level as a fraction (0 &lt; level &le; 1). If out of range (<=0 or &gt;1) the default 0.68 is used.
	 */
	public PropertyError(String dmrole, String distribution,
			double confidenceLevel, List<UtypeDecoder> utypeDecoders, String dmid) {
		super(dmrole, utypeDecoders, dmid);
		this.distribtion = (distribution == null)? "Gaussian" : distribution;
		this.confidenceLevel = (confidenceLevel <= 0 || confidenceLevel > 1)? 0.68: confidenceLevel;
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
	 * @param mivotInstance target instance to receive the attributes
	 * @param hostClass prefix used for attribute names
	 * @throws Exception if the underlying mivotInstance.addAttribute calls fail
	 */
	public void addStatInfo() throws Exception {
		this.errorMivotInstance.addAttribute(
				"ivoa:string",
				this.hostClass + ".distribution",
				"*" + this.distribtion,
				null);
		this.errorMivotInstance.addAttribute(
				"ivoa:string",
				this.hostClass + ".confidenceLevel",
				"*" + this.confidenceLevel,
				null);
	}
	
	
	public  MivotInstance getMivotInstance() throws Exception {
		
		this.errorMivotInstance = super.getMivotInstance();
		this.addStatInfo();
		return this.errorMivotInstance;
	}

}