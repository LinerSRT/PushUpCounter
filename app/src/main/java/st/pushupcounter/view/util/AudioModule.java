package st.pushupcounter.view.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author : "Line'R"
 * @mailto : serinity320@mail.com
 * @created : 27.01.2023, пятница
 **/
public class AudioModule {
    private static AudioModule instance;
    private boolean isWiredHeadsetPlugged;
    private AudioRoute currentAudioRoute;
    private final Context context;
    private final BroadcastReceiver headsetReceiver;
    private final List<Callback> callbackList;

    public static AudioModule getInstance(Context context) {
        if (instance == null)
            return instance = new AudioModule(context);
        return instance;
    }

    private AudioModule(Context context) {
        this.context = context;
        this.currentAudioRoute = AudioRoute.SPEAKER;
        this.callbackList = new ArrayList<>();
        this.headsetReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean isChanged = false;
                if (intent.hasExtra("state")) {
                    int state = intent.getIntExtra("state", 0);
                    if (isWiredHeadsetPlugged && state == 0) {
                        isWiredHeadsetPlugged = false;
                        isChanged = true;
                    } else if (!isWiredHeadsetPlugged && state > 0) {
                        isWiredHeadsetPlugged = true;
                        isChanged = true;
                    }
                }
                if (isChanged) {
                    for (Callback callback : callbackList)
                        callback.onHeadsetStateChanged(isWiredHeadsetPlugged);
                }
            }
        };
            this.context.registerReceiver(headsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));

    }

    public void setCurrentAudioRoute(AudioRoute audioRoute) {
        this.currentAudioRoute = audioRoute;
        for (Callback callback : callbackList)
            callback.onAudioRouteChanged(currentAudioRoute);
    }

    public void updateCurrentAudioRoute() {
        for (Callback callback : callbackList)
            callback.onAudioRouteChanged(currentAudioRoute);
    }

    public void updateHeadsetState() {
        for (Callback callback : callbackList)
            callback.onHeadsetStateChanged(isWiredHeadsetPlugged);

    }

    public void toggleAudioRoute() {
        setCurrentAudioRoute(currentAudioRoute == AudioRoute.SPEAKER ? AudioRoute.HEADSET : AudioRoute.SPEAKER);
    }

    public AudioRoute getCurrentAudioRoute() {
        return currentAudioRoute;
    }

    public boolean isWiredHeadsetPlugged() {
        return isWiredHeadsetPlugged;
    }

    public void attach(Callback callback) {
        callbackList.add(callback);
    }

    public void remove(Callback callback) {
        callbackList.remove(callback);
    }

    public void resume() {
        for (Callback callback : callbackList) {
            callback.onHeadsetStateChanged(isWiredHeadsetPlugged);
            callback.onAudioRouteChanged(currentAudioRoute);
        }
    }

    public interface Callback {
        void onHeadsetStateChanged(boolean plugged);

        void onAudioRouteChanged(AudioRoute audioRoute);
    }
}
