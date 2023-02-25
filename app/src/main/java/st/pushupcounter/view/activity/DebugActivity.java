package st.pushupcounter.view.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import st.pushupcounter.R;
import st.pushupcounter.wifi.WifiModule;

@SuppressLint("MissingPermission")
public class DebugActivity extends AppCompatActivity {
    private WifiModule wifiModule;
    private Button findESP;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);
//        findESP = findViewById(R.id.findESP);
//        wifiModule = new WifiModule(this);
//        ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                new ActivityResultCallback<ActivityResult>() {
//                    @Override
//                    public void onActivityResult(ActivityResult result) {
//                        if (result.getResultCode() == Activity.RESULT_OK) {
//
//                            Log.d("TAGTAG", "onActivityResult: " + result.getData().toString());
//                        }
//                    }
//                });
//
//        findESP.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                wifiModule.scanWifi(new WifiModule.ScanCallback() {
//                    @Override
//                    public void onSuccess(List<ScanResult> scanResultList) {
//                        GenericConsumer.of(scanResultList)
//                                .next(input -> {
//                                    for (ScanResult scanResult : scanResultList)
//                                        if (scanResult.SSID.contains("PushUpCounter"))
//                                            return scanResult;
//                                    return null;
//                                })
//                                .ifPresent(scanResult -> wifiModule.connectWifi(scanResult.SSID, "1234567890"));
//
//                    }
//                    @Override
//                    public void onFailed() {
//
//                    }
//                });
//            }
//        });
//        ESPWebSocket webSocket = new ESPWebSocket();
//        findViewById(R.id.command).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                webSocket.openSocket(new ESPWebSocket.SocketListener() {
//                    @Override
//                    public void onOpened(ESPWebSocket webSocket) {
//                        webSocket.sendCommand("connect");
//                    }
//                }, null);
//
//            }
//        });
    }

    public static void getCurrentSsid(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            Log.d("TAGTAG", "getCurrentSsid: " + connectionInfo.toString());
        }
    }
}