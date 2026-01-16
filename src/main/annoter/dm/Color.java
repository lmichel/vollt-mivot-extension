/**
 * Commented by chatGPT 
 */
package main.annoter.dm;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import main.annoter.meta.Glossary;
import main.annoter.cache.MappingCache;
import main.annoter.meta.UtypeDecoder;
import main.annoter.meta.UtypeDecoderBrowser;
import main.annoter.mivot.FrameHolder;
import main.annoter.mivot.MappingError;
import main.annoter.mivot.MivotInstance;
import tap.metadata.TAPColumn;

public class Color extends Property {

	// Type de données MANGO
	public static final String DMTYPE = "mango:Color";


	public String photcal;
	public UtypeDecoderBrowser utypeBrowser;
	public UtypeDecoder valueUtypeDecoder = null; 
	public MivotInstance errorInstance = null;
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
		this.utypeBrowser = new UtypeDecoderBrowser(utypeDecoders);
		String mode = null;
		for( String constant : constants) {
			String[] parts = constant.split("=");
			String constantClass = parts[0];
			String value = parts[1];
			if( constantClass.equals("mode")) {
				mode = "*" + value;
				break;
			}
		}
		this.valueUtypeDecoder = this.utypeBrowser.getUtypeDecoderByHostAttribute("value");
		if( this.valueUtypeDecoder != null ) {
			this.photcal = this.valueUtypeDecoder.getFrame("photcal");
			TAPColumn tapColumn = this.valueUtypeDecoder.getTapColumn();
			if( this.valueUtypeDecoder != null ) {
				this.addAttribute("ivoa:RealQuantity",
						DMTYPE + "." + this.valueUtypeDecoder.getHostAttribute(), 
						tapColumn.getADQLName(),
						tapColumn.getUnit());
			}

		}
		this.buildErrorInstance();

		if( this.errorInstance != null ) {
			this.addInstance(this.errorInstance);
		}
		MivotInstance colorDef = new MivotInstance("mango:ColorDefinition", DMTYPE + ".colorDef", null);
		colorDef.addAttribute("mango.ColorDefinition", "mango:ColorDef.definition", mode, null);
		for (FrameHolder fh : frameHolders) {
			if( fh.systemClass.equals(Glossary.CSClass.FILTER_HIGH) ) {
				colorDef.addReference("mango:ColorDefinition.high", fh.frameId);
			}
			else if( fh.systemClass.equals(Glossary.CSClass.FILTER_LOW) ) {
				colorDef.addReference("mango:ColorDefinition.low", fh.frameId);
			}
		}
		this.addInstance(colorDef);
	}

	private void buildErrorInstance() throws Exception {
		UtypeDecoder errorUtypeDecoder = this.utypeBrowser.getUtypeDecoderByInnerAttribute("sigma");
		if( errorUtypeDecoder != null ) {
			PropertyError errorFlat = new PropertyError(
					"mango:Brightness.error", 
					null,
					0.68,

					Arrays.asList(errorUtypeDecoder),
					null);
			this.errorInstance = errorFlat.getMivotInstance();
			if( this.photcal == null ) {
				this.photcal = errorUtypeDecoder.getFrame("photcal");
			}

		} else {
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
	public String getPhotCalID() {
		return this.photcal;
	}
}