package biz.ftsdesign.paranoiddiary.data;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import biz.ftsdesign.paranoiddiary.Formats;
import biz.ftsdesign.paranoiddiary.model.Record;
import biz.ftsdesign.paranoiddiary.model.Tag;

import static biz.ftsdesign.paranoiddiary.Formats.TIMESTAMP_FORMAT;

public class DataUtils {
    public static final String MIME_ZIP = "application/zip";
    public static final int DEFAULT_DIARY_ID = 1;
    public static final Pattern PATTERN_HASHTAG = Pattern.compile("#\\w+");

    @NonNull
    public static SortedSet<String> extractTags(@NonNull String text) {
        SortedSet<String> tagNames = new TreeSet<>();
        Matcher m = PATTERN_HASHTAG.matcher(text);
        while (m.find()) {
            String tagText = m.group();
            if (tagText.startsWith("#") && tagText.length() > 1) {
                tagNames.add(tagText.substring(1));
            }
        }
        return tagNames;
    }

    static byte[] assemble(byte[]... byteArrays) {
        int len = 0;
        for (byte[] byteArray : byteArrays) {
            len += byteArray.length;
        }

        final byte[] out = new byte[len];
        int pointer = 0;
        for (byte[] byteArray : byteArrays) {
            System.arraycopy(byteArray, 0, out, pointer, byteArray.length);
            pointer += byteArray.length;
        }

        return out;
    }

    public enum BackupFormat {
        TEXT, JSON
    }

    public static String toJson(List<Record> records) {
        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
        return gson.toJson(records);
    }

    public static String toString(Record record) {
        StringBuilder sb = new StringBuilder();
        sb.append("#").append(record.getId()).append("\n");
        sb.append(Formats.format(TIMESTAMP_FORMAT, record.getTimeCreated())).append("\n");
        if (record.getGeoTag() != null) {
            sb.append(record.getGeoTag().getLat()).append(",").append(record.getGeoTag().getLon()).append("\n");
        }
        if (!record.getTags().isEmpty()) {
            for (Iterator<Tag> it = record.getTags().iterator(); it.hasNext(); ) {
                Tag tag = it.next();
                sb.append("#").append(tag.getName());
                if (it.hasNext())
                    sb.append(" ");
            }
            sb.append("\n");
        }
        sb.append(record.getText()).append("\n\n");
        return sb.toString();
    }

    public static String getRecordsAsText(List<Record> records) {
        StringBuilder sb = new StringBuilder();
        sb.append("Paranoid Diary @ ").append(Formats.TIMESTAMP_FORMAT.format(new Date())).append("\n\n");

        for (Record record : records) {
            sb.append(DataUtils.toString(record));
        }
        return sb.toString();
    }
}
