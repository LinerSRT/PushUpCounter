package st.pushupcounter.di;

import dagger.Component;
import dagger.android.AndroidInjectionModule;
import dagger.android.AndroidInjector;

import javax.inject.Singleton;

import st.pushupcounter.CounterApplication;
import st.pushupcounter.data.model.IntegerCounter;
import st.pushupcounter.domain.repository.CounterRepository;

@Singleton
@Component(modules = {AndroidInjectionModule.class, CounterModule.class})
public interface AppComponent extends AndroidInjector<CounterApplication> {

    CounterRepository<IntegerCounter> localStorage();
}
