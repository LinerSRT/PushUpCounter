package st.pushupcounter.view.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : "Line'R"
 * @mailto : serinity320@mail.com
 * @created : 20.02.2023, понедельник
 **/
public class WifiListener {
    private static WifiListener instance;
    private final ConnectivityManager connectivityManager;
    private final WifiManager wifiManager;
    private final List<Callback> callbackList;

    public static WifiListener getInstance(Context context) {
        return instance == null ? instance = new WifiListener(context) : instance;
    }

    private WifiListener(Context context) {
        this.callbackList = new ArrayList<>();
        this.connectivityManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        this.wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                    NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                    if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        if (wifiInfo != null && (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED)) {
                            for (Callback callback : callbackList)
                                callback.onWifiConnected();
                        }
                    }
                }
            }
        };
        context.registerReceiver(broadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public void subscribe(Callback callback) {
        this.callbackList.add(callback);
    }

    public interface Callback {
        void onWifiConnected();
    }
}
