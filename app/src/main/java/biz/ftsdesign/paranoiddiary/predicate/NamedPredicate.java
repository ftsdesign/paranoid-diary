package biz.ftsdesign.paranoiddiary.predicate;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class NamedPredicate<T> {
    protected final String name;

    NamedPredicate(@NonNull String name) {
        this.name = name;
    }

    public abstract boolean test(@Nullable T t);

    @NonNull
    public String getName() {
        return name;
    }

    @NonNull
    @Override
    public String toString() {
        return getName();
    }
}
