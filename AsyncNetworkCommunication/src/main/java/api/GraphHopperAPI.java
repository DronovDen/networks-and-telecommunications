package api;

import responses.GeoCodingResponse;

import java.net.URI;
import java.util.concurrent.CompletableFuture;


public class GraphHopperAPI {
    public static CompletableFuture<GeoCodingResponse> getAddressesByNameOrNull(String name, String key) {
        name = name.replace(' ', '+');
        String uriString = String.format("https://graphhopper.com/api/1/geocode?q=%s&locale=en&key=%s",
                name,
                key);

        URI uri = URI.create(uriString);
        return APIUtils.doAndParseRequest(uri, GeoCodingResponse.class, "ERROR: Status code of geocode response isn't 200!");
    }


}
