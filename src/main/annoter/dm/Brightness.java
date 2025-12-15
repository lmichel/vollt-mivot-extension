/**
 * Commented by chatGPT 
 */
package main.annoter.dm;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import main.annoter.meta.MappingCache;
import main.annoter.meta.UtypeDecoder;
import main.annoter.meta.UtypeDecoderBrowser;
import main.annoter.mivot.MappingError;
import main.annoter.mivot.MivotInstance;
import tap.metadata.TAPColumn;

/**
 * Représente une position d'époque astronomique (EpochPosition)
 * avec ses paramètres (latitude, longitude, mouvements propres, etc.)
 * ainsi que les éventuelles erreurs associées.
 */
public class Brightness extends Property {

	// Type de données MANGO
	public static final String DMTYPE = "mango:Brightness";


	public String photcal;
	public UtypeDecoderBrowser utypeBrowser;
	public UtypeDecoder valueUtypeDecoder = null; 
	public MivotInstance errorInstance = null;
	
	@SuppressWarnings("serial")
	public Brightness(List<UtypeDecoder> utypeDecoders, String tableName, List<FrameHolder> frameHolders)
			throws Exception {

		super(DMTYPE, null, null, new HashMap<String, String>() {
			{
			put("description", "magnitude value with its photometric system");
			put("uri", "https://www.ivoa.net/rdf/uat/uat.html#magnitude");
			put("label", "magnitude");
			}
		});
		this.utypeBrowser = new UtypeDecoderBrowser(utypeDecoders);
		
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
		
		for (FrameHolder fh : frameHolders) {
			if (fh.systemClass.equals(Glossary.CSClass.PHOTCAL)) {
				this.addReference(DMTYPE + ".photCal", fh.frameId);
			}
		}
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

