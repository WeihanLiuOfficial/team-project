package use_case.predict_passes;

import entity.SatellitePass;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Validates a request and selects suitable upcoming ISS passes.
 */
public final class PredictPassesInteractor
        implements PredictPassesInputBoundary {

    private static final int MIN_DAYS = 1;
    private static final int MAX_DAYS = 7;

    private final PredictPassesDataAccessInterface dataAccess;
    private final PredictPassesOutputBoundary presenter;

    public PredictPassesInteractor(
            PredictPassesDataAccessInterface dataAccess,
            PredictPassesOutputBoundary presenter) {
        this.dataAccess = Objects.requireNonNull(dataAccess);
        this.presenter = Objects.requireNonNull(presenter);
    }

    @Override
    public void execute(PredictPassesInputData inputData) {
        String validationError = validateInput(inputData);

        if (validationError != null) {
            presenter.prepareFailView(validationError);
            return;
        }

        List<SatellitePass> passes;

        try {
            passes = dataAccess.fetchPasses(
                    inputData.getLatitude(),
                    inputData.getLongitude(),
                    inputData.getAltitudeMeters(),
                    inputData.getDays());
        }
        catch (PassPredictionException exception) {
            presenter.prepareFailView(
                    "Unable to retrieve satellite passes. Please try again later.");
            return;
        }

        List<SatellitePass> matchingPasses = new ArrayList<>();

        for (SatellitePass pass : passes) {
            if (pass.isVisibleForObserving(
                    inputData.getMinElevationDeg(),
                    inputData.isExcludeDaylight())) {
                matchingPasses.add(pass);
            }
        }

        matchingPasses.sort(
                Comparator.comparingLong(SatellitePass::getStartTime));

        presenter.prepareSuccessView(
                new PredictPassesOutputData(matchingPasses));
    }

    /**
     * Returns an error message, or null when the input is valid.
     */
    private String validateInput(PredictPassesInputData inputData) {
        if (inputData == null) {
            return "Please provide the observation details.";
        }

        if (!isWithinRange(inputData.getLatitude(), -90.0, 90.0)) {
            return "Latitude must be between -90 and 90 degrees.";
        }

        if (!isWithinRange(inputData.getLongitude(), -180.0, 180.0)) {
            return "Longitude must be between -180 and 180 degrees.";
        }

        if (!Double.isFinite(inputData.getAltitudeMeters())) {
            return "Altitude must be a finite number in meters.";
        }

        if (inputData.getDays() < MIN_DAYS
                || inputData.getDays() > MAX_DAYS) {
            return "Please choose between 1 and 7 days.";
        }

        if (!isWithinRange(inputData.getMinElevationDeg(), 0.0, 90.0)) {
            return "Minimum elevation must be between 0 and 90 degrees.";
        }

        return null;
    }

    private boolean isWithinRange(double value, double min, double max) {
        return Double.isFinite(value) && value >= min && value <= max;
    }
}