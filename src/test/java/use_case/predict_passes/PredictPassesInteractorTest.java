package use_case.predict_passes;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import entity.SatellitePass;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class PredictPassesInteractorTest {

    @Test
    void invalidLatitudeReportsFailureWithoutCallingDataAccess() {
        PredictPassesDataAccessInterface dataAccess =
                (latitude, longitude, altitudeMeters, days) -> {
                    throw new AssertionError(
                            "Invalid input must not trigger a data request.");
                };

        RecordingPresenter presenter = new RecordingPresenter();
        PredictPassesInteractor interactor =
                new PredictPassesInteractor(dataAccess, presenter);

        interactor.execute(new PredictPassesInputData(
                100.0, -79.38, 100.0, 3, 20.0, true));

        assertFailure(presenter);
    }

    @Test
    void filtersPassesAndSortsByStartTime() {
        SatellitePass later = createPass(3000, 60.0, false);
        SatellitePass daylight = createPass(1500, 50.0, true);
        SatellitePass tooLow = createPass(1800, 19.0, false);
        SatellitePass atThreshold = createPass(2000, 20.0, false);

        PredictPassesDataAccessInterface dataAccess =
                (latitude, longitude, altitudeMeters, days) -> {
                    assertEquals(43.65, latitude);
                    assertEquals(-79.38, longitude);
                    assertEquals(100.0, altitudeMeters);
                    assertEquals(3, days);

                    return List.of(later, daylight, tooLow, atThreshold);
                };

        RecordingPresenter presenter = new RecordingPresenter();
        PredictPassesInteractor interactor =
                new PredictPassesInteractor(dataAccess, presenter);

        interactor.execute(validInput(true));

        assertSuccess(presenter, List.of(atThreshold, later));
    }

    @Test
    void includesDaylightWhenExclusionIsDisabled() {
        SatellitePass daylight = createPass(2000, 45.0, true);
        PredictPassesDataAccessInterface dataAccess =
                (latitude, longitude, altitudeMeters, days) ->
                        List.of(daylight);

        RecordingPresenter presenter = new RecordingPresenter();
        PredictPassesInteractor interactor =
                new PredictPassesInteractor(dataAccess, presenter);

        interactor.execute(validInput(false));

        assertSuccess(presenter, List.of(daylight));
    }

    @Test
    void emptyApiResultProducesSuccessfulEmptyOutput() {
        PredictPassesDataAccessInterface dataAccess =
                (latitude, longitude, altitudeMeters, days) -> List.of();

        RecordingPresenter presenter = new RecordingPresenter();
        PredictPassesInteractor interactor =
                new PredictPassesInteractor(dataAccess, presenter);

        interactor.execute(validInput(true));

        assertSuccess(presenter, List.of());
    }

    @Test
    void noMatchingPassesProducesSuccessfulEmptyOutput() {
        SatellitePass daylight = createPass(2000, 45.0, true);
        SatellitePass tooLow = createPass(3000, 10.0, false);
        PredictPassesDataAccessInterface dataAccess =
                (latitude, longitude, altitudeMeters, days) ->
                        List.of(daylight, tooLow);

        RecordingPresenter presenter = new RecordingPresenter();
        PredictPassesInteractor interactor =
                new PredictPassesInteractor(dataAccess, presenter);

        interactor.execute(validInput(true));

        assertSuccess(presenter, List.of());
    }

    @Test
    void dataAccessFailureProducesErrorInsteadOfSuccess() {
        PredictPassesDataAccessInterface dataAccess =
                (latitude, longitude, altitudeMeters, days) -> {
                    throw new PassPredictionException(
                            "Simulated connection failure");
                };

        RecordingPresenter presenter = new RecordingPresenter();
        PredictPassesInteractor interactor =
                new PredictPassesInteractor(dataAccess, presenter);

        interactor.execute(validInput(true));

        assertFailure(presenter);
    }

    @ParameterizedTest
    @CsvSource({
            "-90.1, 0, 100, 3, 20",
            "90.1, 0, 100, 3, 20",
            "0, -180.1, 100, 3, 20",
            "0, 180.1, 100, 3, 20",
            "NaN, 0, 100, 3, 20",
            "0, Infinity, 100, 3, 20",
            "0, 0, NaN, 3, 20",
            "0, 0, Infinity, 3, 20",
            "0, 0, 100, 0, 20",
            "0, 0, 100, 8, 20",
            "0, 0, 100, 3, -0.1",
            "0, 0, 100, 3, 90.1",
            "0, 0, 100, 3, NaN",
            "0, 0, 100, 3, Infinity"
    })
    void invalidValuesAreRejectedBeforeDataAccess(
            double latitude,
            double longitude,
            double altitudeMeters,
            int days,
            double minElevationDeg) {

        PredictPassesDataAccessInterface dataAccess =
                (lat, lon, altitude, numberOfDays) -> {
                    throw new AssertionError(
                            "Invalid input must not trigger a data request.");
                };

        RecordingPresenter presenter = new RecordingPresenter();
        PredictPassesInteractor interactor =
                new PredictPassesInteractor(dataAccess, presenter);

        interactor.execute(new PredictPassesInputData(
                latitude, longitude, altitudeMeters,
                days, minElevationDeg, true));

        assertFailure(presenter);
    }

    @ParameterizedTest
    @CsvSource({
            "-90, -180, 0, 1, 0",
            "90, 180, 100, 7, 90",
            "0, 0, -50, 3, 20",
            "43.65, -79.38, 100, 3, 20"
    })
    void validBoundaryValuesReachDataAccess(
            double latitude,
            double longitude,
            double altitudeMeters,
            int days,
            double minElevationDeg) {

        int[] requestCount = {0};

        PredictPassesDataAccessInterface dataAccess =
                (lat, lon, altitude, numberOfDays) -> {
                    requestCount[0]++;
                    assertEquals(latitude, lat);
                    assertEquals(longitude, lon);
                    assertEquals(altitudeMeters, altitude);
                    assertEquals(days, numberOfDays);
                    return List.of();
                };

        RecordingPresenter presenter = new RecordingPresenter();
        PredictPassesInteractor interactor =
                new PredictPassesInteractor(dataAccess, presenter);

        interactor.execute(new PredictPassesInputData(
                latitude, longitude, altitudeMeters,
                days, minElevationDeg, true));

        assertEquals(1, requestCount[0]);
        assertSuccess(presenter, List.of());
    }

    @Test
    void nullInputReportsFailureWithoutCallingDataAccess() {
        PredictPassesDataAccessInterface dataAccess =
                (latitude, longitude, altitudeMeters, days) -> {
                    throw new AssertionError(
                            "Null input must not trigger a data request.");
                };

        RecordingPresenter presenter = new RecordingPresenter();
        PredictPassesInteractor interactor =
                new PredictPassesInteractor(dataAccess, presenter);

        interactor.execute(null);

        assertFailure(presenter);
    }

    private static PredictPassesInputData validInput(
            boolean excludeDaylight) {
        return new PredictPassesInputData(
                43.65, -79.38, 100.0, 3, 20.0, excludeDaylight);
    }

    private static SatellitePass createPass(
            long startTime, double elevation, boolean daylight) {
        return new SatellitePass(
                SatellitePass.ISS_NORAD_ID,
                "ISS",
                startTime,
                startTime + 300,
                300,
                elevation,
                90.0,
                180.0,
                270.0,
                daylight);
    }

    private static void assertSuccess(
            RecordingPresenter presenter,
            List<SatellitePass> expectedPasses) {
        assertEquals(1, presenter.successCalls);
        assertEquals(0, presenter.failureCalls);
        assertNull(presenter.errorMessage);
        assertNotNull(presenter.outputData);
        assertEquals(expectedPasses, presenter.outputData.getPasses());
    }

    private static void assertFailure(RecordingPresenter presenter) {
        assertEquals(0, presenter.successCalls);
        assertEquals(1, presenter.failureCalls);
        assertNull(presenter.outputData);
        assertNotNull(presenter.errorMessage);
        assertFalse(presenter.errorMessage.isBlank());
    }

    private static class RecordingPresenter
            implements PredictPassesOutputBoundary {

        private PredictPassesOutputData outputData;
        private String errorMessage;
        private int successCalls;
        private int failureCalls;

        @Override
        public void prepareSuccessView(PredictPassesOutputData outputData) {
            this.outputData = outputData;
            successCalls++;
        }

        @Override
        public void prepareFailView(String errorMessage) {
            this.errorMessage = errorMessage;
            failureCalls++;
        }
    }
}