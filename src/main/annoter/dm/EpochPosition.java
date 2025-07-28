/**
 * Commented by chatGPT 
 */
package main.annoter.dm;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import main.annoter.meta.MappingCache;
import main.annoter.meta.UtypeDecoder;
import main.annoter.mivot.MappingError;
import main.annoter.mivot.MivotInstance;
import tap.metadata.TAPColumn;

/**
 * Représente une position d'époque astronomique (EpochPosition)
 * avec ses paramètres (latitude, longitude, mouvements propres, etc.)
 * ainsi que les éventuelles erreurs associées.
 */
public class EpochPosition extends Property {

	// Type de données MANGO
	public static final String DMTYPE = "mango:EpochPosition";


	// Liste des frames de coordonnées spatiales utilisées
	public List<String> frames;

	/**
	 * Constructeur principal de l'objet EpochPosition.
	 *
	 * @param mappingCache     Le cache de mappage entre colonnes et Utypes
	 * @param tableName        Le nom de la table TAP
	 * @param selectedColumns  Liste des colonnes sélectionnées
	 * @throws MappingError    En cas d'erreur dans le mappage
	 */
	@SuppressWarnings("serial")
	public EpochPosition(MappingCache mappingCache, String tableName, List<String> selectedColumns)
			throws MappingError {

		// Appel au constructeur de la classe mère Property
		super(DMTYPE, null, null, new HashMap<String, String>() {
			{
			put("description", "6 parameters position");
			put("uri", "https://www.ivoa.net/rdf/uat/2024-06-25/uat.html#astronomical-location");
			put("label", "Astronomical location");
			}
		});

		// Récupère les colonnes mappables de la table pour le DMTYPE
		List<UtypeDecoder> mappableColumns = mappingCache.getTableMapping(tableName, DMTYPE);
		String spaceSys = null;

		// Parcours les colonnes et ajoute les attributs reconnus
		for (UtypeDecoder mappableColumn : mappableColumns) {
			String attribute = mappableColumn.getHostAttribute();
			TAPColumn tapColumn = mappableColumn.getTapColumn();
			String adqlName = tapColumn.getADQLName();
			this.frames = mappableColumn.getFrames();

			// Vérifie que l'attribut est reconnu et que la colonne a été sélectionnée
			if (Glossary.Roles.EPOCH_POSITION.contains(attribute) && selectedColumns.contains(adqlName)) {
				this.addAttribute("ivoa:RealQuantity", DMTYPE + "." + attribute, adqlName, tapColumn.getUnit());

				// Recherche la frame spatiale (spaceSys)
				for (String frame : this.frames) {
					if (frame.startsWith("spaceSys")) {
						spaceSys = frame.replace("spaceSys=", "_spaceframe_") + "_BARYCENTER";
					}
				}
			}
		}

		MivotInstance obsDate = new MivotInstance("mango:ObsDate", "mango:EpochPosition.obsDate", null);
		obsDate.addAttribute("ivoa:string","mango:DateTime.representation", "*year", null);
		obsDate.addAttribute("ivoa:datetime","mango:DateTime.dateTime", 2000.0, null);
		this.addInstance(obsDate);
		// Ajoute les éventuelles erreurs associées à l'époque
		MivotInstance erri = this.buildEpochErrors(mappingCache, tableName, selectedColumns);
		if (erri != null) {
			this.addInstance(erri);
		}

		// Ajoute une référence à la frame spatiale si trouvée
		if (spaceSys != null) {
			this.addReference(DMTYPE + ".spaceSys", spaceSys);
		}
	}

	/**
	 * Construit les erreurs associées à la position d'époque.
	 *
	 * @return MivotInstance représentant les erreurs, ou null si aucune erreur n'est mappée.
	 */
	private MivotInstance buildEpochErrors(MappingCache mappingCache, String tableName, List<String> selectedColumns)
			throws MappingError {

		List<UtypeDecoder> mappableColumns = mappingCache.getTableMapping(tableName, DMTYPE);
		List<UtypeDecoder> positionUtypes = new ArrayList<>();
		List<UtypeDecoder> pmUtypes = new ArrayList<>();

		// Filtrage des erreurs de type position et properMotion
		for (UtypeDecoder mappableColumn : mappableColumns) {
			String attribute = mappableColumn.getHostAttribute();
			TAPColumn tapColumn = mappableColumn.getTapColumn();
			String adqlName = tapColumn.getADQLName();

			if (attribute.equals("errors") && selectedColumns.contains(adqlName)) {
				if ("position".equals(mappableColumn.getInnerRole())) {
					positionUtypes.add(mappableColumn);
				}
				if ("properMotion".equals(mappableColumn.getInnerRole())) {
					pmUtypes.add(mappableColumn);
				}
			}
		}

		// Création de l'instance principale d'erreur
		MivotInstance errorInstance = new MivotInstance(
			positionUtypes.isEmpty() ? pmUtypes.get(0).getInnerClass() : positionUtypes.get(0).getInnerClass(),
			DMTYPE + ".errors",
			null
		);

		boolean mapped = false;

		// Ajoute les erreurs de position si présentes
		if (!positionUtypes.isEmpty()) {
			MivotInstance positionError = this.buildFlatInstance("mango:EpochPositionErrors.position", positionUtypes);
			errorInstance.addInstance(positionError);
			mapped = true;
		}

		// Ajoute les erreurs de mouvement propre si présentes
		if (!pmUtypes.isEmpty()) {
			MivotInstance pmError = this.buildFlatInstance("mango:EpochPositionErrors.properMotion", pmUtypes);
			errorInstance.addInstance(pmError);
			mapped = true;
		}

		return mapped ? errorInstance : null;
	}

	/**
	 * Construit une instance "plate" de type MivotInstance à partir d'une liste d'Utypes.
	 */
	private MivotInstance buildFlatInstance(String role, List<UtypeDecoder> utypeList) throws MappingError {
		MivotInstance flatInstance = new MivotInstance(utypeList.get(0).getInnerClass(), role, null);

		// Ajoute chaque attribut à l'instance plate
		for (UtypeDecoder mappableColumn : utypeList) {
			flatInstance.addAttribute(
				"ivoa:RealQuantity",
				utypeList.get(0).getInnerClass() + "." + mappableColumn.getInnerAttribute(),
				mappableColumn.getTapColumn().getADQLName(),
				mappableColumn.getTapColumn().getUnit()
			);
		}
		return flatInstance;
	}
}
