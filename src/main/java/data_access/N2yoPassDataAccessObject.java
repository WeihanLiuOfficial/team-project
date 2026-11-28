package data_access;

import entity.SatellitePass;
import use_case.predict_passes.PassPredictionException;
import use_case.predict_passes.PredictPassesDataAccessInterface;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Retrieves provider-filtered, optically visible ISS passes from N2YO.
 * Does not retrieve the complete set of daytime or radio passes.
 */
public final class N2yoPassDataAccessObject
        implements PredictPassesDataAccessInterface {

    private static final URI API_BASE =
            URI.create("https://api.n2yo.com/rest/v1/satellite/");

    private final String apiKey;
    private final HttpClient client;
    private final URI baseUri;
    private final N2yoVisualPassParser parser;

    public N2yoPassDataAccessObject(String apiKey) {
        this(
                apiKey,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .build(),
                API_BASE);
    }

    /**
     * Allows tests in this package to use a local HTTP server.
     */
    N2yoPassDataAccessObject(
            String apiKey,
            HttpClient client,
            URI baseUri) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "An N2YO API key is required.");
        }

        this.apiKey = apiKey.strip();
        this.client = Objects.requireNonNull(client);
        this.baseUri = Objects.requireNonNull(baseUri);
        this.parser = new N2yoVisualPassParser();
    }

    @Override
    public List<SatellitePass> fetchPasses(
            double latitude,
            double longitude,
            double altitudeMeters,
            int days) throws PassPredictionException {

        String encodedKey = URLEncoder.encode(
                apiKey, StandardCharsets.UTF_8);

        String path = "visualpasses/"
                + SatellitePass.ISS_NORAD_ID + "/"
                + latitude + "/"
                + longitude + "/"
                + altitudeMeters + "/"
                + days + "/1/"
                + "&apiKey=" + encodedKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(baseUri.resolve(path))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8));

            int status = response.statusCode();

            if (status == 401 || status == 403) {
                throw new PassPredictionException(
                        "N2YO denied access. Check the API credentials.");
            }

            if (status == 429) {
                throw new PassPredictionException(
                        "N2YO request limit reached. Please try again later.");
            }

            if (status != 200) {
                throw new PassPredictionException(
                        "N2YO request failed with HTTP status " + status + ".");
            }

            return parser.parse(response.body());
        }
        catch (HttpTimeoutException exception) {
            throw new PassPredictionException(
                    "The N2YO request timed out.");
        }
        catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PassPredictionException(
                    "The N2YO request was interrupted.");
        }
        catch (IOException exception) {
            throw new PassPredictionException(
                    "Unable to connect to N2YO.");
        }
    }
}
