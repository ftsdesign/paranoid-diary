package biz.ftsdesign.paranoiddiary.data;

import android.provider.BaseColumns;

final class TagTable implements BaseColumns {
    static final String TABLE_NAME = "Tags";
    static final String COLUMN_ENCRYPTED_NAME = "EncryptedName";

    static final String CREATE_TABLE_QUERY = "CREATE TABLE " + TABLE_NAME + " (" +
            _ID + " INTEGER PRIMARY KEY," +
            COLUMN_ENCRYPTED_NAME + " BLOB" +
            ")";

    private TagTable() {
        throw new UnsupportedOperationException();
    }
}
