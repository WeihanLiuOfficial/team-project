package data_access;

import entity.SatellitePass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import use_case.predict_passes.PassPredictionException;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class N2yoVisualPassParserTest {

    private static final String VALID_JSON =
            "{\"info\":{\"satid\":25544,\"satname\":\"SPACE STATION\"},"
                    + "\"passes\":[{"
                    + "\"startUTC\":1000,"
                    + "\"endUTC\":1600,"
                    + "\"duration\":445,"
                    + "\"maxEl\":88.07,"
                    + "\"startAz\":301.72,"
                    + "\"maxAz\":14.21,"
                    + "\"endAz\":122.97"
                    + "}]}";

    @Test
    void validResponseMapsFieldsAndPreservesVisibleDuration()
            throws PassPredictionException {
        N2yoVisualPassParser parser = new N2yoVisualPassParser();

        List<SatellitePass> passes = parser.parse(VALID_JSON);

        assertEquals(1, passes.size());

        SatellitePass pass = passes.get(0);

        assertEquals(25544, pass.getSatelliteId());
        assertEquals("SPACE STATION", pass.getSatelliteName());
        assertEquals(1000L, pass.getStartTime());
        assertEquals(1600L, pass.getEndTime());

        // Visible duration must come from the API, not end - start.
        assertEquals(445, pass.getDurationSeconds());

        assertEquals(88.07, pass.getMaxElevationDeg(), 0.000001);
        assertEquals(301.72, pass.getStartAzimuth(), 0.000001);
        assertEquals(14.21, pass.getMaxAzimuth(), 0.000001);
        assertEquals(122.97, pass.getEndAzimuth(), 0.000001);

        // This adapter accepts provider-filtered visual passes.
        assertFalse(pass.isDaylight());
    }

    @Test
    void emptyPassesArrayReturnsEmptyList()
            throws PassPredictionException {
        String json =
                "{\"info\":{\"satid\":25544,"
                        + "\"satname\":\"SPACE STATION\"},\"passes\":[]}";

        N2yoVisualPassParser parser = new N2yoVisualPassParser();

        List<SatellitePass> passes = parser.parse(json);

        assertTrue(passes.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("invalidResponses")
    void invalidResponseThrowsPredictionException(String json) {
        N2yoVisualPassParser parser = new N2yoVisualPassParser();

        assertThrows(
                PassPredictionException.class,
                () -> parser.parse(json));
    }

    private static Stream<String> invalidResponses() {
        return Stream.of(
                // Missing or unusable response.
                (String) null,
                "",
                "<html>Service unavailable</html>",

                // Wrong root structure or missing required fields.
                "[]",
                "{}",

                // API-reported failure.
                "{\"error\":\"Invalid API key\"}",

                // Response for a different satellite.
                VALID_JSON.replace(
                        "\"satid\":25544",
                        "\"satid\":12345"),

                // Missing start time.
                VALID_JSON.replace(
                        "\"startUTC\":1000,",
                        ""),

                // A timestamp must not be silently truncated.
                VALID_JSON.replace(
                        "\"startUTC\":1000",
                        "\"startUTC\":1000.5"),

                // End time precedes start time.
                VALID_JSON.replace(
                        "\"endUTC\":1600",
                        "\"endUTC\":999"),

                // Elevation is outside the domain's valid range.
                VALID_JSON.replace(
                        "\"maxEl\":88.07",
                        "\"maxEl\":91"),

                // Duration exceeds the int range.
                VALID_JSON.replace(
                        "\"duration\":445",
                        "\"duration\":2147483648")
        );
    }
}
