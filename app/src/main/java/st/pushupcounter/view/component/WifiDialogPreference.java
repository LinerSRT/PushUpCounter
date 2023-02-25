package st.pushupcounter.view.component;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.AttributeSet;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import java.util.ArrayList;
import java.util.List;

import st.pushupcounter.R;
import st.pushupcounter.view.util.PermissionWrapper;
import st.pushupcounter.websocket.WifiStatusReceiver;
import st.pushupcounter.wifi.WifiModule;

/**
 * @author : "Line'R"
 * @mailto : serinity320@mail.com
 * @created : 22.02.2023, среда
 **/
@SuppressWarnings("unused")
public class WifiDialogPreference extends Preference implements WifiModule.ScanCallback {
    private ProgressDialog progressDialog;
    private WifiModule wifiModule;
    private String selectedSSID;
    private WifiStatusReceiver wifiStatusReceiver;
    private boolean wasConnected = false;

    public WifiDialogPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public WifiDialogPreference(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public WifiDialogPreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WifiDialogPreference(@NonNull Context context) {
        super(context);
        init();
    }

    private void init() {
        wifiModule = new WifiModule(getContext());
        wifiStatusReceiver = new WifiStatusReceiver(new WifiStatusReceiver.WifiStatusListener() {
            @Override
            public void onWifiChanged(String SSID) {
                if (SSID.contains("PushUpCounter")) {
                    if(!wasConnected) {
                        wasConnected = true;
                        new Handler(Looper.getMainLooper()).post(() -> {
                            setSummary(String.format(getContext().getString(R.string.summary_remote_device_connected), SSID));
                        });
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        setSummary(getContext().getString(R.string.select_remote_device_preference_summary));
                        if (wasConnected) {
                            Toast.makeText(getContext(), getContext().getString(R.string.toast_remote_device_disconnected_network_change), Toast.LENGTH_SHORT).show();
                            wasConnected = false;
                        }
                    });
                }
            }

            @Override
            public void onWifiDisconnected() {
                new Handler(Looper.getMainLooper()).post(() -> {
                    setSummary(getContext().getString(R.string.select_remote_device_preference_summary));
                    if (wasConnected) {
                        Toast.makeText(getContext(), getContext().getString(R.string.toast_remote_device_disconnected_wifi_disabled), Toast.LENGTH_SHORT).show();
                        wasConnected = false;
                    }
                });
            }
        });
        wifiStatusReceiver.setListening(true);
    }

    @Override
    protected void onClick() {
        if (!wasConnected) {
            showProgress(getContext().getString(R.string.dialog_remote_device_search), getContext().getString(R.string.dialog_remote_device_search_description));
            wifiModule.scanWifi(this);
        } else {
            Toast.makeText(getContext(), getContext().getString(R.string.toast_remote_device_already_connected), Toast.LENGTH_SHORT).show();
        }
    }

    private void showProgress(String title, String message) {
        if (progressDialog != null && progressDialog.isShowing())
            progressDialog.dismiss();
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle(title);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    private AlertDialog.Builder showAlert(String title, String message) {
        return new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false);
    }

    @Override
    public void onSuccess(List<ScanResult> scanResultList) {
        progressDialog.dismiss();
        List<String> espDevices = new ArrayList<>();
        for (ScanResult scanResult : scanResultList) {
            if (scanResult.SSID.contains("PushUpCounter"))
                espDevices.add(scanResult.SSID);
        }
        if (!espDevices.isEmpty()) {
            showAlert(getContext().getString(R.string.dialog_select_remote_device_title), null)
                    .setSingleChoiceItems(espDevices.toArray(new String[0]), -1, (dialogInterface, i) -> {
                        dialogInterface.dismiss();
                        selectedSSID = espDevices.get(i);
                        showProgress(getContext().getString(R.string.dialog_connection_title), getContext().getString(R.string.dialog_connection_title_description));
                        wifiModule.connectWifi(selectedSSID, "1234567890", new WifiModule.ConnectCallback() {
                            @Override
                            public void onSuccess() {
                                progressDialog.dismiss();
                            }

                            @Override
                            public void onFailed() {
                                progressDialog.dismiss();
                                new Handler(Looper.getMainLooper())
                                        .post(() -> {
                                            setSummary(getContext().getString(R.string.select_remote_device_preference_summary));
                                            showAlert(getContext().getString(R.string.dialog_failed_connect_title), getContext().getString(R.string.dialog_failed_connect_title_description))
                                                    .setPositiveButton(getContext().getString(R.string.ok), ((dialogInterface1, i1) -> dialogInterface1.dismiss())).show();
                                        });
                            }
                        });
                    }).setPositiveButton(getContext().getString(R.string.cancel), ((dialogInterface, i) -> dialogInterface.dismiss())).show();
        } else {
            showAlert(
                    getContext().getString(R.string.dialog_remote_device_not_found_title),
                    getContext().getString(R.string.dialog_remote_device_not_found_title_description)
            ).setPositiveButton(getContext().getString(R.string.ok), ((dialogInterface, i) -> dialogInterface.dismiss())).show();
        }
    }

    @Override
    public void onFailed(WifiModule.ScanFailed reason) {
        progressDialog.dismiss();
        switch (reason) {
            case WIFI_DISABLED:
                showAlert(getContext().getString(R.string.dialog_failed_scan_wifi_title), getContext().getString(R.string.dialog_failed_scan_wifi_title_description))
                        .setNeutralButton(getContext().getString(R.string.cancel), ((dialogInterface, i) -> dialogInterface.dismiss()))
                        .setPositiveButton(getContext().getString(R.string.enable), (dialogInterface, i) -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                getContext().startActivity(new Intent(Settings.Panel.ACTION_WIFI));
                            } else {
                                getContext().startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                            }
                        }).show();
                break;
            case LOCATION_PERMISSION_DENIED:
                showAlert(getContext().getString(R.string.dialog_failed_scan_wifi_title), getContext().getString(R.string.dialog_failed_scan_wifi_location_permission))
                        .setNeutralButton(getContext().getString(R.string.cancel), ((dialogInterface, i) -> dialogInterface.dismiss()))
                        .setPositiveButton(getContext().getString(R.string.grant), (dialogInterface, i) -> {
                            dialogInterface.dismiss();
                            PermissionWrapper.requestPermissions(getContext(), new PermissionWrapper.Callback() {
                                @Override
                                public void onGranted() {
                                    wifiModule.scanWifi(WifiDialogPreference.this);
                                }

                                @Override
                                public void onDenied() {
                                    showAlert(getContext().getString(R.string.dialog_failed_scan_wifi_title), getContext().getString(R.string.dialog_failed_scan_wifi_location_permission))
                                            .setNeutralButton(getContext().getString(R.string.cancel), ((dialogInterface, i) -> dialogInterface.dismiss()))
                                            .setPositiveButton(getContext().getString(R.string.settings_title), ((dialogInterface1, i1) -> {
                                                dialogInterface.dismiss();
                                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                                Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
                                                intent.setData(uri);
                                                getContext().startActivity(intent);
                                            })).show();
                                }
                            }, Manifest.permission.ACCESS_FINE_LOCATION);
                        })
                        .show();
                break;
            case LOCATION_DISABLED:
                showAlert(getContext().getString(R.string.dialog_failed_scan_wifi_title), getContext().getString(R.string.dialog_failed_scan_wifi_location_disabled))
                        .setNeutralButton(getContext().getString(R.string.cancel), ((dialogInterface, i) -> dialogInterface.dismiss()))
                        .setPositiveButton(getContext().getString(R.string.settings_title), (dialogInterface, i) -> {
                            dialogInterface.dismiss();
                            getContext().startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }).show();
                break;
        }
    }


}
