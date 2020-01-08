package biz.ftsdesign.paranoiddiary.geo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import biz.ftsdesign.paranoiddiary.R;
import biz.ftsdesign.paranoiddiary.model.GeoTag;

public final class GeoUtils {
    private static final int REQUEST_LOCATION = 1;
    private static final long ONE_MINUTE_IN_MS = 60 * 1000;
    private static final long UPDATE_INTERVAL_MS = 5 * ONE_MINUTE_IN_MS;
    private static final float MIN_DISTANCE_CHANGE_METERS = 100;
    private static Location latestLocation = null;

    private GeoUtils() {
        throw new UnsupportedOperationException();
    }

    private static boolean haveLocationPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public static GeoTag getGeoTag(@NonNull Activity activity) {
        Log.i(GeoUtils.class.getSimpleName(), "getGeoTag");
        GeoTag geoTag = null;

        boolean geoTaggingEnabled = isGeoTaggingEnabled(activity);

        if (!haveLocationPermission(activity)) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);

        }

        if (geoTaggingEnabled && haveLocationPermission(activity)) {
            try {
                LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
                if (locationManager != null) {
                    String bestProvider = getBestProvider(locationManager);
                    if (bestProvider != null) {
                        setNewLocation(locationManager.getLastKnownLocation(bestProvider));
                        if (latestLocation != null) {
                            geoTag = new GeoTag(latestLocation.getLatitude(), latestLocation.getLongitude());
                            Log.i(GeoUtils.class.getSimpleName(), geoTag.toString());
                        }
                    }
                }

            } catch (SecurityException e) {
                Log.wtf(GeoUtils.class.getSimpleName(), "Checked for permissions, but getLocation still failed", e);
            }
        }
        return geoTag;
    }

    private static String getBestProvider(LocationManager lm) {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        return lm.getBestProvider(criteria, true);
    }

    private static boolean isGeoTaggingEnabled(@NonNull Activity activity) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        return sharedPreferences.getBoolean(activity.getString(R.string.pref_key_geotagging_enabled), false);
    }

    private static void setNewLocation(Location location) {
        if (location != null && (latestLocation == null || location.getTime() > latestLocation.getTime())) {
            latestLocation = location;
        }
    }

    public static void requestLocationUpdates(Activity activity) {
        if (GeoUtils.isGeoTaggingEnabled(activity)) {
            final LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
            if (locationManager != null) {
                LocationListener dummyListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        setNewLocation(location);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {

                    }

                    @Override
                    public void onProviderEnabled(String provider) {

                    }

                    @Override
                    public void onProviderDisabled(String provider) {

                    }
                };

                if (haveLocationPermission(activity)) {
                    try {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_INTERVAL_MS, MIN_DISTANCE_CHANGE_METERS, dummyListener);
                        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, UPDATE_INTERVAL_MS, MIN_DISTANCE_CHANGE_METERS, dummyListener);
                        locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, UPDATE_INTERVAL_MS, MIN_DISTANCE_CHANGE_METERS, dummyListener);
                    } catch (SecurityException e) {
                        Log.wtf(GeoUtils.class.getSimpleName(), "Checked for permissions, but getLocation still failed", e);
                    } catch (Exception e) {
                        Log.wtf(GeoUtils.class.getSimpleName(), "Something else happened", e);
                    }
                }
            }
        }
    }
}
