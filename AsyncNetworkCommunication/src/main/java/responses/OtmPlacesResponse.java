package responses;

import lombok.Getter;

import java.util.ArrayList;
@Getter
public class OtmPlacesResponse {
    private ArrayList<OtmFeature> features;

    @Getter
    public static class OtmFeature {
        private String type;
        private String id;
        private OtmProperties properties;
    }
    @Getter
    public static class OtmProperties {
        private String xid;
        private String name;
        private double dist;
        private int rate;
        private String kinds;
    }
}
