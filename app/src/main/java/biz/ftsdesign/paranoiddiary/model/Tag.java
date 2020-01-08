package biz.ftsdesign.paranoiddiary.model;

import androidx.annotation.NonNull;

import java.util.Objects;

public class Tag implements Comparable<Tag> {
    private final long id;
    private final String name;

    public Tag(final long id, final String name) {
        this.id = id;
        if (name == null)
            throw new NullPointerException();
        if (name.trim().isEmpty())
            throw new IllegalArgumentException();
        this.name = name.trim();
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    @Override
    public int compareTo(Tag that) {
        return this.name.compareToIgnoreCase(that.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        return id == tag.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public @NonNull String toString() {
        return "#" + name + "[" + id + "]";
    }
}
