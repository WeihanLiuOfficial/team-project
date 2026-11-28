package data_access;

import com.sun.net.httpserver.HttpServer;
import entity.SatellitePass;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import use_case.predict_passes.PassPredictionException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class N2yoPassDataAccessObjectTest {

    private HttpServer server;

    private final AtomicReference<String> receivedMethod =
            new AtomicReference<>();

    private final AtomicReference<String> receivedPath =
            new AtomicReference<>();

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsExpectedRequestAndReturnsParsedPass()
            throws IOException, PassPredictionException {
        String response =
                "{\"info\":{\"satid\":25544,"
                        + "\"satname\":\"SPACE STATION\"},"
                        + "\"passes\":[{"
                        + "\"startUTC\":1000,"
                        + "\"endUTC\":1600,"
                        + "\"duration\":445,"
                        + "\"maxEl\":45.0,"
                        + "\"startAz\":90.0,"
                        + "\"maxAz\":180.0,"
                        + "\"endAz\":270.0"
                        + "}]}";

        N2yoPassDataAccessObject dataAccess =
                startServer(200, response);

        List<SatellitePass> passes =
                dataAccess.fetchPasses(43.65, -79.38, 100.0, 3);

        assertEquals("GET", receivedMethod.get());
        assertEquals(
                "/visualpasses/25544/43.65/-79.38/100.0/3/1/"
                        + "&apiKey=test-key",
                receivedPath.get());

        assertEquals(1, passes.size());
        assertEquals(25544, passes.get(0).getSatelliteId());
        assertEquals(1000L, passes.get(0).getStartTime());
        assertEquals(445, passes.get(0).getDurationSeconds());
    }

    @Test
    void successfulEmptyResponseReturnsEmptyList()
            throws IOException, PassPredictionException {
        String response =
                "{\"info\":{\"satid\":25544,"
                        + "\"satname\":\"SPACE STATION\"},\"passes\":[]}";

        N2yoPassDataAccessObject dataAccess =
                startServer(200, response);

        List<SatellitePass> passes =
                dataAccess.fetchPasses(43.65, -79.38, 100.0, 3);

        assertTrue(passes.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 403, 429, 500})
    void unsuccessfulHttpStatusThrowsPredictionException(int status)
            throws IOException {
        N2yoPassDataAccessObject dataAccess =
                startServer(status, "{\"error\":\"Simulated failure\"}");

        assertThrows(
                PassPredictionException.class,
                () -> dataAccess.fetchPasses(
                        43.65, -79.38, 100.0, 3));
    }

    @Test
    void apiErrorInsideHttp200StillThrowsPredictionException()
            throws IOException {
        N2yoPassDataAccessObject dataAccess =
                startServer(200, "{\"error\":\"Invalid API key\"}");

        assertThrows(
                PassPredictionException.class,
                () -> dataAccess.fetchPasses(
                        43.65, -79.38, 100.0, 3));
    }

    @Test
    void invalidResponseBodyThrowsPredictionException()
            throws IOException {
        N2yoPassDataAccessObject dataAccess =
                startServer(200, "<html>Service unavailable</html>");

        assertThrows(
                PassPredictionException.class,
                () -> dataAccess.fetchPasses(
                        43.65, -79.38, 100.0, 3));
    }

    private N2yoPassDataAccessObject startServer(
            int status, String responseBody) throws IOException {

        server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0), 0);

        server.createContext("/", exchange -> {
            receivedMethod.set(exchange.getRequestMethod());
            receivedPath.set(exchange.getRequestURI().getRawPath());

            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                    "Content-Type", "application/json; charset=UTF-8");

            exchange.sendResponseHeaders(status, bytes.length);

            try (OutputStream output = exchange.getResponseBody()) {
                output.write(bytes);
            }
            finally {
                exchange.close();
            }
        });

        server.start();

        URI baseUri = URI.create(
                "http://127.0.0.1:"
                        + server.getAddress().getPort()
                        + "/");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        return new N2yoPassDataAccessObject(
                "test-key", client, baseUri);
    }
}
