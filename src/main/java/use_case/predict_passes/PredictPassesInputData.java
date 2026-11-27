package use_case.predict_passes;

/**
 * Input values for requesting upcoming ISS passes.
 */
public final class PredictPassesInputData {
    private final double latitude;
    private final double longitude;
    private final double altitudeMeters;
    private final int days;
    private final double minElevationDeg;
    private final boolean excludeDaylight;

    public PredictPassesInputData(double latitude,
                                  double longitude,
                                  double altitudeMeters,
                                  int days,
                                  double minElevationDeg,
                                  boolean excludeDaylight) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitudeMeters = altitudeMeters;
        this.days = days;
        this.minElevationDeg = minElevationDeg;
        this.excludeDaylight = excludeDaylight;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAltitudeMeters() {
        return altitudeMeters;
    }

    public int getDays() {
        return days;
    }

    public double getMinElevationDeg() {
        return minElevationDeg;
    }

    public boolean isExcludeDaylight() {
        return excludeDaylight;
    }
}
