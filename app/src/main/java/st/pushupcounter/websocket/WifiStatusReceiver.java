package st.pushupcounter.websocket;

import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import st.pushupcounter.view.util.Broadcast;

/**
 * @author : "Line'R"
 * @mailto : serinity320@mail.com
 * @created : 23.02.2023, четверг
 **/
public class WifiStatusReceiver extends Broadcast {
    private final WifiManager wifiManager;
    private final WifiStatusListener listener;

    public WifiStatusReceiver(WifiStatusListener listener){
        super(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        this.listener = listener;
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    public void handleChanged(Intent intent) {
        String action = intent.getAction();
        if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (networkInfo != null && networkInfo.isConnected()) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (listener != null)
                    listener.onWifiChanged(wifiInfo.getSSID());
            } else {
                if (listener != null)
                    listener.onWifiDisconnected();
            }
        }
    }

    public boolean isConnected(String ssid){
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo != null && wifiInfo.getSSID().contains(ssid);
    }

    public interface WifiStatusListener {
        void onWifiChanged(String SSID);

        void onWifiDisconnected();
    }
}
