package biz.ftsdesign.paranoiddiary;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class Formats {
    @SuppressLint("SimpleDateFormat")
    public static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("dd-MMM-yyyy, EEE HH:mm");
    @SuppressLint("SimpleDateFormat")
    static final SimpleDateFormat FILE_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmm");
    @SuppressLint("SimpleDateFormat")
    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MMM-yyyy");

    private Formats() {
        throw new UnsupportedOperationException();
    }

    public static String format(@NonNull SimpleDateFormat format, long time) {
        return format.format(new Date(time));
    }
}
