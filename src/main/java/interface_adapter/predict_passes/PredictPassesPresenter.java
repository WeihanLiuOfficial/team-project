package interface_adapter.predict_passes;

import entity.SatellitePass;
import use_case.predict_passes.PredictPassesOutputBoundary;
import use_case.predict_passes.PredictPassesOutputData;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Formats prediction results for display.
 */
public final class PredictPassesPresenter
        implements PredictPassesOutputBoundary {

    private final PredictPassesViewModel viewModel;
    private final DateTimeFormatter timeFormatter;

    public PredictPassesPresenter(
            PredictPassesViewModel viewModel,
            ZoneId displayZone) {
        this.viewModel = Objects.requireNonNull(viewModel);

        this.timeFormatter = DateTimeFormatter
                .ofPattern("uuuu-MM-dd HH:mm:ss XXX", Locale.ROOT)
                .withZone(Objects.requireNonNull(displayZone));
    }

    @Override
    public void prepareSuccessView(PredictPassesOutputData outputData) {
        List<List<String>> rows = new ArrayList<>();

        try {
            for (SatellitePass pass : outputData.getPasses()) {
                int seconds = pass.getDurationSeconds();

                rows.add(List.of(
                        pass.getSatelliteName(),
                        formatTime(pass.getStartTime()),
                        formatTime(pass.getEndTime()),
                        String.format(
                                Locale.ROOT,
                                "%d min %02d s",
                                seconds / 60,
                                seconds % 60),
                        formatAngle(pass.getMaxElevationDeg()),
                        formatAngle(pass.getStartAzimuth()),
                        formatAngle(pass.getEndAzimuth())));
            }
        }
        catch (DateTimeException exception) {
            prepareFailView(
                    "The prediction contains a time that cannot be displayed.");
            return;
        }

        String message;

        if (rows.isEmpty()) {
            message = "No visible ISS passes matched your search.";
        }
        else {
            message = "Found " + rows.size() + " matching visual passes.";
        }

        viewModel.update(rows, message, false);
    }

    @Override
    public void prepareFailView(String errorMessage) {
        viewModel.update(List.of(), errorMessage, true);
    }

    private String formatTime(long unixSeconds) {
        return timeFormatter.format(
                Instant.ofEpochSecond(unixSeconds));
    }

    private String formatAngle(double degrees) {
        return String.format(Locale.ROOT, "%.2f°", degrees);
    }
}
