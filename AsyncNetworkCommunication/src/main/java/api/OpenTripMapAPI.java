package api;

import responses.OtmPlaceInfo;
import responses.OtmPlacesResponse;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

public class OpenTripMapAPI {
    public static CompletableFuture<OtmPlacesResponse> getInterestingPlacesOrNull(double lat, double lng, int radius, String key) {
        String latS = Double.toString(lat).replace(',', '.');
        String lngS = Double.toString(lng).replace(',', '.');
        String uriString = String.format("https://api.opentripmap.com/0.1/en/places/radius?radius=%d&lon=%s&lat=%S&apikey=%s",
                radius,
                lngS,
                latS,
                key);

        URI uri = URI.create(uriString);
        return APIUtils.doAndParseRequest(uri, OtmPlacesResponse.class,
                "ERROR: Status code of Open Trip Map (places list) response isn't 200");

    }

    public static CompletableFuture<OtmPlaceInfo> getInfoAboutPlaceOrNull(String xid, String key) {
        String uriString = String.format("https://api.opentripmap.com/0.1/en/places/xid/%s?apikey=%s",
                xid,
                key);

        URI uri = URI.create(uriString);
        return APIUtils.doAndParseRequest(uri, OtmPlaceInfo.class,
                "ERROR: Status code of Open Trip Map (detailed info) response isn't 200!");
    }
}
