package use_case.predict_passes;

/**
 * Indicates that satellite pass predictions could not be retrieved.
 */
public class PassPredictionException extends Exception {
    private static final long serialVersionUID = 1L;

    public PassPredictionException(String message) {
        super(message);
    }

    public PassPredictionException(String message, Throwable cause) {
        super(message, cause);
    }
}
