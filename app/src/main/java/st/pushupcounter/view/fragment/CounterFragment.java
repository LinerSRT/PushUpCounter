package st.pushupcounter.view.fragment;

import static android.content.Context.SENSOR_SERVICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import st.pushupcounter.CounterApplication;
import st.pushupcounter.R;
import st.pushupcounter.data.exception.MissingCounterException;
import st.pushupcounter.data.model.IntegerCounter;
import st.pushupcounter.domain.repository.CounterRepository;
import st.pushupcounter.domain.util.SharedPrefKeys;
import st.pushupcounter.view.activity.MainActivity;
import st.pushupcounter.view.fragment.dialog.DeleteDialog;
import st.pushupcounter.view.fragment.dialog.DonateDialog;
import st.pushupcounter.view.fragment.dialog.EditDialog;
import st.pushupcounter.view.util.AudioModule;
import st.pushupcounter.view.util.AudioRoute;
import st.pushupcounter.view.util.Flashlight;
import st.pushupcounter.websocket.WebSocketClient;
import st.pushupcounter.websocket.WifiStatusReceiver;

@SuppressWarnings("ConstantConditions")
public class CounterFragment extends Fragment implements SensorEventListener, AudioModule.Callback {

    private static final String TAG = CounterFragment.class.getSimpleName();
    public static final String COUNTER_NAME_ATTRIBUTE = "COUNTER_NAME";
    public static final int DEFAULT_VALUE = 0;
    public static final long DEFAULT_TIME = 0L;
    private static final long DEFAULT_VIBRATION_DURATION = 50; // Milliseconds
    private static final long DEFAULT_PHYSICAL_BUTTON_DELAY = 500;
    private static final String STATE_SELECTED_COUNTER_NAME = "SELECTED_COUNTER_NAME";

    /**
     * Name is used as a key to look up and modify counter value in storage.
     */
    private String name;

    private MenuItem soundMenuItem;

    private SharedPreferences sharedPrefs;
    private Vibrator vibrator;
    private SensorManager sensorManager;
    private Sensor sensor;
    private TextView counterLabel;
    private Button incrementButton;
    private Button decrementButton;
    // Флаги нужны, чтобы реализовать логику срабатываний при однократных и длительных нажатий физических клавиш
    private Boolean onKeyPressFlag = false;
    private Boolean onKeyPressFlag2 = false;
    private Boolean isStopwatchStarted = false;
    private Boolean isProximitySensorTriggered = false;
    private long startTime = 0L;
    //    private final long startSensorProximityTime = 0L;
    private long lastPressButton = 0L;

    private TextView chronometer;
    private final Handler customHandler = new Handler();

    private IntegerCounter counter;

    private MediaPlayer incrementSoundPlayer;
    private MediaPlayer decrementSoundPlayer;

    private TextToSpeech textToSpeech;
    private boolean textToSpeechReady;

    private Flashlight flashlight;


    private WebSocketClient webSocket;
    private WifiStatusReceiver wifiStatusReceiver;
    private boolean wasConnected;

    private void restoreSavedState(@Nullable final Bundle savedInstanceState) {
        if (savedInstanceState == null) return;

        this.name = savedInstanceState.getString(STATE_SELECTED_COUNTER_NAME);
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        restoreSavedState(savedInstanceState);


        CounterApplication.audioModule.attach(this);

        webSocket = new WebSocketClient(URI.create(CounterApplication.WS_ADDRESS)) {
            @Override
            public void onOpen(WebSocketClient webSocketClient) {
                wasConnected = true;
                webSocketClient.send("connect");
            }

            @Override
            public void onTextReceived(WebSocketClient webSocketClient, String message) {
                int currentState = Integer.parseInt(message);
                if (currentState == 1 && sharedPrefs.getBoolean(SharedPrefKeys.WIFI_RELAY_ON.getName(), false)) {
                    new Handler(Looper.getMainLooper()).post(increment);
                }
                Log.d("TAGTAG", "onTextReceived: " + message);
            }

            @Override
            public void onException(Exception e) {
                wasConnected = false;
            }

            @Override
            public void onCloseReceived(WebSocketClient webSocketClient, int reason, String description) {
                wasConnected = false;
                Log.d("TAGTAG", String.format("Close socket: %s, %s", reason, description));
            }
        };
        wifiStatusReceiver = new WifiStatusReceiver(new WifiStatusReceiver.WifiStatusListener() {
            @Override
            public void onWifiChanged(String SSID) {
                if (wifiStatusReceiver.isConnected("PushUpCounter")) {
                    if (!webSocket.isRunning())
                        webSocket.connect();
                } else {
                    if (wasConnected) {
                        wasConnected = false;
                        webSocket.close(0, WebSocketClient.CLOSE_CODE_NORMAL, "Force close");
                        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(CounterApplication.getContext(), CounterApplication.getContext().getString(R.string.toast_remote_device_disconnected_network_change), Toast.LENGTH_SHORT).show());
                    }
                }
            }


            @Override
            public void onWifiDisconnected() {
                if (wasConnected) {
                    wasConnected = false;
                    if (webSocket.isRunning())
                        webSocket.close(0, WebSocketClient.CLOSE_CODE_NORMAL, "Force close");
                }
            }
        });
        wifiStatusReceiver.setListening(true);

        flashlight = new Flashlight(getContext());
        vibrator = (Vibrator) requireActivity().getSystemService(Context.VIBRATOR_SERVICE);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireActivity());
        initCounter();
        textToSpeech = new TextToSpeech(getContext(), status -> {
            Log.d("TTS", "TextToSpeech.OnInitListener.onInit...");
            setupTextToSpeech();
        }, "com.google.android.tts");
        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroy() {
        CounterApplication.audioModule.remove(this);
        super.onDestroy();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.counter, container, false);

        incrementButton = view.findViewById(R.id.incrementButton);
        incrementButton.setOnClickListener(v -> increment.run());

        decrementButton = view.findViewById(R.id.decrementButton);
        decrementButton.setOnClickListener(v -> decrement());

        counterLabel = view.findViewById(R.id.counterLabel);
        counterLabel.setOnClickListener(
                v -> {
                    if (sharedPrefs.getBoolean(SharedPrefKeys.LABEL_CONTROL_ON.getName(), true)) {
                        increment.run();
                    }
                });

        chronometer = view.findViewById(R.id.chronometer); // Нахожу секундомер на View и устанавливаю стандартное значение
        if (chronometer != null) chronometer.setText(counter.getTimerValue());

        Button startButton = view.findViewById(R.id.startButton);
        if (startButton != null) startButton.setOnClickListener(v -> startChronometer());

        Button stopButton = view.findViewById(R.id.stopButton);
        if (stopButton != null) stopButton.setOnClickListener(v -> stopChronometer());

        invalidateUI();
        return view;
    }


    private void setupTextToSpeech() {
//    Locale language = new Locale(Locale.getDefault().getLanguage());

        //Initial setup using speakers as route to play sound
        AudioRoute.SPEAKER.update(textToSpeech);
        String speechLanguage = sharedPrefs.getString(SharedPrefKeys.SPEECH_LANGUAGE.getName(), "ru");
        if (!speechLanguage.equals("no")) {
            Locale language = new Locale(speechLanguage);
            int result = textToSpeech.setLanguage(language);
            if (result == TextToSpeech.LANG_MISSING_DATA) {
                Toast.makeText(getActivity(), getResources().getText(R.string.toast_unable_to_speech_on), Toast.LENGTH_SHORT).show(); // если устройство не поддерживает озвучку, вывожу сообщение
                textToSpeechReady = false;
            } else if (result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(getActivity(), getResources().getText(R.string.toast_unable_to_speech_on), Toast.LENGTH_SHORT).show(); // если устройство не поддерживает озвучку, вывожу сообщение
                textToSpeechReady = false;
            } else {
                textToSpeechReady = true;
            }
        } else {
            textToSpeechReady = false;
        }
    }


    private void initCounter() {
        final CounterRepository<IntegerCounter> storage = CounterApplication.getComponent().localStorage();
        try {
            final String requestedCounter = requireArguments().getString(COUNTER_NAME_ATTRIBUTE);
            if (requestedCounter == null) {
                this.counter = storage.readAll(true).get(0);
                return;
            }
            this.counter = storage.read(requestedCounter);
        } catch (MissingCounterException e) {
            Log.w(TAG, "Unable to find provided counter. Retrieving a different one.", e);
            this.counter = storage.readAll(true).get(0);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle savedInstanceState) {
        savedInstanceState.putString(STATE_SELECTED_COUNTER_NAME, this.name);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        registerProximitySensor();
        //Recreate media player instances after app is restarted, resumed...
        CounterApplication.audioModule.resume();
        //if (wifiStatusReceiver.isConnected("PushUpCounter")) {
        //    webSocket.close(0, WebSocketClient.CLOSE_CODE_NORMAL, "Resume close");
        //    webSocket.connect();
        //}
    }

    @Override
    public void onPause() {
        super.onPause();
        //webSocket.close(0, WebSocketClient.CLOSE_CODE_NORMAL, "Pause Fragment");
        if (isStopwatchStarted)
            stopChronometer();
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        unregisterProximitySensor();
        if (incrementSoundPlayer != null && incrementSoundPlayer.isPlaying()) {
            incrementSoundPlayer.stop();
            incrementSoundPlayer.release();
        }
        if (decrementSoundPlayer != null && decrementSoundPlayer.isPlaying()) {
            decrementSoundPlayer.stop();
            decrementSoundPlayer.release();
        }
    }

    private void startChronometer() {
        if (!isStopwatchStarted) {
            counter.startChronometer();
            customHandler.post(updateTimerThread);
            vibrate(DEFAULT_VIBRATION_DURATION + 20);
            isStopwatchStarted = true;

            saveValue.run();
        }
    }

    private void stopChronometer() {
        counter.stopChronometer();
        customHandler.removeCallbacks(updateTimerThread);
        vibrate(DEFAULT_VIBRATION_DURATION + 20);
        isStopwatchStarted = false;

        saveValue.run();
    }

    private final Runnable updateTimerThread = new Runnable() {
        public void run() {
            counter.setTimeInMilliseconds(SystemClock.uptimeMillis() - counter.getStartTime());
            long updatedTime = counter.getTimeSwapBuff() + counter.getTimeInMilliseconds();

            if (updatedTime >= 3599999) {
                stopChronometer();
                return;
            }
            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            int milliseconds = (int) (updatedTime % 1000) / 10;
            counter.setTimerValue(String.format(Locale.getDefault(), "%02d:%02d.%02d", mins, secs, milliseconds));
            chronometer.setText(counter.getTimerValue());
            customHandler.postDelayed(this, 0);
        }
    };

    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu, @NonNull final MenuInflater inflater) {
        inflater.inflate(R.menu.counter_menu, menu);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull final Menu menu) {
        MainActivity activity = (MainActivity) requireActivity();
        boolean isDrawerOpen = activity.isNavigationOpen();

        soundMenuItem = menu.findItem(R.id.menu_sound);
        soundMenuItem.setVisible(CounterApplication.audioModule.isWiredHeadsetPlugged() && !isDrawerOpen);
        soundMenuItem.setIcon(CounterApplication.audioModule.getCurrentAudioRoute() == AudioRoute.SPEAKER ? R.drawable.ic_volume_up : R.drawable.ic_headphones);

        MenuItem editItem = menu.findItem(R.id.menu_edit);
        editItem.setVisible(!isDrawerOpen);

        MenuItem deleteItem = menu.findItem(R.id.menu_delete);
        deleteItem.setVisible(!isDrawerOpen);

        MenuItem donateItem = menu.findItem(R.id.menu_donate);
        donateItem.setVisible(!isDrawerOpen);

        MenuItem resetItem = menu.findItem(R.id.menu_reset);
        resetItem.setVisible(!isDrawerOpen);
    }

    public boolean onKeyLongPress(int keyCode) {
        Log.d(TAG, "onKeyUp: ");
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:

            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (sharedPrefs.getBoolean(SharedPrefKeys.HARDWARE_BTN_CONTROL_ON.getName(), true)) {
                    onKeyPressFlag2 = true;
                    return true;
                }
                return false;

            default:
                return false;
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (sharedPrefs.getBoolean(SharedPrefKeys.HARDWARE_BTN_CONTROL_ON.getName(), true)) {
                    event.startTracking();
                    if (!onKeyPressFlag)
                        startTime = SystemClock.uptimeMillis();
                    onKeyPressFlag = true;
                    return true;
                }
                return false;
            default:
                return false;
        }
    }

    public void onHeadsetStateChange() {
        invalidateUI();
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (SystemClock.uptimeMillis() - lastPressButton < DEFAULT_PHYSICAL_BUTTON_DELAY)
            return true;

        switch (keyCode) {
            case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_VOLUME_UP:
                return onVolumeUp(event);
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return onVolumeDown(event);
            default:
                return false;
        }
    }

    public boolean onVolumeUp(KeyEvent event) {
        if (sharedPrefs.getBoolean(SharedPrefKeys.HARDWARE_BTN_CONTROL_ON.getName(), true)) {
            event.startTracking();
            if (onKeyPressFlag && !onKeyPressFlag2)
                increment.run();
            else {
                long different = SystemClock.uptimeMillis();
                different -= startTime;
                if (different < 3000)
                    increment.run();
            }
            onKeyPressFlag = false;
            onKeyPressFlag2 = false;
            return true;
        }

        return false;
    }

    public boolean onVolumeDown(KeyEvent event) {
        if (sharedPrefs.getBoolean(SharedPrefKeys.HARDWARE_BTN_CONTROL_ON.getName(), true)) {
            event.startTracking();
            if (onKeyPressFlag && !onKeyPressFlag2)
                decrement();
            else {
                long different = SystemClock.uptimeMillis();
                different -= startTime;
                if (different < 3000)
                    decrement();
            }
            onKeyPressFlag = false;
            onKeyPressFlag2 = false;
            return true;
        }

        return false;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sound:
                toggleSoundDevice();
                return true;
            case R.id.menu_reset:
                showResetConfirmationDialog();
                return true;
            case R.id.menu_edit:
                showEditDialog();
                return true;
            case R.id.menu_delete:
                showDeleteDialog();
                return true;
            case R.id.menu_donate:
                showDonateDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void toggleSoundDevice() {
        CounterApplication.audioModule.toggleAudioRoute();
        invalidateUI();
    }

    private void showResetConfirmationDialog() {
        final Dialog dialog =
                new AlertDialog.Builder(getActivity())
                        .setMessage(getResources().getText(R.string.dialog_reset_title))
                        .setCancelable(false)
                        .setPositiveButton(
                                getResources().getText(R.string.dialog_button_reset), (d, id) -> reset())
                        .setNegativeButton(getResources().getText(R.string.dialog_button_cancel), null)
                        .create();
        Objects.requireNonNull(dialog.getWindow())
                .setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();
    }

    private void showEditDialog() {
        final EditDialog dialog = EditDialog.newInstance(counter.getName(), counter.getValue(), counter.getTimerValue());
        dialog.show(getParentFragmentManager(), EditDialog.TAG);
    }

    private void showDeleteDialog() {
        final DeleteDialog dialog = DeleteDialog.newInstance(counter.getName());
        dialog.show(getParentFragmentManager(), DeleteDialog.TAG);
    }

    private void showDonateDialog() {
        final DonateDialog dialog = new DonateDialog();
        dialog.show(getParentFragmentManager(), DeleteDialog.TAG);
    }

    // Озвучивание текста
    private void speakOut(String toSpeak) {
        if (textToSpeechReady) {
            // A random String (Unique ID).
            String utteranceId = UUID.randomUUID().toString();
            textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        }
    }

    private final Runnable increment = new Runnable() {
        public void run() {
            // Если включена инверсия, значение уменьшается, а не повышается
            if (sharedPrefs.getBoolean(SharedPrefKeys.INVERSE_COUNTER_ON.getName(), false)) {
                counter.decrement();
                lastPressButton = SystemClock.uptimeMillis();
                // Подача сигнала на wifi реле esp 01
                if (sharedPrefs.getBoolean(SharedPrefKeys.WIFI_RELAY_ON.getName(), false))
                    webSocket.send("switch_relay");

            } else {
                counter.increment();
            }
            vibrate(DEFAULT_VIBRATION_DURATION);
            if (sharedPrefs.getBoolean(SharedPrefKeys.FLASHLIGHT.getName(), false))
                flashlight.blink();

            //Если включена инверсия, включаем озвучку уменьшения
            if (sharedPrefs.getBoolean(SharedPrefKeys.INVERSE_COUNTER_ON.getName(), false)) {
                // цокания
                playSound(decrementSoundPlayer);
                // Если озвучивание слова "ошибка" включено, то озвучивать значение счётчика не нужно
                if (sharedPrefs.getBoolean(SharedPrefKeys.SPEECH_ERROR_ON.getName(), true)) {
                    //Озвучиваем слово "Ошибка" в зависимости от выбранного языка в настройках

                    String speechLanguage = sharedPrefs.getString(SharedPrefKeys.SPEECH_LANGUAGE.getName(), "ru");
                    if (!speechLanguage.equals("no")) {
                        speakOut(getStringByLocal(requireActivity(), R.string.error, speechLanguage));
                    }

                } else {
                    speakOut(String.valueOf(counter.getValue()));
                }
            } else {
                // Если инверсии нет
                playSound(incrementSoundPlayer);
                speakOut(String.valueOf(counter.getValue()));
            }

            // Запуск секундомера вместе со счётчиком, если не включена инверсия
            if (counter.isStop() && sharedPrefs.getBoolean(SharedPrefKeys.AUTO_STOPWATCH_ON.getName(), true) && !sharedPrefs.getBoolean(SharedPrefKeys.INVERSE_COUNTER_ON.getName(), false))
                startChronometer();

            // Обновить интерфейс и сохранить значение в памяти
            invalidateUI();
            saveValue.run();
        }
    };

    private void decrement() {
        // Если включена инверсия, значение увеличивается, а не уменьшается
        if (sharedPrefs.getBoolean(SharedPrefKeys.INVERSE_COUNTER_ON.getName(), false)) {
            counter.increment();
        } else {
            // Подача сигнала на wifi реле esp 01
            if (sharedPrefs.getBoolean(SharedPrefKeys.WIFI_RELAY_ON.getName(), false))
                webSocket.send("switch_relay");
            counter.decrement();
            lastPressButton = SystemClock.uptimeMillis();
        }
        vibrate(DEFAULT_VIBRATION_DURATION + 20);
        if (sharedPrefs.getBoolean(SharedPrefKeys.FLASHLIGHT.getName(), false))
            flashlight.blink();
        //Если включена инверсия, включаем озвучку увеличения
        if (sharedPrefs.getBoolean(SharedPrefKeys.INVERSE_COUNTER_ON.getName(), false)) {
            // цокания
            playSound(incrementSoundPlayer);
            speakOut(String.valueOf(counter.getValue()));
        } else {
            // цокания
            playSound(decrementSoundPlayer);
            // Если озвучивание слова "ошибка" включено, то озвучивать значение счётчика не нужно
            if (sharedPrefs.getBoolean(SharedPrefKeys.SPEECH_ERROR_ON.getName(), true)) {
                //Озвучиваем слово "Ошибка" в зависимости от выбранного языка в настройках
                speakOut(getStringByLocal(requireActivity(), R.string.error, sharedPrefs.getString(SharedPrefKeys.SPEECH_LANGUAGE.getName(), "ru")));
            } else {
                speakOut(String.valueOf(counter.getValue()));
            }
        }
        // Запуск секундомера вместе со счётчиком, если включена инверсия
        if (counter.isStop() && sharedPrefs.getBoolean(SharedPrefKeys.AUTO_STOPWATCH_ON.getName(), true) && sharedPrefs.getBoolean(SharedPrefKeys.INVERSE_COUNTER_ON.getName(), false))
            startChronometer();
        // Обновить интерфейс и сохранить значение в памяти
        invalidateUI();
        saveValue.run();
    }

    private void reset() {
        // Останавливаем всё и обнуляем значения
        try {
            stopChronometer();
            counter.reset();
        } catch (Exception e) {
            Log.getStackTraceString(e);
            throw new RuntimeException(e);
        }

        // Обновить интерфейс и сохранить значение в памяти
        invalidateUI();
        saveValue.run();
    }

    /**
     * Updates UI elements of the fragment based on current value of the counter.
     */
    @SuppressLint("SetTextI18n")
    public void invalidateUI() {
        if (counter != null) {
            counterLabel.setText(Integer.toString(counter.getValue()));
            incrementButton.setEnabled(counter.getValue() < IntegerCounter.MAX_VALUE);
            decrementButton.setEnabled(counter.getValue() > IntegerCounter.MIN_VALUE);
            chronometer.setText(counter.getTimerValue());
        }
    }

    private final Runnable saveValue = new Runnable() {
        public void run() {
            final CounterRepository<IntegerCounter> storage = CounterApplication.getComponent().localStorage();
            storage.write(counter);
        }
    };

    /**
     * Triggers vibration for a specified duration, if vibration is turned on.
     */
    private void vibrate(long duration) {
        if (sharedPrefs.getBoolean(SharedPrefKeys.VIBRATION_ON.getName(), true)) {
            try {
                vibrator.vibrate(duration);
            } catch (Exception e) {
                Log.e(TAG, "Unable to vibrate", e);
            }
        }
    }

    /**
     * Plays sound if sounds are turned on.
     */
    private void playSound(@NonNull final MediaPlayer soundPlayer) {
        if (sharedPrefs.getBoolean(SharedPrefKeys.SOUNDS_ON.getName(), true)) {
            try {
                soundPlayer.start();
            } catch (Exception e) {
                Log.e(TAG, "Unable to play sound", e);
            }
        }
    }

    private void registerProximitySensor() {
        if (sharedPrefs.getBoolean(SharedPrefKeys.PROXIMITY_SENSOR_ON.getName(), false)) {
            sensorManager = (SensorManager) requireActivity().getSystemService(SENSOR_SERVICE);
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            if (sensor != null)
                sensorManager.registerListener(this, sensor, 4 * 100 * 1000);
            else
                Log.i(TAG, "onCreate: Датчик приближения недоступен");
        }
    }

    private void unregisterProximitySensor() {
        if (sensorManager != null)
            sensorManager.unregisterListener(this, sensor);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // При срабатывании датчика приближения
        if (sensorEvent.values[0] < sensor.getMaximumRange()) {
            isProximitySensorTriggered = true;
        } else if (sensorEvent.values[0] >= sensor.getMaximumRange() && isProximitySensorTriggered) {
            increment.run();
            isProximitySensorTriggered = false;
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @NonNull
    public static String getStringByLocal(Activity context, int id, String locale) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(new Locale(locale));
        return context.createConfigurationContext(configuration).getResources().getString(id);
    }

    @Override
    public void onHeadsetStateChanged(boolean plugged) {
        if (soundMenuItem != null)
            soundMenuItem.setVisible(plugged);
    }

    @Override
    public void onAudioRouteChanged(AudioRoute audioRoute) {
        if (soundMenuItem != null)
            soundMenuItem.setIcon(audioRoute == AudioRoute.SPEAKER ? R.drawable.ic_volume_up : R.drawable.ic_headphones);
        new Thread(() -> {
            if (textToSpeech != null)
                audioRoute.update(textToSpeech);
            incrementSoundPlayer = audioRoute.recreate(CounterApplication.getContext(), null, R.raw.increment_sound);
            decrementSoundPlayer = audioRoute.recreate(CounterApplication.getContext(), null, R.raw.decrement_sound);
        }).start();
    }
}
