package biz.ftsdesign.paranoiddiary.model;

import androidx.annotation.NonNull;

public class GeoTag {
    private final double lat;
    private final double lon;

    public GeoTag(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    @Override
    public @NonNull String toString() {
        return lat + "," + lon;
    }
}
