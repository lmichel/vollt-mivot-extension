# vollt-mivot-extension

VOLLT extension able to annotate data on the fly

## Annotation activation

- The query response annoter ins invoked if `RESPONSEFORMAT=application/x-votable+xml;content=mivot`
- This Mime type is defined in `main.vollt_tuning.MangotFormat`
   - It embeds a custom formator: `main.vollt_tuning.CustomVOTableFormat`
   - The custom formator inserts the annotations in between the header write out and the data write out. 
- This class is declared in `tap.property` that way: `output_formats={main.vollt_tuning.MangoFormat}, fits, csv, tsv, text, html, json`

## Model mapped

- The current implementation is based on MANGO
- Only the EpochPositib class is mapped (without correlations)
- All parameters are expressed in  ICRS (hard-coded)
- All parameters are normalized at the year 2000.0 epoch (hard-coded) 

## Annotation Build

The VOTable formator check the parsed query to see if some selected columns ca be annotated.
- If so, it builds a MIVOT block based on these columns
- The annotation block is an XML string which is nserted in the output stream.

## Packages Structure

- `dev`: some utilities to play with the logic out of any DB context
- `main.volt_tuning`: Contain the custom classes that allows to connect the annotation feature with VOLLT.
- `main.annoter`: root package for the logic.
- `main.annoter.dm`: classes modeling the MANGO components
- `main.annoter.mivot`: classes handling the construction of the MIVOT block
- `main.annoter.meta`: classes handling the mapping rules stored in tne TAP Schema as pseudo Utypes.
- `main.annoter.utils`: SOme utilities

