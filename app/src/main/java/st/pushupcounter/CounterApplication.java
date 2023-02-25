package st.pushupcounter;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import javax.inject.Inject;

import dagger.android.DispatchingAndroidInjector;
import st.pushupcounter.di.AppComponent;
import st.pushupcounter.di.CounterModule;
import st.pushupcounter.di.DaggerAppComponent;
import st.pushupcounter.view.util.AudioModule;

public class CounterApplication extends Application {
    public static final String WS_ADDRESS = "ws://192.168.19.91:81";
    public static AudioModule audioModule;
    private static Context context;

    @Inject
    DispatchingAndroidInjector<Activity> dispatchingAndroidInjector;

    private static AppComponent component;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
        audioModule = AudioModule.getInstance(this);
        component = DaggerAppComponent.builder().counterModule(new CounterModule(this)).build();
    }

    public static Context getContext() {
        return context;
    }

    public static AppComponent getComponent() {
        return component;
    }
}
