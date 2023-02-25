package st.pushupcounter.di;

import androidx.annotation.NonNull;

import dagger.Module;
import dagger.Provides;

import javax.inject.Singleton;

import st.pushupcounter.CounterApplication;
import st.pushupcounter.R;
import st.pushupcounter.data.model.IntegerCounter;
import st.pushupcounter.data.util.BroadcastHelper;
import st.pushupcounter.domain.repository.CounterRepository;
import st.pushupcounter.data.repository.CounterRepositoryImpl;

@Module
public class CounterModule {

    private final CounterApplication app;

    public CounterModule(CounterApplication app) {
        this.app = app;
    }

    @Provides
    @Singleton
    CounterApplication provideApp() {
        return app;
    }

    @Provides
    @Singleton
    CounterRepository<IntegerCounter> provideCounterRepository(final @NonNull CounterApplication app) {
        return new CounterRepositoryImpl(
                app,
                new BroadcastHelper(app),
                (String) app.getResources().getText(R.string.default_counter_name));
    }
}
