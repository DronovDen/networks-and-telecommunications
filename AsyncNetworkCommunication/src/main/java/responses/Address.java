package responses;

import lombok.Getter;

@Getter
public class Address {
    private Point point;
    private String osm_id;
    private String osm_type;
    private String osm_key;
    private String osm_value;
    private String name;
    private String country;
    private String city;
    private String state;
    private String street;
    private String postcode;

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("");
        if (country != null) {
            stringBuilder.append(country).append(",");
        }
        if (city != null) {
            stringBuilder.append(city).append(",");
        }
        if (state != null) {
            stringBuilder.append(state).append(",");
        }
        if (street != null) {
            stringBuilder.append(street).append(",");
        }
        if (name != null) {
            stringBuilder.append(name).append(",");
        }
        if (postcode != null) {
            stringBuilder.append(postcode);
        }
        if (stringBuilder.charAt(stringBuilder.length() - 1) == ',') {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }
        return stringBuilder.toString();
    }
}
