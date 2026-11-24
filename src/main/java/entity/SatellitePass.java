package entity;

import java.util.Objects;

/**
 * Immutable domain entity representing a predicted overhead satellite pass.
 *
 * <p>Enforces core orbital-pass invariants (positive NORAD ID, non-blank satellite
 * name, non-negative ordered Unix UTC timestamps, non-negative duration, elevation
 * in {@code [0, 90]} degrees, and azimuth angles in {@code [0, 360]} degrees).
 */
public final class SatellitePass {

    /** Default NORAD catalog ID for the International Space Station (ISS). */
    public static final int ISS_NORAD_ID = 25544;

    /** Minimum valid elevation angle above the horizon, in degrees. */
    public static final double MIN_ELEVATION_DEG = 0.0;

    /** Maximum valid elevation angle (zenith), in degrees. */
    public static final double MAX_ELEVATION_DEG = 90.0;

    /** Minimum valid compass azimuth angle, in degrees. */
    public static final double MIN_AZIMUTH_DEG = 0.0;

    /** Maximum valid compass azimuth angle, in degrees. */
    public static final double MAX_AZIMUTH_DEG = 360.0;

    private static final int MIN_SATELLITE_ID = 1;
    private static final long MIN_UNIX_TIME = 0L;
    private static final int MIN_DURATION_SECONDS = 0;

    private final int satelliteId;
    private final String satelliteName;
    private final long startTime;
    private final long endTime;
    private final int durationSeconds;
    private final double maxElevationDeg;
    private final double startAzimuth;
    private final double maxAzimuth;
    private final double endAzimuth;
    private final boolean daylight;

    /**
     * Constructs a validated {@code SatellitePass} entity.
     *
     * @param satelliteId positive NORAD catalog ID of the satellite (e.g., 25544 for ISS)
     * @param satelliteName non-blank human-readable name of the satellite
     * @param startTime pass start timestamp in Unix UTC seconds (non-negative)
     * @param endTime pass end timestamp in Unix UTC seconds (at least {@code startTime})
     * @param durationSeconds visible pass duration in seconds (non-negative)
     * @param maxElevationDeg peak elevation angle above the horizon in degrees {@code [0, 90]}
     * @param startAzimuth start compass azimuth in degrees {@code [0, 360]}
     * @param maxAzimuth azimuth at maximum elevation in degrees {@code [0, 360]}
     * @param endAzimuth end compass azimuth in degrees {@code [0, 360]}
     * @param daylight {@code true} if the pass occurs during daylight or low-contrast
     *     conditions
     * @throws IllegalArgumentException if any domain invariant is violated
     */
    public SatellitePass(int satelliteId,
                         String satelliteName,
                         long startTime,
                         long endTime,
                         int durationSeconds,
                         double maxElevationDeg,
                         double startAzimuth,
                         double maxAzimuth,
                         double endAzimuth,
                         boolean daylight) {
        if (satelliteId < MIN_SATELLITE_ID) {
            throw new IllegalArgumentException(
                    "Satellite NORAD ID must be positive, got: " + satelliteId);
        }
        if (satelliteName == null || satelliteName.trim().isEmpty()) {
            throw new IllegalArgumentException("Satellite name must not be null or blank.");
        }
        if (startTime < MIN_UNIX_TIME) {
            throw new IllegalArgumentException(
                    "Start time must be a non-negative Unix UTC timestamp, got: " + startTime);
        }
        if (endTime < startTime) {
            throw new IllegalArgumentException(
                    "End time (" + endTime + ") must be greater than or equal to start time ("
                            + startTime + ").");
        }
        if (durationSeconds < MIN_DURATION_SECONDS) {
            throw new IllegalArgumentException(
                    "Duration must be non-negative, got: " + durationSeconds);
        }
        validateAngle(maxElevationDeg, MIN_ELEVATION_DEG, MAX_ELEVATION_DEG, "Max elevation");
        validateAngle(startAzimuth, MIN_AZIMUTH_DEG, MAX_AZIMUTH_DEG, "Start azimuth");
        validateAngle(maxAzimuth, MIN_AZIMUTH_DEG, MAX_AZIMUTH_DEG, "Max azimuth");
        validateAngle(endAzimuth, MIN_AZIMUTH_DEG, MAX_AZIMUTH_DEG, "End azimuth");

        this.satelliteId = satelliteId;
        this.satelliteName = satelliteName.trim();
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationSeconds = durationSeconds;
        this.maxElevationDeg = maxElevationDeg;
        this.startAzimuth = startAzimuth;
        this.maxAzimuth = maxAzimuth;
        this.endAzimuth = endAzimuth;
        this.daylight = daylight;
    }

    /**
     * Returns the satellite's NORAD catalog ID.
     *
     * @return the NORAD catalog ID
     */
    public int getSatelliteId() {
        return satelliteId;
    }

    /**
     * Returns the satellite's name.
     *
     * @return the trimmed satellite name
     */
    public String getSatelliteName() {
        return satelliteName;
    }

    /**
     * Returns the pass start timestamp in Unix UTC seconds.
     *
     * @return the start time in Unix UTC seconds
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Returns the pass end timestamp in Unix UTC seconds.
     *
     * @return the end time in Unix UTC seconds
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Returns the visible duration of the pass in seconds.
     *
     * @return the duration in seconds
     */
    public int getDurationSeconds() {
        return durationSeconds;
    }

    /**
     * Returns the peak elevation angle above the horizon in degrees.
     *
     * @return the maximum elevation angle in degrees
     */
    public double getMaxElevationDeg() {
        return maxElevationDeg;
    }

    /**
     * Returns the starting compass azimuth angle in degrees.
     *
     * @return the start azimuth in degrees
     */
    public double getStartAzimuth() {
        return startAzimuth;
    }

    /**
     * Returns the compass azimuth angle at peak elevation in degrees.
     *
     * @return the azimuth at maximum elevation in degrees
     */
    public double getMaxAzimuth() {
        return maxAzimuth;
    }

    /**
     * Returns the ending compass azimuth angle in degrees.
     *
     * @return the end azimuth in degrees
     */
    public double getEndAzimuth() {
        return endAzimuth;
    }

    /**
     * Returns whether this pass occurs during daylight or low-contrast sky conditions.
     *
     * @return {@code true} if the pass is a daylight pass; {@code false} otherwise
     */
    public boolean isDaylight() {
        return daylight;
    }

    /**
     * Checks whether this pass reaches at least the given minimum elevation cutoff.
     *
     * @param minElevationDeg minimum elevation threshold in degrees {@code [0, 90]}
     * @return {@code true} if {@code maxElevationDeg >= minElevationDeg}
     * @throws IllegalArgumentException if {@code minElevationDeg} is outside {@code [0, 90]}
     */
    public boolean isAboveElevation(double minElevationDeg) {
        validateAngle(minElevationDeg, MIN_ELEVATION_DEG, MAX_ELEVATION_DEG,
                "Minimum elevation cutoff");
        return this.maxElevationDeg >= minElevationDeg;
    }

    /**
     * Checks whether this pass satisfies both the minimum elevation cutoff and optional
     * daylight filtering rules for astronomical observing.
     *
     * @param minElevationDeg minimum elevation threshold in degrees {@code [0, 90]}
     * @param excludeDaylight {@code true} to reject daylight/low-contrast passes
     * @return {@code true} if the pass meets the visibility criteria
     * @throws IllegalArgumentException if {@code minElevationDeg} is outside {@code [0, 90]}
     */
    public boolean isVisibleForObserving(double minElevationDeg, boolean excludeDaylight) {
        return isAboveElevation(minElevationDeg) && (!excludeDaylight || !this.daylight);
    }

    private static void validateAngle(double value, double min, double max, String fieldName) {
        if (Double.isNaN(value) || value < min || value > max) {
            throw new IllegalArgumentException(
                    fieldName + " must be between " + min + " and " + max
                            + " degrees, got: " + value);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SatellitePass)) {
            return false;
        }
        SatellitePass that = (SatellitePass) other;
        return satelliteId == that.satelliteId
                && startTime == that.startTime
                && endTime == that.endTime
                && durationSeconds == that.durationSeconds
                && Double.compare(that.maxElevationDeg, maxElevationDeg) == 0
                && Double.compare(that.startAzimuth, startAzimuth) == 0
                && Double.compare(that.maxAzimuth, maxAzimuth) == 0
                && Double.compare(that.endAzimuth, endAzimuth) == 0
                && daylight == that.daylight
                && Objects.equals(satelliteName, that.satelliteName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                satelliteId,
                satelliteName,
                startTime,
                endTime,
                durationSeconds,
                maxElevationDeg,
                startAzimuth,
                maxAzimuth,
                endAzimuth,
                daylight);
    }

    @Override
    public String toString() {
        return "SatellitePass{"
                + "satelliteId=" + satelliteId
                + ", satelliteName='" + satelliteName + '\''
                + ", startTime=" + startTime
                + ", endTime=" + endTime
                + ", durationSeconds=" + durationSeconds
                + ", maxElevationDeg=" + maxElevationDeg
                + ", startAzimuth=" + startAzimuth
                + ", maxAzimuth=" + maxAzimuth
                + ", endAzimuth=" + endAzimuth
                + ", daylight=" + daylight
                + '}';
    }
}
