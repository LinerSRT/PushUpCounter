package st.pushupcounter.wifi;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : "Line'R"
 * @mailto : serinity320@mail.com
 * @created : 25.02.2023, суббота
 **/
@SuppressLint("MissingPermission")
public class Wifi {
    private static Wifi instance;
    private final Context context;
    private final WifiManager wifiManager;
    private final ConnectivityManager connectivityManager;
    private final LocationManager locationManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    private Wifi(Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public static synchronized Wifi getInstance(Context context) {
        if (instance == null) {
            instance = new Wifi(context.getApplicationContext());
        }
        return instance;
    }

    public void connect(String ssid, String password) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (networkCallback == null)
                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        connectivityManager.bindProcessToNetwork(network);
                    }
                };
            WifiUtils.connectWifiQ(connectivityManager, ssid, password, networkCallback);
        } else {
            WifiUtils.connectWifiDeprecated(wifiManager, ssid, password);
        }
    }

    public boolean disconnect() {
        if (networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
        return wifiManager.disconnect();
    }

    public WifiInfo getWifiInfo() {
        return wifiManager.getConnectionInfo();
    }

    public String getConnectedSSID() {
        WifiInfo wifiInfo = getWifiInfo();
        if (wifiInfo != null) {
            String ssid = wifiInfo.getSSID();
            if (ssid != null && ssid.length() > 2 && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length() - 1);
            }
            return ssid;
        }
        return null;
    }


    public Wifi scan(String ssidFilter, WifiScanCallback callback) {
        if (!wifiManager.isWifiEnabled()) {
            callback.onWifiDisabled();
            return this;
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callback.onPermissionDenied();
            return this;
        }
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
            callback.onLocationDisabled();
            return this;
        }
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
               List<ScanResult> scanResults = wifiManager.getScanResults();
                if (ssidFilter != null && !ssidFilter.isEmpty()) {
                    List<ScanResult> filtered = new ArrayList<>();
                    for (ScanResult scanResult : scanResults)
                        if (scanResult.SSID.contains(ssidFilter))
                            filtered.add(scanResult);
                    scanResults = filtered;
                }
                callback.onResult(scanResults);
                context.unregisterReceiver(this);
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        wifiManager.startScan();
        context.registerReceiver(receiver, filter);
        return this;
    }

    public Wifi scan(WifiScanCallback callback) {
        if (!wifiManager.isWifiEnabled()) {
            callback.onWifiDisabled();
            return this;
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            callback.onPermissionDenied();
            return this;
        }
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                List<ScanResult> scanResults = wifiManager.getScanResults();
                callback.onResult(scanResults);
                context.unregisterReceiver(this);
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        wifiManager.startScan();
        context.registerReceiver(receiver, filter);
        return this;
    }

    public Wifi waitForConnection(String ssidFilter, WifiConnectionListener listener) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                    NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    if (networkInfo != null && networkInfo.isConnected()) {
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        if (wifiInfo != null && wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                            String ssid = wifiInfo.getSSID().replace("\"", "");
                            if (ssid.contains(ssidFilter)) {
                                listener.onConnected(ssid);
                                context.unregisterReceiver(this);
                            }
                        }
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        context.registerReceiver(receiver, filter);
        return this;
    }
    public Wifi waitForConnection(WifiConnectionListener listener) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                    NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    if (networkInfo != null && networkInfo.isConnected()) {
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        if (wifiInfo != null && wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
                            listener.onConnected(null);
                            context.unregisterReceiver(this);
                        }
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        context.registerReceiver(receiver, filter);
        return this;
    }

    public Wifi ifDisconnected(String ssidFilter, WifiDisconnectionListener listener) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
                    NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    if (networkInfo != null && networkInfo.isConnected()) {
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        if (wifiInfo != null && wifiInfo.getSupplicantState() == SupplicantState.DISCONNECTED) {
                            String ssid = wifiInfo.getSSID().replace("\"", "");
                            if (ssid.contains(ssidFilter)) {
                                listener.onDisconnected(ssid);
                                context.unregisterReceiver(this);
                            }
                        }
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        context.registerReceiver(receiver, filter);
        return this;
    }


    public interface WifiScanCallback {
        void onResult(List<ScanResult> scanResults);

        void onPermissionDenied();

        void onWifiDisabled();

        void onLocationDisabled();
    }

    public interface WifiConnectionListener {
        void onConnected(String ssid);
    }

    public interface WifiDisconnectionListener {
        void onDisconnected(String ssid);
    }
}
