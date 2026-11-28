package interface_adapter.predict_passes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import use_case.predict_passes.PredictPassesInputBoundary;
import use_case.predict_passes.PredictPassesInputData;
import use_case.predict_passes.PredictPassesOutputBoundary;
import use_case.predict_passes.PredictPassesOutputData;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class PredictPassesControllerTest {

    @Test
    void validTextIsConvertedAndPassedToUseCase() {
        RecordingUseCase useCase = new RecordingUseCase();
        RecordingOutput output = new RecordingOutput();
        PredictPassesController controller =
                new PredictPassesController(useCase, output);

        controller.execute(
                " 43.65 ", " -79.38 ", " 100 ", " 3 ", " 20 ");

        assertEquals(1, useCase.calls);
        assertNotNull(useCase.input);
        assertEquals(43.65, useCase.input.getLatitude());
        assertEquals(-79.38, useCase.input.getLongitude());
        assertEquals(100.0, useCase.input.getAltitudeMeters());
        assertEquals(3, useCase.input.getDays());
        assertEquals(20.0, useCase.input.getMinElevationDeg());
        assertTrue(useCase.input.isExcludeDaylight());
        assertEquals(0, output.failureCalls);
        assertNull(output.errorMessage);
    }

    @ParameterizedTest
    @MethodSource("invalidInputs")
    void invalidTextReportsErrorWithoutExecutingUseCase(
            String latitude,
            String longitude,
            String altitude,
            String days,
            String elevation) {

        RecordingUseCase useCase = new RecordingUseCase();
        RecordingOutput output = new RecordingOutput();
        PredictPassesController controller =
                new PredictPassesController(useCase, output);

        controller.execute(
                latitude, longitude, altitude, days, elevation);

        assertEquals(0, useCase.calls);
        assertNull(useCase.input);
        assertEquals(1, output.failureCalls);
        assertNotNull(output.errorMessage);
        assertFalse(output.errorMessage.isBlank());
    }

    @Test
    void numericRangeValidationIsDelegatedToUseCase() {
        RecordingUseCase useCase = new RecordingUseCase();
        RecordingOutput output = new RecordingOutput();
        PredictPassesController controller =
                new PredictPassesController(useCase, output);

        controller.execute("100", "-79.38", "100", "3", "20");

        assertEquals(1, useCase.calls);
        assertNotNull(useCase.input);
        assertEquals(100.0, useCase.input.getLatitude());
        assertEquals(0, output.failureCalls);
    }

    private static Stream<Arguments> invalidInputs() {
        return Stream.of(
                Arguments.of(
                        null, "-79.38", "100", "3", "20"),
                Arguments.of(
                        "   ", "-79.38", "100", "3", "20"),
                Arguments.of(
                        "43.65", "", "100", "3", "20"),
                Arguments.of(
                        "43.65", "-79.38", "", "3", "20"),
                Arguments.of(
                        "43.65", "-79.38", "100", "", "20"),
                Arguments.of(
                        "43.65", "-79.38", "100", "3", ""),
                Arguments.of(
                        "hello", "-79.38", "100", "3", "20"),
                Arguments.of(
                        "43.65", "-79.38", "100", "3.5", "20")
        );
    }

    private static class RecordingUseCase
            implements PredictPassesInputBoundary {
        private int calls;
        private PredictPassesInputData input;

        @Override
        public void execute(PredictPassesInputData inputData) {
            calls++;
            input = inputData;
        }
    }

    private static class RecordingOutput
            implements PredictPassesOutputBoundary {
        private int failureCalls;
        private String errorMessage;

        @Override
        public void prepareSuccessView(PredictPassesOutputData outputData) {
            fail("The controller should delegate success to the use case.");
        }

        @Override
        public void prepareFailView(String errorMessage) {
            failureCalls++;
            this.errorMessage = errorMessage;
        }
    }
}
