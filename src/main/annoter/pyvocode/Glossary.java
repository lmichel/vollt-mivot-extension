package main.annoter.pyvocode;
import java.util.Arrays;
import java.util.List;

public class Glossary {

    public static class Url {
        /**
         * Service URL(s) that are used by the API
         */
        // Filter Profile Service URL (SVO)
        public static final String FPS = "http://svo2.cab.inta-csic.es/svo/theory/fps/fpsmivot.php?PhotCalID=";
    }

    public static class IvoaType {
        /**
         * Primitive VODML types
         */
        public static final String STRING = "ivoa:string";
        public static final String REAL = "ivoa:real";
        public static final String REAL_QUANTITY = "ivoa:RealQuantity";
        public static final String BOOL = "ivoa:boolean";
        public static final String DATETIME = "ivoa:datetime";
    }

    public static class Roles {
        /**
         * Accepted roles for all implemented classes
         */
        public static final List<String> EPOCH_POSITION = Arrays.asList(
            "longitude",
            "latitude",
            "parallax",
            "radialVelocity",
            "pmLongitude",
            "pmLatitude",
            "obsDate"
        );

        public static final List<String> EPOCH_POSITION_CORRELATIONS = Arrays.asList(
            "longitudeParallax",
            "latitudeParallax",
            "pmLongitudeParallax",
            "pmLatitudeParallax",
            "longitudeLatitude",
            "pmLongitudePmLatitude",
            "latitudePmLatitude",
            "latitudePmLongitude",
            "longitudePmLatitude",
            "longitudePmLongitude",
            "isCovariance"
        );

        public static final List<String> EPOCH_POSITION_ERRORS = Arrays.asList(
            "parallax",
            "radialVelocity",
            "position",
            "properMotion"
        );

        public static final List<String> PERROR_SYM_1D = Arrays.asList("sigma");

        public static final List<String> PERROR_ASYM_1D = Arrays.asList("low", "high");

        public static final List<String> PERROR_SYM_2D = Arrays.asList("sigma1", "sigma2");

        public static final List<String> PERROR_ELLIPSE = Arrays.asList("semiMajorAxis", "semiMinorAxis", "angle");

        public static final List<String> PHOTOMETRIC_PROPERTY = Arrays.asList("value", "error");

        public static final List<String> COLOR = Arrays.asList("value", "error", "definition");

        public static final List<String> QUERY_ORIGIN = Arrays.asList(
            "publisher", "server_software", "service_protocol",
            "request", "request_date", "query", "contact", "ivoid"
        );

        public static final List<String> DATA_ORIGIN = Arrays.asList(
            "ivoid", "reference_url", "resource_version", "creators",
            "cites", "is_derived_from", "original_date", "rights", "rights_uri", "articles"
        );

        public static final List<String> ARTICLE = Arrays.asList("identifier", "editor");
    }

    public static class CoordSystems {
        /**
         * Supported values for the coordinate system parameters (space and time)
         */
        public static final List<String> SPACE_FRAMES = Arrays.asList(
            "eq_FK4", "FK4", "eq_FK5", "FK5", "ICRS", "GALACTIC", "SUPER_GALACTIC", "ECLIPTIC"
        );

        public static final List<String> REF_POSITIONS = Arrays.asList(
            "BARYCENTER", "GEOCENTER", "TOPOCENTER"
        );

        public static final List<String> TIME_FRAMES = Arrays.asList(
            "TAI", "TT", "TDT", "ET", "IAT", "UT1",
            "UTC", "GMT", "GPS", "TCG", "TCB", "TBD", "LOCAL"
        );

        public static final List<String> TIME_FORMATS = Arrays.asList(
            "byear", "cxcsec", "decimalyear", "fits",
            "gps", "iso", "timestamp", "jyear", "year", "jd", "mjd"
        );
    }

    public static class ModelPrefix {
        /**
         * Model names as defined in VODML
         */
        public static final String IVOA = "ivoa";
        public static final String MANGO = "mango";
        public static final String PHOT = "Phot";
        public static final String COORDS = "coords";
        public static final String MEAS = "meas";
    }

    public static class VodmlUrl {
        /**
         * VODML URLs of the supported models.
         */
        public static final String IVOA = "https://www.ivoa.net/xml/VODML/IVOA-v1.vo-dml.xml";
        public static final String MANGO = "https://ivoa.net/xml/MANGO/MANGO-V1.vodml.xml";
        public static final String PHOT = "https://ivoa.net/xml/VODML/Phot-v1.vodml.xml";
        public static final String COORDS = "https://ivoa.net/xml/VODML/Coords-v1.vo-dml.xml";
        public static final String MEAS = "https://ivoa.net/xml/VODML/Meas-v1.vo-dml.xml";
    }

    public static class EpochPositionAutoMapping {
        /**
         * Expected UCDs for identifying FIELD to be mapped to EpochPosition attributes.
         */
        public static final List<String> LONGITUDE = Arrays.asList("POS_EQ_RA_MAIN", "pos.eq.ra;meta.main");
        public static final List<String> LATITUDE = Arrays.asList("POS_EQ_DEC_MAIN", "pos.eq.dec;meta.main");
        public static final List<String> PM_LONGITUDE = Arrays.asList("pos.pm;pos.eq.ra");
        public static final List<String> PM_LATITUDE = Arrays.asList("pos.pm;pos.eq.dec");
        public static final List<String> OBS_DATE = Arrays.asList("time.epoch;obs;stat.mean", "time.epoch;obs");
        public static final List<String> PARALLAX = Arrays.asList("pos.parallax.trig");
        public static final String RADIAL_VELOCITY = "spect.dopplerVeloc.opt";  // single string, not list
    }
}

