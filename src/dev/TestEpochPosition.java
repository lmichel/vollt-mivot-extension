package dev;

import java.util.ArrayList;
import java.util.List;

import main.annoter.dm.Property;
import main.annoter.meta.UtypeDecoder;
import main.annoter.mivot.FrameHolder;
import tap.metadata.TAPColumn;

public class TestEpochPosition {

	/**
	 * Build a Property for each epoch-like string in the test set and print
	 * the XML representation. This shows how different epoch tokens are
	 * handled by the UtypeDecoder and Property factory.
	 */
	public static void testObsDate() throws Exception {

		// List of known/placeholder frame holders (empty here for the test).
		List<FrameHolder> frameHolders = new ArrayList<>();

		// Test inputs: a set of epoch-like strings to embed into the utype.
		// - "J2000" and "B1234" are common epoch labels,
		// - "2000.0" is a numeric epoch representation,
		// - "turlututu" is an intentionally invalid token to exercise
		//   decoder handling of unexpected values.
		String[] strings = {"J2000", "B1234", "2000.0", "turlututu"};

		for (String year : strings) {
			// For each token build a TAPColumn whose utype contains an
			// EpochPosition component with CT.epoch equal to the token.
			// The variable 'year' is concatenated directly into the utype string.


			UtypeDecoder decoder = new UtypeDecoder(
					new TAPColumn("ra", "description", "deg", "ucd",
						"mango:EpochPosition.longitude[CS.spaceSys=ICRS CT.epoch=" + year +"]"
						)
					);

			// Collect any constant tokens produced by the decoder (if any).
			List<String> constants = new ArrayList<String>();
			List<UtypeDecoder> utds = new ArrayList<UtypeDecoder>();
			utds.add(decoder);

			// Copy constants from the first (and only) decoder into the list
			// passed to Property.getInstance. This mirrors how real code would
			// collect decoder-derived constants for property construction.
			for (String ct : utds.get(0).getConstants()) {
				constants.add(ct);
			}

			// Create the Property using the factory method. The resulting
			// Property encapsulates details extracted from the utype and
			// can produce an XML representation via xmlString().
			Property property = (Property) Property.getInstance("EpochPosition", utds,
					frameHolders, constants);
			System.out.println(property.xmlString());
		}
	}
	
	public static void main(String[] args) throws Exception {
		testObsDate();
	}
}