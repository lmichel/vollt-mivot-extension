CREATE SCHEMA IF NOT EXIST hip;
ALTER TABLE hipparcos SET SCHEMA hip;

UPDATE "TAP_SCHEMA".columns SET utype = 'mango:MangoObject.identifier' WHERE columns.column_name = 'hip';
UPDATE "TAP_SCHEMA".columns SET utype = 'mango:EpochPosition.latitude[CS.spaceSys=ICRS]' WHERE columns.column_name = 'ra';
UPDATE "TAP_SCHEMA".columns SET utype = 'mango:EpochPosition.longitude[CS.spaceSys=ICRS]' WHERE columns.column_name = 'dec';
UPDATE "TAP_SCHEMA".columns SET utype = 'mango:EpochPosition.errors.position/mango:error.PErrorSym2D.sigma1[CS.spaceSys=ICRS]' WHERE columns.column_name = 'e_ra';
UPDATE "TAP_SCHEMA".columns SET utype = 'mango:EpochPosition.errors.position/mango:error.PErrorSym2D.sigma2[CS.spaceSys=ICRS]' WHERE columns.column_name = 'e_dec';

UPDATE "TAP_SCHEMA".columns SET utype = 'mango:EpochPosition.pmLatitude[CS.spaceSys=ICRS]' WHERE columns.column_name = 'pmra';
UPDATE "TAP_SCHEMA".columns SET utype = 'mango:EpochPosition.pmLongitude[CS.spaceSys=ICRS]' WHERE columns.column_name = 'pmdec';
UPDATE "TAP_SCHEMA".columns SET utype = 'mango:EpochPosition.errors.properMotion/mango:error.PErrorSym2D.sigma1[CS.spaceSys=ICRS]' WHERE columns.column_name = 'e_pmra';
UPDATE "TAP_SCHEMA".columns SET utype = 'mango:EpochPosition.errors.properMotion/mango:error.PErrorSym2D.sigma2[CS.spaceSys=ICRS]' WHERE columns.column_name = 'e_pmdec';

UPDATE "TAP_SCHEMA".columns SET utype = 'mango:EpochPosition.parallax[CS.spaceSys=ICRS]' WHERE columns.column_name = 'plx';
UPDATE "TAP_SCHEMA".columns SET utype = 'mango:EpochPosition.errors.parallax/mango:error.PErrorSym1D.sigma1[CS.spaceSys=ICRS]' WHERE columns.column_name = 'e_plx';
