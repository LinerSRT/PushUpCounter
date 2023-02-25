package st.pushupcounter.wifi;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.LocaleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import st.pushupcounter.view.util.Broadcast;

/**
 * @author : "Line'R"
 * @mailto : serinity320@mail.com
 * @created : 20.02.2023, понедельник
 **/
@SuppressLint("MissingPermission")
public class WifiModule {
    private final Context context;
    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;
    private LocationManager localeManager;
    private Broadcast scanReceiver;
    private Broadcast wifiReceiver;
    private ScanCallback scanCallback;
    private ConnectCallback connectCallback;
    private ConnectivityManager.NetworkCallback networkCallback;

    public WifiModule(@NonNull Context context) {
        this.context = context;
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        this.connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        this.localeManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.scanReceiver = new Broadcast(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
            @Override
            public void handleChanged(Intent intent) {
                if (scanCallback != null) {
                    List<ScanResult> scanResultList = wifiManager.getScanResults();
                    List<ScanResult> filteredList = new ArrayList<>();
                    for (ScanResult scanResult : scanResultList)
                        if (!filteredList.contains(scanResult))
                            filteredList.add(scanResult);
                    scanCallback.onSuccess(filteredList);
                }
                scanReceiver.setListening(false);
            }
        };
        this.wifiReceiver = new Broadcast(WifiManager.WIFI_STATE_CHANGED_ACTION) {
            @Override
            public void handleChanged(Intent intent) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if(networkInfo != null && networkInfo.isConnected()){
                    if(connectCallback != null)
                        connectCallback.onSuccess();
                } else {
                    if(connectCallback != null)
                        connectCallback.onFailed();
                }
                wifiReceiver.setListening(false);
            }
        };
        this.networkCallback = new ConnectivityManager.NetworkCallback(){
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    connectivityManager.bindProcessToNetwork(network);
                if(connectCallback != null)
                    connectCallback.onSuccess();
            }

            @Override
            public void onUnavailable() {
                if(connectCallback != null)
                    connectCallback.onFailed();
            }
        };
    }

    public void scanWifi(ScanCallback callback) {
        this.scanCallback = callback;
        if(wifiManager.isWifiEnabled()){
            if(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                if(localeManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || localeManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
                    this.scanReceiver.setListening(true);
                    this.wifiManager.startScan();
                } else {
                    scanCallback.onFailed(ScanFailed.LOCATION_DISABLED);
                }
            } else {
                scanCallback.onFailed(ScanFailed.LOCATION_PERMISSION_DENIED);
            }
        } else {
            scanCallback.onFailed(ScanFailed.WIFI_DISABLED);
        }
    }

    public void connectWifi(@NonNull String ssid, @NonNull String password, ConnectCallback connectCallback){
        this.connectCallback = connectCallback;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            WifiUtils.connectWifiQ(connectivityManager, ssid, password, networkCallback);
        } else {
            wifiReceiver.setListening(true);
            WifiUtils.connectWifiDeprecated(wifiManager, ssid, password);
        }
    }

    public boolean isConnected(){
        if(!wifiManager.isWifiEnabled())
            return false;
        WifiInfo networkInfo = wifiManager.getConnectionInfo();
        return networkInfo != null && networkInfo.getSSID().contains("PushUpCounter");
    }

    @Nullable
    public String getConnectedSSID(){
        if(isConnected())
            return wifiManager.getConnectionInfo().getSSID();
        return null;
    }

    public WifiManager getWifiManager() {
        return wifiManager;
    }

    public ConnectivityManager getConnectivityManager() {
        return connectivityManager;
    }

    public interface ScanCallback {
        void onSuccess(List<ScanResult> scanResultList);
        void onFailed(ScanFailed reason);
    }
    public interface ConnectCallback{
        void onSuccess();
        void onFailed();
    }

    public enum ScanFailed{
        WIFI_DISABLED("To scan WiFi networks, WiFi should be enabled"),
        LOCATION_PERMISSION_DENIED("Location permission is denied, scan networks impossible"),
        LOCATION_DISABLED("Location services is disabled, scan networks impossible");
        private final String description;

        ScanFailed(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
