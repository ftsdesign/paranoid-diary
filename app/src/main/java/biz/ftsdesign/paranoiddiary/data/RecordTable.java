package biz.ftsdesign.paranoiddiary.data;

import android.provider.BaseColumns;

final class RecordTable implements BaseColumns {
    static final String TABLE_NAME = "Records";
    static final String COLUMN_DIARY_ID = "DiaryId";
    static final String COLUMN_TIME_CREATED = "TimeCreated";
    static final String COLUMN_TIME_UPDATED = "TimeUpdated";
    static final String COLUMN_ENCRYPTED_TEXT = "EncryptedText";
    static final String COLUMN_LAT = "Lat";
    static final String COLUMN_LON = "Lon";

    static final String CREATE_TABLE_QUERY = "CREATE TABLE " + TABLE_NAME + " (" +
            _ID + " INTEGER PRIMARY KEY," +
            COLUMN_DIARY_ID + " INTEGER," +
            COLUMN_TIME_CREATED + " INTEGER," +
            COLUMN_TIME_UPDATED + " INTEGER," +
            COLUMN_ENCRYPTED_TEXT + " BLOB," +
            COLUMN_LAT + " REAL," +
            COLUMN_LON + " REAL" +
            ")";

    private RecordTable() {
        throw new UnsupportedOperationException();
    }
}
