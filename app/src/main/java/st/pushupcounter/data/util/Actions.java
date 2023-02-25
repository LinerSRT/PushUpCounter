package st.pushupcounter.data.util;

import androidx.annotation.NonNull;

import st.pushupcounter.CounterApplication;

public enum Actions {
    COUNTER_SET_CHANGE,
    SELECT_COUNTER;

    @NonNull
    public String getActionName() {
        return String.format("%s.%s", CounterApplication.getContext().getPackageName(), this);
    }
}
