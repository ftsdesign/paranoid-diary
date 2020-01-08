package biz.ftsdesign.paranoiddiary.data;

import android.provider.BaseColumns;

final class RecordTagTable implements BaseColumns {
    static final String TABLE_NAME = "RecordTags";
    static final String COLUMN_RECORD_ID = "RecordId";
    static final String COLUMN_TAG_ID = "TagId";

    static final String CREATE_TABLE_QUERY = "CREATE TABLE " + TABLE_NAME + " (" +
            COLUMN_RECORD_ID + " INTEGER NOT NULL," +
            COLUMN_TAG_ID + " INTEGER NOT NULL, " +
            "PRIMARY KEY (" + COLUMN_RECORD_ID + ", " + COLUMN_TAG_ID + ")" +
            ")";

    private RecordTagTable() {
        throw new UnsupportedOperationException();
    }
}
