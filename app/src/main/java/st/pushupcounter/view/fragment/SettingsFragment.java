package st.pushupcounter.view.fragment;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.CheckBoxPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceFragmentCompat;

import st.pushupcounter.R;
import st.pushupcounter.domain.util.SharedPrefKeys;
import st.pushupcounter.view.activity.SettingsActivity;

public final class SettingsFragment extends PreferenceFragmentCompat {

    private static final String TAG = SettingsFragment.class.getSimpleName();

    private OnPreferenceClickListener onRemoveCountersClickListener;
    private OnPreferenceClickListener onExportClickListener;
    private String appVersion;
    private String theme;

    public void setOnRemoveCountersClickListener(
            final @NonNull OnPreferenceClickListener onRemoveCountersClickListener) {
        this.onRemoveCountersClickListener = onRemoveCountersClickListener;
    }

    public void setOnExportClickListener(
            final @NonNull OnPreferenceClickListener onExportClickListener) {
        this.onExportClickListener = onExportClickListener;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {

            // Setting summaries for necessary preferences
            findPreference(SharedPrefKeys.THEME.getName()).setSummary(theme);
            findPreference(SettingsActivity.KEY_VERSION).setSummary(appVersion);
            findPreference(SettingsActivity.KEY_REMOVE_COUNTERS).setOnPreferenceClickListener(onRemoveCountersClickListener);
            findPreference(SettingsActivity.KEY_EXPORT_COUNTERS).setOnPreferenceClickListener(onExportClickListener);
            findPreference(SharedPrefKeys.FLASHLIGHT.getName()).setEnabled(getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH));
            Preference espPreference = findPreference(SharedPrefKeys.SELECT_REMOTE_DEVICE.getName());
            espPreference.setVisible(getPreferenceManager().getSharedPreferences().getBoolean(SharedPrefKeys.WIFI_RELAY_ON.getName(), false));
            findPreference(SharedPrefKeys.WIFI_RELAY_ON.getName()).setOnPreferenceClickListener(preference -> {
                CheckBoxPreference checkBoxPreference = (CheckBoxPreference) preference;
                espPreference.setVisible(checkBoxPreference.isChecked());
                return true;
            });
        } catch (NullPointerException e) {
            Log.e(TAG, "Unable to retrieve one of the preferences", e);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
    }
}
