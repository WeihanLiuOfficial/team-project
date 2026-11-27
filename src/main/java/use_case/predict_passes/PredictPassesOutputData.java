package use_case.predict_passes;

import entity.SatellitePass;

import java.util.List;
import java.util.Objects;

/**
 * Results of a successful pass prediction request.
 */
public final class PredictPassesOutputData {
    private final List<SatellitePass> passes;

    public PredictPassesOutputData(List<SatellitePass> passes) {
        Objects.requireNonNull(passes, "Pass list must not be null.");
        this.passes = List.copyOf(passes);
    }

    public List<SatellitePass> getPasses() {
        return passes;
    }
}
