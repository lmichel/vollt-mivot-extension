package main.annoter.meta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Helper class to search and filter a list of {@link UtypeDecoder} instances.
 *
 * This small utility centralizes common lookup patterns used by the mapping
 * pipeline: finding a decoder by its inner attribute, matching multiple
 * decoders against a set of inner attributes, and analogous host-attribute
 * lookups. The class is intentionally lightweight and does not modify the
 * underlying decoders.
 */
public class UtypeDecoderBrowser {
	/** The list of decoders this browser operates on. */
	public List<UtypeDecoder> utypeDecoders;
	
	/**
	 * Create a browser for the given list of decoders.
	 *
	 * @param utypeDecoders list of {@link UtypeDecoder} to be queried; callers
	 *                      may reuse or mutate this list after creating the browser
	 */
	public UtypeDecoderBrowser(List<UtypeDecoder> utypeDecoders) {
		this.utypeDecoders = utypeDecoders;
	}

	/**
	 * Find the first decoder whose innerAttribute equals the provided value.
	 *
	 * @param innerAttribute the inner attribute name to look for (may be null)
	 * @return the matching {@link UtypeDecoder} or null if none found
	 */
	public UtypeDecoder getUtypeDecoderByInnerAttribute(String innerAttribute) {
		for( UtypeDecoder utypeDecoder: this.utypeDecoders ) {
			String innerAttr = utypeDecoder.getInnerAttribute();
			// skip decoders that don't define an innerAttribute
			if( innerAttr != null && innerAttr.equals(innerAttribute)) {
				return utypeDecoder;
			}
		}
		return null;
	}
	
	/**
	 * Return all decoders whose inner attributes are present in the provided array.
	 *
	 * This is useful when a mapping rule references multiple inner attributes
	 * and you need the corresponding decoders grouped together.
	 *
	 * @param innerAttributes array of inner attribute names to match
	 * @return list of matching {@link UtypeDecoder}; empty list when none match
	 */
	public List<UtypeDecoder> getUtypeDecodersMatchingInnerAttributes(String[] innerAttributes) {
		List<UtypeDecoder> innerAttrs = new ArrayList<UtypeDecoder>();
		for( UtypeDecoder utypeDecoder: this.utypeDecoders ) {
			String innerAttr = utypeDecoder.getInnerAttribute();
			// Use Arrays.asList to check membership; avoids N^2 lookups for small arrays
			if( innerAttr != null && Arrays.asList(innerAttributes).contains(innerAttr) ) {
				innerAttrs.add(utypeDecoder);
			}
		}
		return innerAttrs;
	}
	
	/**
	 * Find the first decoder whose hostAttribute equals the provided value.
	 *
	 * @param hostAttribute the host attribute name to search for
	 * @return the matching {@link UtypeDecoder} or null when none match
	 */
	public UtypeDecoder getUtypeDecoderByHostAttribute(String hostAttribute) {
		for( UtypeDecoder utypeDecoder: this.utypeDecoders ) {
			String hostAttr = utypeDecoder.getHostAttribute();
			if( hostAttr != null && hostAttr.equals(hostAttribute)) {
				return utypeDecoder;
			}
		}
		return null;
	}

}