package responses;

import lombok.Getter;

import java.util.ArrayList;

@Getter
public class GeoCodingResponse {
    private ArrayList<Address> hits;
}
