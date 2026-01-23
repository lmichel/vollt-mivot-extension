/**
 * Commented by chatGPT 
 */
package main.annoter.dm;


import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import main.annoter.cache.MappingCache;
import main.annoter.meta.Glossary;
import main.annoter.meta.UtypeDecoder;
import main.annoter.meta.UtypeDecoderBrowser;
import main.annoter.mivot.FrameHolder;
import main.annoter.mivot.MappingError;
import main.annoter.mivot.MivotInstance;
import tap.metadata.TAPColumn;

public class Label extends Property {

	// Type de données MANGO
	public static final String DMTYPE = "mango:Label";


	public UtypeDecoderBrowser utypeBrowser;
	public UtypeDecoder textUtypeDecoder = null; 
	public MivotInstance errorInstance = null;
	
	public Label(List<UtypeDecoder> utypeDecoders,
			String tableName,
			List<FrameHolder> frameHolders,
			List<String> constants) throws Exception {

		super(DMTYPE, null, null);
		
		this.utypeBrowser = new UtypeDecoderBrowser(utypeDecoders);
		
		this.textUtypeDecoder = this.utypeBrowser.getUtypeDecoderByHostAttribute("text");
		String vocab = this.textUtypeDecoder.getConstant(Glossary.CTClass.VOCABULARY);
		String label = null;
		Map<String, String> semantics = new LinkedHashMap<String, String>();
		if( vocab != null && vocab.length() > 0 ) {
			semantics.put("uri", vocab)	;
			String[] uri = vocab.split("#");
			if( uri.length == 2) {
				label = uri[1];
			}
		}
		if( label != null && label.length() > 0 ) {
			semantics.put("label", label)	;	
		}
		String description = this.textUtypeDecoder.getTapColumn().getDescription();
		if( description != null && description.length() > 0 ) {
			semantics.put("description", description)	;	
		}
		
		this.setSemantics(semantics);

		if( this.textUtypeDecoder != null ) {
			TAPColumn tapColumn = this.textUtypeDecoder.getTapColumn();
			if( this.textUtypeDecoder != null ) {
				this.addAttribute("ivoa:string",
						DMTYPE + "." + this.textUtypeDecoder.getHostAttribute(), 
						tapColumn.getADQLName(),
						tapColumn.getUnit());
			}
		}
				
		for (FrameHolder fh : frameHolders) {
			
			if (fh.systemClass.equals(Glossary.CTClass.VOCABULARY)) {
				System.out.println("}}}}}}}}}}}}}}}}}}}" + fh);
			}
		}
	}

}

