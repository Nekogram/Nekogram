package tw.nekomimi.nekogram.location;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Looper;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.LocationSource;

import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;

public class NekoLocationSource implements LocationSource {
    public final static Set<Integer> recent = Collections.synchronizedSet(Collections.newSetFromMap(new Cache<>()));
    private boolean checkPermission = true;
    private final Context context;
    private OnLocationChangedListener onLocationChangedListener;
    private final LocationCallback callback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if (onLocationChangedListener != null) {
                Location location = locationResult.getLastLocation();
                transform(location);
                onLocationChangedListener.onLocationChanged(location);
            }
        }
    };

    public NekoLocationSource(Context context) {
        this.context = context;
    }

    public static void transform(Location location) {
        final double latitude = location.getLatitude();
        final double longitude = location.getLongitude();

        if (recent.contains(new Pair<>(latitude, longitude).hashCode())) return;// Dejavu

        final Pair<Double, Double> trans = GeodeticTransform.transform(latitude, longitude);
        location.setLatitude(trans.first);
        location.setLongitude(trans.second);

        recent.add(trans.hashCode());

        if (BuildVars.LOGS_ENABLED) {
            FileLog.d(String.format(Locale.US, "%.4f,%.4f => %.4f,%.4f", latitude, longitude, trans.first, trans.second));
        }
    }

    @Override
    public void activate(@NonNull OnLocationChangedListener onLocationChangedListener) {
        if (checkPermission && Build.VERSION.SDK_INT >= 23) {
            if (context instanceof Activity) {
                checkPermission = false;
                Activity activity = (Activity) context;
                if (activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    activity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 2);
                    return;
                }
            }
        }
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(0);
        locationRequest.setFastestInterval(0);
        LocationServices.getFusedLocationProviderClient(context).requestLocationUpdates(locationRequest, callback, Looper.getMainLooper());
        this.onLocationChangedListener = onLocationChangedListener;
    }

    @Override
    public void deactivate() {
        LocationServices.getFusedLocationProviderClient(context).removeLocationUpdates(callback);
    }

    static class Cache<K, V> extends LinkedHashMap<K, V> {

        private static final int KMaxEntries = 128;
        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(final java.util.Map.Entry<K, V> eldest) {
            return (size() > KMaxEntries);
        }
    }
}
