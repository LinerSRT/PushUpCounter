package st.pushupcounter.view.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import androidx.core.util.Consumer;

/**
 * @author : "Line'R"
 * @mailto : serinity320@mail.com
 * @created : 20.02.2023, понедельник
 **/
public class GenericConsumer<T> {
    @Nullable
    private final T value;

    public GenericConsumer(@Nullable T value) {
        this.value = value;
    }

    public GenericConsumer() {
        this.value = null;
    }

    public static <T> GenericConsumer<T> of(@NonNull T value) {
        return new GenericConsumer<>(value);
    }

    public static <T> GenericConsumer<T> empty() {
        return new GenericConsumer<>();
    }

    public <S> GenericConsumer<S> next(@NonNull Function<? super T, ? extends S> function) {
        return new GenericConsumer<>(value == null ? null : function.apply(value));
    }

    public T get() {
        return value;
    }

    public boolean asBoolean() {
        if (value == null)
            return false;
        if (value instanceof Boolean)
            return (Boolean) value;
        return false;
    }

    public int asInt() {
        if (value == null)
            return 0;
        if (value instanceof Integer)
            return (Integer) value;
        return 0;
    }

    public long asLong() {
        if (value == null)
            return 0;
        if (value instanceof Long)
            return (Long) value;
        return 0;
    }

    public double asDouble() {
        if (value == null)
            return 0;
        if (value instanceof Double)
            return (Double) value;
        return 0;
    }

    public float asFloat() {
        if (value == null)
            return 0;
        if (value instanceof Float)
            return (Float) value;
        return 0;
    }

    public T orElse(@NonNull T other){
        return value == null ? other : value;
    }

    public void ifPresent(Consumer<? super T> consumer){
        if(value != null)
            consumer.accept(value);
    }

    public boolean isPresent(){
        return value != null;
    }
}
