package biz.ftsdesign.paranoiddiary.model;

import androidx.annotation.NonNull;

import java.util.Date;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

public class Record {
    private final long id;
    private int diaryId;
    private long timeCreated;
    private long timeUpdated;
    private String text = "";
    private final SortedSet<Tag> tags;
    private GeoTag geoTag;

    public Record(final long id) {
        this.id = id;
        this.tags = new TreeSet<>();
    }

    public Record(final long id, final Record copy) {
        this(id);
        this.diaryId = copy.diaryId;
        this.timeCreated = copy.timeCreated;
        this.timeUpdated = copy.timeUpdated;
        this.text = copy.text;
        this.tags.addAll(copy.tags);
        this.geoTag = copy.geoTag;
    }

    public long getId() {
        return id;
    }

    public int getDiaryId() {
        return diaryId;
    }

    public void setDiaryId(int diaryId) {
        this.diaryId = diaryId;
    }

    public long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(long timeCreated) {
        this.timeCreated = timeCreated;
    }

    public long getTimeUpdated() {
        return timeUpdated;
    }

    public void setTimeUpdated(long timeUpdated) {
        this.timeUpdated = timeUpdated;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @NonNull
    public SortedSet<Tag> getTags() {
        return tags;
    }

    public GeoTag getGeoTag() {
        return geoTag;
    }

    public void setGeoTag(GeoTag geoTag) {
        this.geoTag = geoTag;
    }

    @Override
    public @NonNull String toString() {
        return "[#" + id + " " + (new Date(timeCreated)) + " len=" + text.length() + "]";
    }

    public boolean hasText() {
        return text != null && !text.trim().isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Record record = (Record) o;
        return id == record.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
