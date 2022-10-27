package api;

import responses.OpenWeatherMapResponse;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class OpenWeatherMapAPI {
    public static CompletableFuture<OpenWeatherMapResponse> getWeatherByCordsOrNull(double lat, double lng, String key) {
        String uriString = String.format("http://api.openweathermap.org/data/2.5/weather?lat=%f&lon=%f&appid=%s",lat, lng, key);
        URI uri = URI.create(uriString);

        return APIUtils.doAndParseRequest(uri, OpenWeatherMapResponse.class,"ERROR: Status code of Open Weather Map response isn't 200!");
    }
}
