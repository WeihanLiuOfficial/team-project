package use_case.predict_passes;

/**
 * Receives the outcome of the predict-passes use case.
 */
public interface PredictPassesOutputBoundary {

    void prepareSuccessView(PredictPassesOutputData outputData);

    void prepareFailView(String errorMessage);
}
