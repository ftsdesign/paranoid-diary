package biz.ftsdesign.paranoiddiary;

import java.text.SimpleDateFormat;

public abstract class Formats {
    public static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("dd-MMM-yyyy, EEE HH:mm");
    public static final SimpleDateFormat FILE_TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmm");

    private Formats() {
        throw new UnsupportedOperationException();
    }
}
