package main.annoter.meta;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Central project glossary containing constant values and small lookup maps
 * used throughout the mivot extension.
 *
 * This class groups related constants into nested static classes to make their
 * purpose explicit (URLs, model prefixes, types, coordinate system constants,
 * etc.). Keep this file lightweight: avoid logic here and use it simply as a
 * canonical source of project-wide strings and lists.
 */
public class Glossary {

    /**
     * Service endpoints used by the project. Keep URLs here so they are easy to
     * locate and replace when needed (for tests or environment changes).
     */
    public static class Url {
        /**
         * Service URL(s) that are used by the API
         */
        // Filter Profile Service URL (SVO)
        public static final String FPS = "http://svo2.cab.inta-csic.es/svo/theory/fps/fpsmivot.php?PhotCalID=";
    }

    /**
     * Primitive VODML / IVOA data type constants used when building attributes.
     * Using named constants keeps code readable and avoids scattered string
     * literals across the codebase.
     */
    public static class IvoaType {
        /** Primitive string type */
        public static final String STRING = "ivoa:string";
        /** Primitive real number type */
        public static final String REAL = "ivoa:real";
        /** Quantity wrapper for real numbers */
        public static final String REAL_QUANTITY = "ivoa:RealQuantity";
        /** Boolean type */
        public static final String BOOL = "ivoa:boolean";
        /** Date/time type */
        public static final String DATETIME = "ivoa:datetime";
    }
    
    /**
     * List of supported high-level properties that the mapper knows how to
     * handle. Use this list to iterate available mapping targets.
     */
    public static final List<String> SUPPORTED_PROPERTIES  = Arrays.asList(
            "EpochPosition",
            "Brightness",
            "Color",
            "Label"
        );
      
    /**
     * Well-known role/field lists for various model components. These lists are
     * used for validation, mapping and to enumerate acceptable child roles.
     */
    public static class Roles {
        /** Accepted roles for brightness-related frames */
        public static final List<String> BRIGHTNESS = Arrays.asList(
                "value"
            );
        public static final List<String> BRIGHTNESS_ERROR = Arrays.asList(
                "value"
            );

        /** Expected roles for epoch-position frames (positional attributes) */
        public static final List<String> EPOCH_POSITION = Arrays.asList(
            "longitude",
            "latitude",
            "parallax",
            "radialVelocity",
            "pmLongitude",
            "pmLatitude",
            "obsDate"
        );

        /** Known correlation role names for epoch-position covariance descriptions */
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

        /** Common names used for epoch-position error descriptions */
        public static final List<String> EPOCH_POSITION_ERRORS = Arrays.asList(
            "parallax",
            "radialVelocity",
            "position",
            "properMotion"
        );

        /** Symmetric 1D parameter error role names */
        public static final List<String> PERROR_SYM_1D = Arrays.asList("sigma");

        /** Asymmetric 1D parameter error role names */
        public static final List<String> PERROR_ASYM_1D = Arrays.asList("low", "high");

        /** Symmetric 2D parameter error role names */
        public static final List<String> PERROR_SYM_2D = Arrays.asList("sigma1", "sigma2");

        /** Elliptical error role names (semi-major, semi-minor, angle) */
        public static final List<String> PERROR_ELLIPSE = Arrays.asList("semiMajorAxis", "semiMinorAxis", "angle");

        /** Roles used for photometric property mapping */
        public static final List<String> PHOTOMETRIC_PROPERTY = Arrays.asList("value", "error");

        /** Roles for color properties */
        public static final List<String> COLOR = Arrays.asList("value", "error", "definition");

        /** Roles used to describe query provenance */
        public static final List<String> QUERY_ORIGIN = Arrays.asList(
            "publisher", "server_software", "service_protocol",
            "request", "request_date", "query", "contact", "ivoid"
        );

        /** Roles used to describe data provenance */
        public static final List<String> DATA_ORIGIN = Arrays.asList(
            "ivoid", "reference_url", "resource_version", "creators",
            "cites", "is_derived_from", "original_date", "rights", "rights_uri", "articles"
        );

        /** Article metadata roles */
        public static final List<String> ARTICLE = Arrays.asList("identifier", "editor");
    }

    /**
     * Coordinate system allowed lists. These constants enumerate acceptable
     * values for space/time frames and reference positions; they are used to
     * validate incoming CS qualifiers and to build default frames.
     */
    public static class CoordSystems {
        /** Supported space frames (names used in UCD/CS qualifiers) */
        public static final List<String> SPACE_FRAMES = Arrays.asList(
            "eq_FK4", "FK4", "eq_FK5", "FK5", "ICRS", "GALACTIC", "SUPER_GALACTIC", "ECLIPTIC"
        );

        /** Supported reference locations for frames (origin of coordinates) */
        public static final List<String> REF_POSITIONS = Arrays.asList(
            "BARYCENTER", "GEOCENTER", "TOPOCENTER"
        );

        /** Known time frames */
        public static final List<String> TIME_FRAMES = Arrays.asList(
            "TAI", "TT", "TDT", "ET", "IAT", "UT1",
            "UTC", "GMT", "GPS", "TCG", "TCB", "TBD", "LOCAL"
        );

        /** Accepted time format identifiers used by the mapper */
        public static final List<String> TIME_FORMATS = Arrays.asList(
            "byear", "cxcsec", "decimalyear", "fits",
            "gps", "iso", "timestamp", "jyear", "year", "jd", "mjd"
        );
    }

    /**
     * Standard VODML model prefixes used when building MIVOT fragments. Store
     * prefixes here so they remain consistent across the codebase.
     */
    public static class ModelPrefix {
        public static final String IVOA = "ivoa";
        public static final String MANGO = "mango";
        public static final String PHOT = "Phot";
        public static final String COORDS = "coords";
        public static final String MEAS = "meas";
    }

    /**
     * Canonical URLs for the VODML model documents referenced in generated
     * MIVOT output. These can be replaced with local copies for offline
     * operation or testing environments.
     */
    public static class VodmlUrl {
        public static final String IVOA = "https://www.ivoa.net/xml/VODML/IVOA-v1.vo-dml.xml";
        public static final String MANGO = "https://ivoa.net/xml/MANGO/MANGO-V1.vodml.xml";
        public static final String PHOT = "https://ivoa.net/xml/VODML/Phot-v1.vodml.xml";
        public static final String COORDS = "https://ivoa.net/xml/VODML/Coords-v1.vo-dml.xml";
        public static final String MEAS = "https://ivoa.net/xml/VODML/Meas-v1.vo-dml.xml";
    }

    /**
     * Lists of UCDs used for automatic mapping of EpochPosition attributes.
     * These lists are consulted by the auto-mapper to identify columns likely
     * to correspond to longitude, latitude, proper motions, etc.
     */
    public static class EpochPositionAutoMapping {
        public static final List<String> LONGITUDE = Arrays.asList("POS_EQ_RA_MAIN", "pos.eq.ra;meta.main");
        public static final List<String> LATITUDE = Arrays.asList("POS_EQ_DEC_MAIN", "pos.eq.dec;meta.main");
        public static final List<String> PM_LONGITUDE = Arrays.asList("pos.pm;pos.eq.ra");
        public static final List<String> PM_LATITUDE = Arrays.asList("pos.pm;pos.eq.dec");
        public static final List<String> OBS_DATE = Arrays.asList("time.epoch;obs;stat.mean", "time.epoch;obs");
        public static final List<String> PARALLAX = Arrays.asList("pos.parallax.trig");
        // Single-value UCD for radial velocity (kept as a String for compactness)
        public static final String RADIAL_VELOCITY = "spect.dopplerVeloc.opt";
    }
    
    /**
     * Mapping from user-visible filter abbreviations to SVO identifiers. This
     * map is populated with common filters; an empty string means a band is
     * known but no SVO identifier is available.
     *
     * The map is intentionally a LinkedHashMap to preserve insertion order for
     * deterministic behaviour in logs and tests.
     */
    public static class Filters {
        static public Map<String, String> map = new LinkedHashMap<>();

        static {
            map.put("K", "2MASS/2MASS.Ks/AB");
            map.put("H", "2MASS/2MASS.H/AB");
            map.put("R", "");
            map.put("U", "");
            map.put("J", "2MASS/2MASS.J/AB");
            map.put("V", "");
            map.put("B", "");
            map.put("I", "");
            map.put("u", "SLOAN/SDSS.u/AB");
            map.put("r", "SLOAN/SDSS.r/AB");
            map.put("z", "SLOAN/SDSS.z/AB");
            map.put("g", "SLOAN/SDSS.g/AB");
            map.put("i", "SLOAN/SDSS.i/AB");
            map.put("G", "GAIA/GAIA3.G/AB");
            map.put("F444W", "JWST/NIRCam.F444W/AB");
            map.put("F150W", "JWST/NIRCam.F150W/AB");
            map.put("F200W", "JWST/NIRCam.F200W/AB");
        }
    }
    
    /**
     * Role name used to recognise photometry filter instances in FPS/PhotCal
     * XML responses. Kept as a named constant to avoid magic strings.
     */
    public static String FILTER_ROLE ="Phot:PhotCal.photometryFilter";

    /**
     * Short names used to identify coordinate-system class tokens parsed from
     * UType/CS qualifiers. These constants are referenced by the FrameFactory
     * and mapping code to route frame building.
     */
    public static class CSClass {
        public static final String SPACE = "spaceSys";
        public static final String TIME = "timeSys";
        public static final String PHOTCAL = "photCal";
        public static final String FILTER_HIGH = "photFilterHigh";
        public static final String FILTER_LOW = "photFilterLow";
        public static final String LOCAL = "local";
    }
    public static class CTClass {
        /**
         * Common CT (constant) qualifiers used in bracketed UType qualifiers.
         */
        public static final String EPOCH = "epoch";
        public static final String LOCAL = "local";
        public static final String MODE = "mode";
        public static final String VOCABULARY = "vocabulary";
    }
}