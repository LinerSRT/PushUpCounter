package st.pushupcounter.view.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import st.pushupcounter.CounterApplication;
import st.pushupcounter.R;
import st.pushupcounter.data.exception.MissingCounterException;
import st.pushupcounter.data.model.IntegerCounter;
import st.pushupcounter.data.util.Actions;
import st.pushupcounter.data.util.BroadcastHelper;
import st.pushupcounter.domain.repository.CounterRepository;
import st.pushupcounter.domain.util.SharedPrefKeys;
import st.pushupcounter.view.fragment.CounterFragment;
import st.pushupcounter.view.fragment.CountersListFragment;
import st.pushupcounter.view.util.AudioModule;
import st.pushupcounter.view.util.AudioRoute;
import st.pushupcounter.view.util.Themes;

public class MainActivity extends AppCompatActivity implements AudioModule.Callback {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSION_CODE = 1137;
    private ActionBar actionBar;
    private DrawerLayout navigationLayout;
    private ActionBarDrawerToggle navigationToggle;
    private SharedPreferences sharedPrefs;
    private FrameLayout menuFrame;
    private String selectedCounterName;
    private CounterFragment selectedCounterFragment;
    private AudioManager audioManager;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        CounterApplication.audioModule.attach(this);
        actionBar = getSupportActionBar();
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        registerIntentReceivers(getApplicationContext());
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        super.onCreate(savedInstanceState);

        // Enable ActionBar home button to behave as action to toggle navigation drawer
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        setContentView(R.layout.drawer_layout);

        navigationLayout = findViewById(R.id.drawer_layout);
        navigationToggle = generateActionBarToggle(this.actionBar, navigationLayout);
        navigationLayout.addDrawerListener(navigationToggle);

        menuFrame = findViewById(R.id.menu_frame);


//        WifiModule wifiModule = new WifiModule(this);
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            if(wifiModule.isLocationEnabled()){
//                wifiModule.findWifi("PushUpCounter", new WifiModule.FindCallback() {
//                    @Override
//                    public void onSuccess(ScanResult scanResult) {
//                        wifiModule.connectWifi(scanResult, "12345678", new WifiModule.ConnectionCallback() {
//                            @Override
//                            public void onSuccess() {
//                                ESPWebSocket webSocket = new ESPWebSocket();
//                                webSocket.sendCommand("connect");
//                            }
//
//                            @Override
//                            public void onFailed() {
//
//                            }
//                        });
//                    }
//
//                    @Override
//                    public void onFailed() {
//
//                        Log.d(TAG, "onFailed: ");
//                    }
//                });
//            } else {
//                new AlertDialog.Builder(this)
//                        .setTitle("GPS Disabled")
//                        .setMessage("Location should be enabled for scan WiFi networks! Please enable GPS and try again later.")
//                        .setPositiveButton("Open settings", new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface paramDialogInterface, int paramInt) {
//                                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
//                            }
//                        })
//                        .setNegativeButton("Cancel",null)
//                        .show();
//            }
//
//        }
    }



    private void registerIntentReceivers(@NonNull final Context context) {
        final IntentFilter counterSelectionFilter = new IntentFilter(Actions.SELECT_COUNTER.getActionName());
        counterSelectionFilter.addCategory(Intent.CATEGORY_DEFAULT);
        context.registerReceiver(new CounterChangeReceiver(), counterSelectionFilter);
    }

    private ActionBarDrawerToggle generateActionBarToggle(
            @NonNull final ActionBar actionBar, @NonNull final DrawerLayout drawerLayout) {

        return new ActionBarDrawerToggle(
                this, drawerLayout, null, R.string.drawer_open, R.string.drawer_close) {

            public void onDrawerClosed(View view) {
                actionBar.setTitle(selectedCounterName);
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                actionBar.setTitle(getTitle());
                supportInvalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        Themes.initCurrentTheme(sharedPrefs);
        initCountersList();
        switchCounter(findActiveCounter().getName());
        if(selectedCounterFragment != null)
            selectedCounterFragment.invalidateUI();
        if (sharedPrefs.getBoolean(SharedPrefKeys.KEEP_SCREEN_ON.getName(), true)) {
            getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        CounterApplication.audioModule.resume();
    }

    private void initCountersList() {
        final FragmentTransaction transaction = this.getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.menu_frame, new CountersListFragment());
        transaction.commit();
    }

    private void switchCounter(@NonNull final String counterName) {
        this.selectedCounterName = counterName;

        final Bundle bundle = new Bundle();
        bundle.putString(CounterFragment.COUNTER_NAME_ATTRIBUTE, counterName);

        selectedCounterFragment = new CounterFragment();
        selectedCounterFragment.setArguments(bundle);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, selectedCounterFragment)
                .commitAllowingStateLoss();

        actionBar.setTitle(counterName);
        CounterApplication.audioModule.updateCurrentAudioRoute();
        CounterApplication.audioModule.updateHeadsetState();
        if (isNavigationOpen()) closeNavigation();
    }

    /**
     * Finds which counter is selected based on the following priority:
     *
     * <ol>
     *   <li>From value stored during the previous session
     *   <li>First of the stored counters <em>(default one will be generated if none are present)</em>
     * </ol>
     */
    private IntegerCounter findActiveCounter() {
        CounterRepository<IntegerCounter> storage = CounterApplication.getComponent().localStorage();

        final String savedActiveCounter =
                sharedPrefs.getString(SharedPrefKeys.ACTIVE_COUNTER.getName(), null);

        // Checking whether it still exists...
        if (savedActiveCounter != null) {
            try {
                return storage.read(savedActiveCounter);
            } catch (MissingCounterException e) {
                // No need to do anything.
            }
        }

        return storage.readAll(true).get(0);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Since there's no way to keep track of these events from the fragment side, we have to pass
        // them to it for processing.
        return super.onKeyDown(keyCode, event) || selectedCounterFragment.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return super.onKeyLongPress(keyCode, event) || selectedCounterFragment.onKeyLongPress(keyCode);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event) || selectedCounterFragment.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        navigationToggle.syncState();
    }

    @SuppressLint("ApplySharedPref")
    @Override
    protected void onPause() {
        super.onPause();
        final SharedPreferences.Editor prefsEditor = sharedPrefs.edit();
        prefsEditor.putString(SharedPrefKeys.ACTIVE_COUNTER.getName(), selectedCounterName);
        prefsEditor.commit();
    }

    @Override
    public void onConfigurationChanged(@NonNull final Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        navigationToggle.onConfigurationChanged(newConfig);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (isNavigationOpen()) closeNavigation();
                else openDrawer();
                return true;

            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean isNavigationOpen() {
        return navigationLayout.isDrawerOpen(menuFrame);
    }


    private void closeNavigation() {
        navigationLayout.closeDrawer(menuFrame);
    }

    private void openDrawer() {
        navigationLayout.openDrawer(menuFrame);
    }



    private class CounterChangeReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String counterName = intent.getStringExtra(BroadcastHelper.EXTRA_COUNTER_NAME);
            Log.d(
                    TAG,
                    String.format(
                            "Received counter change broadcast. Switching to counter \"%s\"", counterName));
            switchCounter(counterName);
        }
    }

//
//    private class HeadsetStateBroadcastReceiver extends BroadcastReceiver {
//
//        @Override
//        public void onReceive(final Context context, final Intent intent) {
//            boolean isChanged = false;
//
//            if (intent.hasExtra("state")) {
//                int state = intent.getIntExtra("state", 0);
//                if ( CounterApplication.isWiredHeadsetOn && state == 0) {
//                    CounterApplication.isWiredHeadsetOn = false;
//                    isChanged = true;
//                } else if (! CounterApplication.isWiredHeadsetOn && state > 0) {
//                    CounterApplication.isWiredHeadsetOn = true;
//                    isChanged = true;
//                }
//            }
//            if (isChanged) {
//                if(CounterApplication.audioRoute == AudioRoute.HEADSET && !CounterApplication.isWiredHeadsetOn){
//                    setAudioRoute(AudioRoute.SPEAKER);
//                } else if(CounterApplication.isWiredHeadsetOn){
//                    setAudioRoute(AudioRoute.SPEAKER);
//                } else {
//                    setAudioRoute(CounterApplication.audioRoute);
//                }
//                if(selectedCounterFragment != null) {
//                    selectedCounterFragment.onHeadsetStateChange();
//                    selectedCounterFragment.updateSoundIcon();
//                }
//            }
//        }
//    }


    @Override
    public void onHeadsetStateChanged(boolean plugged) {
        if(plugged){
            CounterApplication.audioModule.setCurrentAudioRoute(CounterApplication.audioModule.getCurrentAudioRoute() == AudioRoute.HEADSET ? AudioRoute.HEADSET : AudioRoute.SPEAKER);
        }
    }

    @Override
    public void onAudioRouteChanged(AudioRoute audioRoute) {
        audioRoute.update(audioManager);
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                audioRoute.update(audioManager);
            }
        }) ;
    }
}
