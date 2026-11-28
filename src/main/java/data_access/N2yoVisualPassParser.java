package data_access;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import entity.SatellitePass;
import use_case.predict_passes.PassPredictionException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts N2YO visual-pass JSON into domain entities.
 * Only accepts responses from the visualpasses endpoint.
 */
public final class N2yoVisualPassParser {

    public List<SatellitePass> parse(String json)
            throws PassPredictionException {

        if (json == null || json.isBlank()) {
            throw new PassPredictionException("Empty API response.");
        }

        try {
            JsonObject root = requireObject(JsonParser.parseString(json));

            if (root.has("error")) {
                throw new PassPredictionException(
                        "N2YO returned an API error.");
            }

            JsonObject info = requireObject(root.get("info"));

            int satelliteId = requireNumber(info, "satid").intValueExact();

            if (satelliteId != SatellitePass.ISS_NORAD_ID) {
                throw new PassPredictionException(
                        "The response is not for the ISS.");
            }

            JsonElement name = info.get("satname");

            if (name == null || !name.isJsonPrimitive()
                    || !name.getAsJsonPrimitive().isString()) {
                throw new PassPredictionException(
                        "The satellite name is missing or invalid.");
            }

            String satelliteName = name.getAsString();
            JsonElement passesElement = root.get("passes");

            if (passesElement == null || !passesElement.isJsonArray()) {
                throw new PassPredictionException(
                        "The passes array is missing or invalid.");
            }

            List<SatellitePass> passes = new ArrayList<>();

            for (JsonElement element : passesElement.getAsJsonArray()) {
                JsonObject item = requireObject(element);

                SatellitePass pass = new SatellitePass(
                        satelliteId,
                        satelliteName,
                        requireNumber(item, "startUTC").longValueExact(),
                        requireNumber(item, "endUTC").longValueExact(),
                        requireNumber(item, "duration").intValueExact(),
                        requireNumber(item, "maxEl").doubleValue(),
                        requireNumber(item, "startAz").doubleValue(),
                        requireNumber(item, "maxAz").doubleValue(),
                        requireNumber(item, "endAz").doubleValue(),
                        false);

                // These passes already have a provider-predicted
                // dark-sky viewing opportunity. False means they are
                // not rejected for daylight; it does not mean that
                // every second of the whole pass is dark.
                passes.add(pass);
            }

            return List.copyOf(passes);
        }
        catch (JsonParseException | IllegalStateException
               | IllegalArgumentException | ArithmeticException exception) {
            throw new PassPredictionException(
                    "The API response contains malformed or invalid pass data.");
        }
    }

    private JsonObject requireObject(JsonElement element)
            throws PassPredictionException {
        if (element == null || !element.isJsonObject()) {
            throw new PassPredictionException(
                    "Expected a JSON object in the API response.");
        }
        return element.getAsJsonObject();
    }

    private BigDecimal requireNumber(JsonObject object, String field)
            throws PassPredictionException {
        JsonElement value = object.get(field);

        if (value == null || !value.isJsonPrimitive()
                || !value.getAsJsonPrimitive().isNumber()) {
            throw new PassPredictionException(
                    "Missing or invalid numeric field: " + field);
        }

        return value.getAsBigDecimal();
    }
}
