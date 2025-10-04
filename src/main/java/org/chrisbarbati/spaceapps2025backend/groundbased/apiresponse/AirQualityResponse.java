package org.chrisbarbati.spaceapps2025backend.groundbased.apiresponse;

import java.util.List;

public class AirQualityResponse {
    private Coord coord;
    private List<AirQualityEntry> list;

    // Getters and setters
    public Coord getCoord() {
        return coord;
    }

    public void setCoord(Coord coord) {
        this.coord = coord;
    }

    public List<AirQualityEntry> getList() {
        return list;
    }

    public void setList(List<AirQualityEntry> list) {
        this.list = list;
    }
}
