package biz.ftsdesign.paranoiddiary.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import biz.ftsdesign.paranoiddiary.model.GeoTag;
import biz.ftsdesign.paranoiddiary.model.Record;
import biz.ftsdesign.paranoiddiary.model.Tag;

class DBHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "ParanoidDiary.db";
    private static final int PWD_CHECK_LENGTH = 16;
    private SQLiteDatabase db;

    DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        Log.i(this.getClass().getSimpleName(), "DBHelper created");
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);

//        db.execSQL("DROP TABLE IF EXISTS " + PwdCheckTable.TABLE_NAME);
//        db.execSQL("DROP TABLE IF EXISTS " + TagTable.TABLE_NAME);
//        db.execSQL("DROP TABLE IF EXISTS " + RecordTagTable.TABLE_NAME);
//        db.execSQL(TagTable.CREATE_TABLE_QUERY);
//        db.execSQL(RecordTagTable.CREATE_TABLE_QUERY);
//        Log.i(this.getClass().getSimpleName(), "====== RT =====");
    }

    void recreateDb() {
        dropAllTables(db);
        onCreate(db);
    }

    private void dropAllTables(SQLiteDatabase db) {
        Log.i(this.getClass().getSimpleName(), "Dropping all tables...");
        db.execSQL("DROP TABLE IF EXISTS " + RecordTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TagTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + RecordTagTable.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + PwdCheckTable.TABLE_NAME);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(this.getClass().getSimpleName(), "Initializing the database...");
        try {
            db.execSQL(PwdCheckTable.CREATE_TABLE_QUERY);
            db.execSQL(RecordTable.CREATE_TABLE_QUERY);
            db.execSQL(TagTable.CREATE_TABLE_QUERY);
            db.execSQL(RecordTagTable.CREATE_TABLE_QUERY);
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "Cannot initialize db", e);
        }
        Log.i(this.getClass().getSimpleName(), "Database initialized");
    }

//    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    @NonNull
    Record create(@NonNull final Record record, @NonNull final CryptoModule crypto) throws GeneralSecurityException {
        ContentValues values = new ContentValues();
        values.put(RecordTable.COLUMN_DIARY_ID, record.getDiaryId());
        values.put(RecordTable.COLUMN_TIME_CREATED, record.getTimeCreated());
        values.put(RecordTable.COLUMN_TIME_UPDATED, record.getTimeUpdated());
        if (record.hasText()) {
            values.put(RecordTable.COLUMN_ENCRYPTED_TEXT, crypto.encrypt(record.getText()));
        }
        if (record.getGeoTag() != null) {
            values.put(RecordTable.COLUMN_LAT, record.getGeoTag().getLat());
            values.put(RecordTable.COLUMN_LON, record.getGeoTag().getLon());
        }

        long rowId = db.insert(RecordTable.TABLE_NAME, null, values);

        return new Record(rowId, record);
    }

    Tag create(@NonNull final String tagName, @NonNull final CryptoModule crypto) throws GeneralSecurityException {
        ContentValues values = new ContentValues();
        values.put(TagTable.COLUMN_ENCRYPTED_NAME, crypto.encrypt(tagName));
        long rowId = db.insert(TagTable.TABLE_NAME, null, values);
        final Tag tag = new Tag(rowId, tagName);
        Log.i(this.getClass().getSimpleName(), "Created " + tag);
        return tag;
    }

    void updateTextAndTimestamp(@NonNull final Record record, @NonNull final CryptoModule crypto) throws GeneralSecurityException {
        // We only update text or tags
        ContentValues values = new ContentValues();
        values.put(RecordTable.COLUMN_TIME_UPDATED, record.getTimeUpdated());
        if (record.hasText()) {
            values.put(RecordTable.COLUMN_ENCRYPTED_TEXT, crypto.encrypt(record.getText()));
        } else {
            values.put(RecordTable.COLUMN_ENCRYPTED_TEXT, (byte[])null);
        }

        String[] whereArgs = {String.valueOf(record.getId())};
        int rowsUpdated = db.update(RecordTable.TABLE_NAME, values,  RecordTable._ID + " = ?", whereArgs);
        if (rowsUpdated != 1)
            Log.wtf(this.getClass().getSimpleName(), "Can't update record #" + record.getId() + ": rows updated: " + rowsUpdated);
    }

    void updateTagName(@NonNull final Tag tag, @NonNull final CryptoModule crypto) throws GeneralSecurityException {
        ContentValues values = new ContentValues();
        values.put(TagTable.COLUMN_ENCRYPTED_NAME, crypto.encrypt(tag.getName()));

        String[] whereArgs = {String.valueOf(tag.getId())};
        int rowsUpdated = db.update(TagTable.TABLE_NAME, values,  TagTable._ID + " = ?", whereArgs);
        if (rowsUpdated != 1)
            Log.wtf(this.getClass().getSimpleName(), "Can't update tag name #" + tag.getId() + ": rows updated: " + rowsUpdated);
    }

    private void setTagForRecord(@NonNull Record record, @NonNull Tag tag) {
        setTagForRecord(record.getId(), tag.getId());
    }

    void setTagForRecord(long recordId, long tagId) {
        ContentValues values = new ContentValues();
        values.put(RecordTagTable.COLUMN_RECORD_ID, recordId);
        values.put(RecordTagTable.COLUMN_TAG_ID, tagId);
        // Don't want to check if the composite primary key already exists
        db.insertWithOnConflict(RecordTagTable.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    void unsetTagForRecord(long recordId, long tagId) {
        String selection = RecordTagTable.COLUMN_RECORD_ID + " = ? AND " + RecordTagTable.COLUMN_TAG_ID + " = ?";
        String[] selectionArgs = {String.valueOf(recordId), String.valueOf(tagId)};
        db.delete(RecordTagTable.TABLE_NAME, selection, selectionArgs);
    }

    void updateRecordTagMappings(@NonNull final Record record) {
        clearTagsForRecord(record.getId());
        final Set<Tag> tags = record.getTags();
        for (Tag tag : tags) {
            setTagForRecord(record, tag);
        }
        Log.i(this.getClass().getSimpleName(), "Updated " + tags.size() + " tags for record #" + record.getId());
    }

    @NonNull
    String getRecordText(long recordId, @NonNull final CryptoModule crypto) {
        long t1 = System.currentTimeMillis();
        String text = "";
        String selection = RecordTable._ID + " = ?";
        String[] selectionArgs = { String.valueOf(recordId) };
        try (Cursor cursor = db.query(RecordTable.TABLE_NAME,
                new String[]{RecordTable.COLUMN_ENCRYPTED_TEXT},
                selection,
                selectionArgs,
                null, null,
                null)) {

            if (cursor.moveToNext()) {
                byte[] encryptedText = cursor.getBlob(cursor.getColumnIndexOrThrow(RecordTable.COLUMN_ENCRYPTED_TEXT));
                if (encryptedText != null && encryptedText.length >= crypto.getMinLength()) {
                    text = crypto.decrypt(encryptedText);
                }
            }
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "Can't get text for #" + recordId, e);
        }
        t1 = System.currentTimeMillis() - t1;
        Log.d(this.getClass().getSimpleName(), "getRecordText #" + recordId + " returned in " + t1 + " ms");
        return text;
    }

    @NonNull
    List<Record> getAllRecordsNoTextByTimeDesc(int diaryId) throws GeneralSecurityException {
        return getAllRecordsNoTextByTimeDesc(diaryId, false, null);
    }

    @NonNull
    List<Record> getAllRecordsByTimeDesc(int diaryId, @NonNull final CryptoModule crypto) throws GeneralSecurityException {
        return getAllRecordsNoTextByTimeDesc(diaryId, true, crypto);
    }

    @NonNull
    private List<Record> getAllRecordsNoTextByTimeDesc(int diaryId, boolean loadText, final CryptoModule crypto) throws GeneralSecurityException {
        long t1 = System.currentTimeMillis();
        String selection = RecordTable.COLUMN_DIARY_ID + " = ?";
        String[] selectionArgs = { String.valueOf(diaryId) };
        String sortOrder = RecordTable.COLUMN_TIME_CREATED + " desc";
        try (Cursor cursor = db.query(RecordTable.TABLE_NAME,
                null,
                selection,
                selectionArgs,
                null, null,
                sortOrder)) {

            final List<Record> out = new ArrayList<>();
            while (cursor.moveToNext()) {
                out.add(readRecord(cursor, loadText, crypto));
            }
            t1 = System.currentTimeMillis() - t1;
            Log.i(this.getClass().getSimpleName(), "getAllRecordsNoTextByTimeDesc returned " + out.size() + " records in " + t1 + " ms");
            return out;
        }
    }

    @NonNull
    List<Record> getAllRecords(@NonNull final CryptoModule crypto) throws GeneralSecurityException {
        long t1 = System.currentTimeMillis();
        try (Cursor cursor = db.query(RecordTable.TABLE_NAME,
                null,
                null,
                null,
                null, null,
                null)) {

            final List<Record> out = new ArrayList<>();
            while (cursor.moveToNext()) {
                out.add(readRecord(cursor, true, crypto));
            }
            t1 = System.currentTimeMillis() - t1;
            Log.i(this.getClass().getSimpleName(), "getAllRecords returned " + out.size() + " records in " + t1 + " ms");
            return out;
        }
    }

    @NonNull
    private Record readRecord(@NonNull Cursor cursor, boolean loadText, CryptoModule crypto) throws GeneralSecurityException {
        final long recordId = cursor.getLong(cursor.getColumnIndexOrThrow(RecordTable._ID));
        final Record record = new Record(recordId);
        record.setDiaryId(cursor.getInt(cursor.getColumnIndexOrThrow(RecordTable.COLUMN_DIARY_ID)));
        record.setTimeCreated(cursor.getLong(cursor.getColumnIndexOrThrow(RecordTable.COLUMN_TIME_CREATED)));
        record.setTimeUpdated(cursor.getLong(cursor.getColumnIndexOrThrow(RecordTable.COLUMN_TIME_UPDATED)));
        if (loadText) {
            byte[] encryptedText = cursor.getBlob(cursor.getColumnIndexOrThrow(RecordTable.COLUMN_ENCRYPTED_TEXT));
            if (encryptedText != null && encryptedText.length >= crypto.getMinLength()) {
                record.setText(crypto.decrypt(encryptedText));
            }
        }
        boolean hasGeoTag = !(cursor.isNull(cursor.getColumnIndexOrThrow(RecordTable.COLUMN_LAT)) || cursor.isNull(cursor.getColumnIndexOrThrow(RecordTable.COLUMN_LON)));
        if (hasGeoTag) {
            double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(RecordTable.COLUMN_LAT));
            double lon = cursor.getDouble(cursor.getColumnIndexOrThrow(RecordTable.COLUMN_LON));
            GeoTag geoTag = new GeoTag(lat, lon);
            record.setGeoTag(geoTag);
        }
        return record;
    }

    void onServiceCreate() {
        if (db == null) {
            db = getWritableDatabase();
        }
    }

    void onServiceDestroy() {
        if (db != null) {
            db.close();
        }
    }

    void deleteRecord(long recordId) {
        String selection = RecordTable._ID + " = ?";
        String[] selectionArgs = {String.valueOf(recordId)};
        int deletedRows = db.delete(RecordTable.TABLE_NAME, selection, selectionArgs);
        if (deletedRows != 1)
            Log.wtf(this.getClass().getSimpleName(), "Can't delete record #" + recordId + ": rows deleted: " + deletedRows);
        else
            Log.i(this.getClass().getSimpleName(), "Deleted record #" + recordId);
    }

    void deleteTag(long tagId) {
        String selection = TagTable._ID + " = ?";
        String[] selectionArgs = {String.valueOf(tagId)};
        int deletedRows = db.delete(TagTable.TABLE_NAME, selection, selectionArgs);
        if (deletedRows != 1)
            Log.wtf(this.getClass().getSimpleName(), "Can't delete tag #" + tagId + ": rows deleted: " + deletedRows);
        else
            Log.i(this.getClass().getSimpleName(), "Deleted tag #" + tagId);
    }

    void clearTagsForRecord(long recordId) {
        String selection = RecordTagTable.COLUMN_RECORD_ID + " = ?";
        String[] selectionArgs = {String.valueOf(recordId)};
        int tagsDeleted = db.delete(RecordTagTable.TABLE_NAME, selection, selectionArgs);
        Log.i(this.getClass().getSimpleName(), "Deleted all " + tagsDeleted + " tags for record #" + recordId);
    }

    boolean isPasswordCorrect(@NonNull final CryptoModule crypto) {
        String selection = PwdCheckTable.COLUMN_KEY + " = ?";
        String[] selectionArgs = { String.valueOf(PwdCheckTable.KEY) };
        try (Cursor cursor = db.query(PwdCheckTable.TABLE_NAME,
                new String[]{PwdCheckTable.COLUMN_VALUE},
                selection,
                selectionArgs,
                null, null,
                null)) {

            if (cursor.moveToNext()) {
                byte[] encryptedText = cursor.getBlob(cursor.getColumnIndexOrThrow(PwdCheckTable.COLUMN_VALUE));
                if (encryptedText != null && encryptedText.length >= crypto.getMinLength()) {
                    byte[] decrypted = crypto.decryptBytes(encryptedText);
                    byte[] random = new byte[PWD_CHECK_LENGTH];
                    System.arraycopy(decrypted, 0, random, 0, random.length);
                    byte[] hash = new byte[decrypted.length - random.length];
                    System.arraycopy(decrypted, random.length, hash, 0, hash.length);
                    return checkMd5(random, hash);
                }
            }
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "Can't check password", e);
        }

        return false;
    }

    void deleteZeroLengthRecords(@NonNull final CryptoModule crypto) {
        if (!TransientPasswordStorage.isSet())
            return;

        try {
            Log.i(this.getClass().getSimpleName(), "Deleting records with ecrypted text len < " + crypto.getMinLength());
            for (long id : getZeroLengthRecordIds(crypto)) {
                Log.i(this.getClass().getSimpleName(), "Deleting empty record " + id);
                deleteRecord(id);
                clearTagsForRecord(id);
            }

        } catch (Exception e) {
            Log.wtf(this.getClass().getSimpleName(), e.toString(), e);
        }
    }

    @NonNull
    private List<Long> getZeroLengthRecordIds(@NonNull CryptoModule crypto) {
        final List<Long> recordIdsToDelete = new LinkedList<>();
        // TODO if we can query on byte field length, this can be optimized, got no time to research yet
        try (Cursor cursor = db.query(RecordTable.TABLE_NAME,
                new String[]{RecordTable._ID, RecordTable.COLUMN_ENCRYPTED_TEXT},
                null,
                null,
                null, null,
                null)) {

            while (cursor.moveToNext()) {
                boolean delete = false;
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(RecordTable._ID));
                byte[] enc = cursor.getBlob(cursor.getColumnIndexOrThrow(RecordTable.COLUMN_ENCRYPTED_TEXT));
                if (enc == null || enc.length < crypto.getMinLength()) {
                    delete = true;
                }
                if (delete) {
                    recordIdsToDelete.add(id);
                }
            }
        }
        return recordIdsToDelete;
    }

    @NonNull
    List<Tag> getAllTags(@NonNull final CryptoModule crypto) throws GeneralSecurityException {
        long t1 = System.currentTimeMillis();
        try (Cursor cursor = db.query(TagTable.TABLE_NAME,
                null,
                null,
                null,
                null, null,
                null)) {

            final List<Tag> tags = new ArrayList<>();
            while (cursor.moveToNext()) {
                final long tagId = cursor.getLong(cursor.getColumnIndexOrThrow(TagTable._ID));
                final byte[] encryptedName = cursor.getBlob(cursor.getColumnIndexOrThrow(TagTable.COLUMN_ENCRYPTED_NAME));
                String tagName = crypto.decrypt(encryptedName);
                tags.add(new Tag(tagId, tagName));
            }
            t1 = System.currentTimeMillis() - t1;
            Log.i(this.getClass().getSimpleName(), "getAllTags returned " + tags.size() + " tags in " + t1 + " ms");
            return tags;
        }
    }

    @NonNull
    Map<Long, List<Long>> getAllRecordTagMappings() {
        try (Cursor cursor = db.query(RecordTagTable.TABLE_NAME,
                null,
                null,
                null,
                null, null,
                null)) {

            final Map<Long, List<Long>> recordIdToTagIds = new HashMap<>();
            int recordCount = 0;
            while (cursor.moveToNext()) {
                recordCount++;
                final long recordId = cursor.getLong(cursor.getColumnIndexOrThrow(RecordTagTable.COLUMN_RECORD_ID));
                final long tagId = cursor.getLong(cursor.getColumnIndexOrThrow(RecordTagTable.COLUMN_TAG_ID));
                List<Long> tagIdsForRecord = recordIdToTagIds.get(recordId);
                if (tagIdsForRecord == null) {
                    tagIdsForRecord = new LinkedList<>();
                    recordIdToTagIds.put(recordId, tagIdsForRecord);
                }
                tagIdsForRecord.add(tagId);
            }
            Log.i(this.getClass().getSimpleName(), "getAllRecordTagMappings: " + recordCount);
            return recordIdToTagIds;
        }
    }

    @Nullable
    Record getRecord(long recordId, @NonNull CryptoModule crypto) {
        Record record = null;
        String selection = RecordTable._ID + " = ?";
        String[] selectionArgs = { String.valueOf(recordId) };
        try (Cursor cursor = db.query(RecordTable.TABLE_NAME,
                null,
                selection,
                selectionArgs,
                null, null,
                null)) {

            if (cursor.moveToNext()) {
                record = readRecord(cursor, true, crypto);
            }
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "Can't get record #" + recordId, e);
        }
        return record;

    }

    boolean hasPwdCheck() {
        try {
            long records = DatabaseUtils.queryNumEntries(db, PwdCheckTable.TABLE_NAME);
            if (records == 1) {
                return true;
            } else if (records == 0) {
                return false;
            } else {
                Log.wtf(this.getClass().getSimpleName(), "Record count = " + records);
            }
        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), e.toString(), e);
        }
        return false;
    }

    void savePwdCheck(CryptoModule crypto) throws GeneralSecurityException {
        Random random = new SecureRandom();
        byte[] randomBytes = new byte[PWD_CHECK_LENGTH];
        random.nextBytes(randomBytes);
        byte[] hash = md5(randomBytes);
        byte[] combined = DataUtils.assemble(randomBytes, hash);

        ContentValues values = new ContentValues();
        values.put(PwdCheckTable.COLUMN_KEY, PwdCheckTable.KEY);
        values.put(PwdCheckTable.COLUMN_VALUE, crypto.encrypt(combined));

        long id = db.insert(PwdCheckTable.TABLE_NAME, null, values);
        if (id == -1)
            Log.wtf(this.getClass().getSimpleName(), "savePwdCheck failed");
        else
            Log.i(this.getClass().getSimpleName(), "Password check saved");
    }

    private byte[] md5(byte[] bytes) throws GeneralSecurityException {
        MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
        digest.update(bytes);
        return digest.digest();
    }

    private boolean checkMd5(byte[] bytesToCheck, byte[] hash) throws GeneralSecurityException {
        byte[] md5 = md5(bytesToCheck);
        return Arrays.equals(md5, hash);
    }

}
