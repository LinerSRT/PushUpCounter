package st.pushupcounter.domain.util;

import androidx.annotation.NonNull;

public enum SharedPrefKeys {
    ACTIVE_COUNTER("activeKey"),
    KEEP_SCREEN_ON("keepScreenOn"),
    LABEL_CONTROL_ON("labelControlOn"),
    HARDWARE_BTN_CONTROL_ON("hardControlOn"),
    VIBRATION_ON("vibrationOn"),
    WIFI_RELAY_ON("wifiRelay"),
    SELECT_REMOTE_DEVICE("espSelectedSSID"),
    SOUNDS_ON("soundsOn"),
    THEME("theme"),
    AUTO_STOPWATCH_ON("autoStopwatch"),
    INVERSE_COUNTER_ON("inverseCounter"),
    PROXIMITY_SENSOR_ON("proximitySensor"),
    SPEECH_ERROR_ON("SpeechErrorOn"),
    FLASHLIGHT("flashLightKey"),
    SPEECH_LANGUAGE("speechLanguage");

    @NonNull
    private final String name;

    SharedPrefKeys(@NonNull final String name) {
        this.name = name;
    }

    @NonNull
    public String getName() {
        return name;
    }
}
