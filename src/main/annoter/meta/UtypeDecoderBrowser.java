package main.annoter.meta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UtypeDecoderBrowser {
	public List<UtypeDecoder> utypeDecoders;
	
	public UtypeDecoderBrowser(List<UtypeDecoder> utypeDecoders) {
		this.utypeDecoders = utypeDecoders;
	}

	public UtypeDecoder getUtypeDecoderByInnerAttribute(String innerAttribute) {
		for( UtypeDecoder utypeDecoder: this.utypeDecoders ) {
			String innerAttr = utypeDecoder.getInnerAttribute();
			if( innerAttr != null && innerAttr.equals(innerAttribute)) {
				return utypeDecoder;
			}
		}
		return null;
	}
	
	public List<UtypeDecoder> getUtypeDecodersMatchingInnerAttributes(String[] innerAttributes) {
		List<UtypeDecoder> innerAttrs = new ArrayList<UtypeDecoder>();
		for( UtypeDecoder utypeDecoder: this.utypeDecoders ) {
			String innerAttr = utypeDecoder.getInnerAttribute();
			if( innerAttr != null && Arrays.asList(innerAttributes).contains(innerAttr) ) {
				innerAttrs.add(utypeDecoder);
			}
		}
		return innerAttrs;
	}
	
	public UtypeDecoder getUtypeDecoderByHostAttribute(String hostAttribute) {
		for( UtypeDecoder utypeDecoder: this.utypeDecoders ) {
			String hostAttr = utypeDecoder.getHostAttribute();
			if( hostAttr != null && hostAttr.equals(hostAttribute)) {
				return utypeDecoder;
			}
		}
		return null;
	}
	
	public List<UtypeDecoder> getUtypeDecodersMatchingHostAttributes(List<String> hostAttributes) {
		List<UtypeDecoder> hostAttrs = new ArrayList<UtypeDecoder>();
		for( UtypeDecoder utypeDecoder: this.utypeDecoders ) {
			String hostAttr = utypeDecoder.getInnerAttribute();
			if( hostAttr != null && hostAttributes.contains(hostAttr) ) {
				hostAttrs.add(utypeDecoder);
			}
		}
		return hostAttrs;
	}

}
