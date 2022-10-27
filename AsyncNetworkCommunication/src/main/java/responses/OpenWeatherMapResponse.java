package responses;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Getter;

import java.util.ArrayList;

@Getter
public class OpenWeatherMapResponse {
    ArrayList<Weather> weather;

    @JsonAlias("main")
    private MainInfo mainInfo;

    Wind wind;

    @Getter
    public static class Weather {
        private int id;
        private String description;
    }
    @Getter
    public static class MainInfo {
        private double temp;
        private double feels_like;
        private double humidity;
    }
    @Getter
    public static class Wind {
        private double speed;
        private double deg;
    }

}
