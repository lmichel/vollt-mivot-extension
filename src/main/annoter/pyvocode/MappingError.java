package main.annoter.pyvocode;

/**
 * Exception raised if a Resource or MIVOT element can't be mapped for one of these reasons:
 * - It doesn't match with any expected Resource/MIVOT element.
 * - It matches with too many Resource/MIVOT elements than expected.
 *
 * This exception is intended to be caught by the viewer so that processing can continue
 * by ignoring the annotations.
 */
public class MappingError extends Exception {

    public MappingError() {
        super();
    }

    public MappingError(String message) {
        super(message);
    }

    public MappingError(String message, Throwable cause) {
        super(message, cause);
    }

    public MappingError(Throwable cause) {
        super(cause);
    }
}
