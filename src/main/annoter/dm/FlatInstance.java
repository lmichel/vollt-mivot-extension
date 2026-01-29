package main.annoter.dm;

import java.util.List;

import main.annoter.meta.UtypeDecoder;
import main.annoter.mivot.MivotInstance;

/**
 * Lightweight helper that builds a simple (flat) MivotInstance from a group
 * of related {@link UtypeDecoder} entries.
 *
 * Usage and assumptions:
 * - All provided UtypeDecoder objects are expected to describe the same
 *   innerClass (they are different attributes of a single frame-like
 *   structure). The implementation uses the first decoder's innerClass as
 *   the instance type when constructing the MivotInstance.
 * - Each decoder contributes a single RealQuantity attribute to the
 *   resulting instance. The attribute name is constructed as
 *   <innerClass>.<innerAttribute> and the attribute's value points to the
 *   ADQL column name provided by the decoder's TAPColumn.
 * - This class is intentionally small and does not validate input beyond
 *   relying on the decoders being non-empty and well-formed; callers must
 *   ensure the decoder list is non-empty.
 */
public class FlatInstance {
	/** DM role used when the instance is embedded in a parent. */
	protected String dmrole;
	
	/** Decoders that describe the inner attributes of the flat instance. */
	protected List<UtypeDecoder> utypeDecoders;
	
	/** Optional dmid assigned to the produced instance (may be null). */
	protected String dmid;
	
	/**
	 * Create a new FlatInstance assembler.
	 *
	 * @param dmrole role used when embedding the produced instance
	 * @param utypeDecoders list of decoders describing the instance attributes
	 * @param dmid optional id assigned to the produced instance
	 */
	public FlatInstance(String dmrole, List<UtypeDecoder> utypeDecoders, String dmid) {
		this.dmrole = dmrole;
		this.utypeDecoders = utypeDecoders;
		this.dmid = dmid;
	}
	
	/**
	 * Convert the collected decoders into a MivotInstance.
	 *
	 * Behavior:
	 * - The method uses the first decoder's innerClass as the instance's dmtype.
	 * - For each decoder an attribute is added with:
	 *     type: "ivoa:RealQuantity"
	 *     role: <innerClass>.<innerAttribute>
	 *     value: ADQL column name (from the decoder's TAPColumn)
	 *     unit: unit from the TAPColumn
	 *
	 * @return constructed MivotInstance populated with attributes for each decoder
	 * @throws Exception propagated from MivotInstance methods (rare)
	 */
	public  MivotInstance getMivotInstance() throws Exception {
		// NOTE: method assumes utypeDecoders is non-empty. This mirrors the
		// original implementation; callers should ensure this precondition.
		MivotInstance flatInstance = new MivotInstance(this.utypeDecoders.get(0).getInnerClass(), this.dmrole, this.dmid);
		for (UtypeDecoder mappableColumn : this.utypeDecoders) {
			// Construct attribute role as <innerClass>.<innerAttribute>
			flatInstance.addAttribute(
				"ivoa:RealQuantity",
				this.utypeDecoders.get(0).getInnerClass() + "." + mappableColumn.getInnerAttribute(),
				mappableColumn.getTapColumn().getADQLName(),
				mappableColumn.getTapColumn().getUnit()
			);
		}
		return flatInstance;
	}

}