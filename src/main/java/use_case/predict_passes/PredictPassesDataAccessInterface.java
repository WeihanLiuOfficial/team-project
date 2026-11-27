package use_case.predict_passes;

import entity.SatellitePass;

import java.util.List;

/**
 * Provides upcoming ISS pass predictions for an observer.
 */
public interface PredictPassesDataAccessInterface {

    /**
     * Retrieves predicted ISS passes for the requested location.
     *
     * @param latitude observer latitude in degrees
     * @param longitude observer longitude in degrees
     * @param altitudeMeters observer altitude above sea level in meters
     * @param days number of upcoming days to query
     * @return a non-null list of passes; empty if no passes are found
     * @throws PassPredictionException if predictions cannot be retrieved
     */
    List<SatellitePass> fetchPasses(double latitude,
                                    double longitude,
                                    double altitudeMeters,
                                    int days)
            throws PassPredictionException;
}