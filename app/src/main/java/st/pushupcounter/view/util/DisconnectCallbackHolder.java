package st.pushupcounter.view.util;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * @author : "Line'R"
 * @mailto : serinity320@mail.com
 * @created : 20.02.2023, понедельник
 **/
@RequiresApi(Build.VERSION_CODES.Q)
public class DisconnectCallbackHolder {
    @Nullable
    private static volatile DisconnectCallbackHolder sInstance;
    @Nullable
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    @Nullable
    private ConnectivityManager mConnectivityManager;

    private boolean isNetworkcallbackAdded;

    private boolean isProcessBoundToNetwork;

    private DisconnectCallbackHolder() {
    }

    public static DisconnectCallbackHolder getInstance() {
        if (sInstance == null) {
            synchronized (DisconnectCallbackHolder.class) {
                if (sInstance == null) {
                    sInstance = new DisconnectCallbackHolder();
                }
            }
        }
        return sInstance;
    }


    public void addNetworkCallback(@NonNull ConnectivityManager.NetworkCallback networkCallback, @NonNull ConnectivityManager connectivityManager) {
        mNetworkCallback = networkCallback;
        mConnectivityManager = connectivityManager;
        isNetworkcallbackAdded = true;
    }

    public void disconnect() {
        if (mNetworkCallback != null && mConnectivityManager != null) {
            mConnectivityManager.unregisterNetworkCallback(mNetworkCallback);
            mNetworkCallback = null;
            isNetworkcallbackAdded = false;
        }
    }

    public void requestNetwork(NetworkRequest networkRequest) {
        if (mNetworkCallback != null && mConnectivityManager != null) {
            mConnectivityManager.requestNetwork(networkRequest, mNetworkCallback);
        }
    }


    public void unbindProcessFromNetwork() {
        if (mConnectivityManager != null) {
            mConnectivityManager.bindProcessToNetwork(null);
            isProcessBoundToNetwork = false;
        }
    }


    public void bindProcessToNetwork(@NonNull Network network) {

        if (mConnectivityManager != null) {
            mConnectivityManager.bindProcessToNetwork(network);
            isProcessBoundToNetwork = true;
        }
    }


    public boolean isNetworkcallbackAdded() {
        return isNetworkcallbackAdded;
    }

    public boolean isProcessBoundToNetwork() {
        return isProcessBoundToNetwork;
    }
}
