package interface_adapter.predict_passes;

import use_case.predict_passes.PredictPassesInputBoundary;
import use_case.predict_passes.PredictPassesInputData;
import use_case.predict_passes.PredictPassesOutputBoundary;

import java.util.Objects;

/**
 * Converts text input from the view into a prediction request.
 */
public final class PredictPassesController {

    private final PredictPassesInputBoundary useCase;
    private final PredictPassesOutputBoundary outputBoundary;

    public PredictPassesController(
            PredictPassesInputBoundary useCase,
            PredictPassesOutputBoundary outputBoundary) {
        this.useCase = Objects.requireNonNull(useCase);
        this.outputBoundary = Objects.requireNonNull(outputBoundary);
    }

    /**
     * Executes a visual-pass query using values from the input fields.
     * The caller must run this off the Swing event dispatch thread,
     * because the use case may perform a network request.
     */
    public void execute(
            String latitudeText,
            String longitudeText,
            String altitudeText,
            String daysText,
            String minElevationText) {

        if (isBlank(latitudeText)
                || isBlank(longitudeText)
                || isBlank(altitudeText)
                || isBlank(daysText)
                || isBlank(minElevationText)) {
            outputBoundary.prepareFailView(
                    "Please fill in all observation fields.");
            return;
        }

        PredictPassesInputData inputData;

        try {
            double latitude =
                    Double.parseDouble(latitudeText.strip());

            double longitude =
                    Double.parseDouble(longitudeText.strip());

            double altitudeMeters =
                    Double.parseDouble(altitudeText.strip());

            int days =
                    Integer.parseInt(daysText.strip());

            double minElevationDeg =
                    Double.parseDouble(minElevationText.strip());

            inputData = new PredictPassesInputData(
                    latitude,
                    longitude,
                    altitudeMeters,
                    days,
                    minElevationDeg,
                    true);
        }
        catch (NumberFormatException exception) {
            outputBoundary.prepareFailView(
                    "Enter numeric values for coordinates, altitude and "
                            + "minimum elevation. Days must be a whole number.");
            return;
        }

        useCase.execute(inputData);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
