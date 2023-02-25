package st.pushupcounter.wifi;

import android.annotation.SuppressLint;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author : "Line'R"
 * @mailto : serinity320@mail.com
 * @created : 22.02.2023, среда
 **/
@SuppressLint("MissingPermission")
public class WifiUtils {

    @RequiresApi(Build.VERSION_CODES.Q)
    public static void connectWifiQ(@NonNull ConnectivityManager connectivityManager, @NonNull String ssid, @Nullable String password, ConnectivityManager.NetworkCallback networkCallback) {
        WifiNetworkSpecifier.Builder specifierBuilder = new WifiNetworkSpecifier.Builder();
        specifierBuilder.setSsid(ssid);
        specifierBuilder.setIsHiddenSsid(false);
        specifierBuilder.setWpa2Passphrase(password);
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                .setNetworkSpecifier(specifierBuilder.build())
                .build();
        connectivityManager.requestNetwork(networkRequest, networkCallback, Math.toIntExact(TimeUnit.SECONDS.toMillis(10)));
    }

    public static void connectWifiDeprecated(@NonNull WifiManager wifiManager, @NonNull String ssid, @Nullable String password) {
        WifiConfiguration wifiConfiguration = configurationOf(ssid, password);
        int networkId = registerWifiNetwork(wifiManager, wifiConfiguration);
        if (networkId == -1)
            return;
        if (!wifiManager.disconnect())
            return;
        if (!wifiManager.enableNetwork(networkId, true))
            return;
        wifiManager.reconnect();
    }

    private static int registerWifiNetwork(WifiManager wifiManager, WifiConfiguration wifiConfiguration) {
        int networkId = -1;
        List<WifiConfiguration> configurationList = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration configuration : configurationList) {
            if (wifiConfiguration.SSID.equals(configuration.SSID) && (wifiConfiguration.BSSID == null || configuration.BSSID == null || wifiConfiguration.BSSID.equals(configuration.BSSID))) {
                wifiConfiguration.networkId = configuration.networkId;
                networkId = wifiManager.updateNetwork(wifiConfiguration);
            }
        }
        if (networkId == -1) {
            networkId = wifiManager.addNetwork(wifiConfiguration);
            wifiManager.saveConfiguration();
            return networkId;
        }
        return -1;
    }

    private static WifiConfiguration configurationOf(@NonNull String ssid,@Nullable String password) {
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = String.format("\"%s\"", ssid);
        wifiConfiguration.hiddenSSID = false;
        wifiConfiguration.preSharedKey = String.format("\"%s\"", password);
        wifiConfiguration.allowedProtocols.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wifiConfiguration.status = WifiConfiguration.Status.ENABLED;
        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wifiConfiguration.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wifiConfiguration.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        wifiConfiguration.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        return wifiConfiguration;
    }
}
