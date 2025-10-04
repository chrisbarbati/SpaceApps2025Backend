package org.chrisbarbati.spaceapps2025backend.groundbased.apiresponse;


public class AirQualityEntry {
    private Main main;
    private Components components;
    private long dt;

    // Getters and setters
    public Main getMain() {
        return main;
    }

    public void setMain(Main main) {
        this.main = main;
    }

    public Components getComponents() {
        return components;
    }

    public void setComponents(Components components) {
        this.components = components;
    }

    public long getDt() {
        return dt;
    }

    public void setDt(long dt) {
        this.dt = dt;
    }
}