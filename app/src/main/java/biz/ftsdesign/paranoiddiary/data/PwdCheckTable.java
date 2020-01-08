package biz.ftsdesign.paranoiddiary.data;

import android.provider.BaseColumns;

class PwdCheckTable implements BaseColumns {
    static final String TABLE_NAME = "PwdCheck";
    static final String COLUMN_KEY = "Key";
    static final int KEY = 1;
    static final String COLUMN_VALUE = "Value";

    static final String CREATE_TABLE_QUERY = "CREATE TABLE " + TABLE_NAME + " (" +
            COLUMN_KEY + " INTEGER NOT NULL," +
            COLUMN_VALUE + " BLOB NOT NULL, " +
            "PRIMARY KEY (" + COLUMN_KEY + ")" +
            ")";
}
