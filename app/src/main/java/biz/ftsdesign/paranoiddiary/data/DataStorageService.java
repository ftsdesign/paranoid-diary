package biz.ftsdesign.paranoiddiary.data;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import biz.ftsdesign.paranoiddiary.model.GeoTag;
import biz.ftsdesign.paranoiddiary.model.Record;
import biz.ftsdesign.paranoiddiary.model.Tag;

/**
 * If the password is not set (crypto == null):
 * - For read operations, we return null or empty collections
 * - For write operations, we throw GeneralSecurityException
 */
public class DataStorageService extends Service implements PasswordListener {
    private final IBinder binder;
    private CryptoModule crypto;
    private DBHelper dbHelper;

    // Data structures to keep all tags in memory
    private final SortedMap<String, Tag> tagStringToTag = new TreeMap<>(String::compareToIgnoreCase);
    private final Map<Long, Tag> idToTag = new HashMap<>();
    private Map<Long,List<Long>> recordIdToTagIds = new HashMap<>();
    private boolean allTagsLoaded = false;

    public DataStorageService() {
        this.crypto = getCryptoModule(TransientPasswordStorage.getPassword());
        this.binder = new DataStorageServiceBinder();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(this.getClass().getSimpleName(), "onBind");
        return binder;
    }

    /**
     * Loads a record from the database, together with the associated tags
     */
    @Nullable
    public synchronized Record getRecord(long recordId) {
        if (crypto == null)
            return null;
        Log.i(this.getClass().getSimpleName(), "getRecord " + recordId);
        final Record record = dbHelper.getRecord(recordId, crypto);
        if (record != null) {
            populateRecordWithTags(record);
        } else {
            Log.w(this.getClass().getSimpleName(), "Record not found #" + recordId);
        }
        return record;
    }

    /**
     * Updates the record and its tags mappings in the database and cache. Tags must already exist.
     */
    public synchronized void updateRecordAndTags(@NonNull Record record) throws GeneralSecurityException, DataException {
        if (crypto == null)
            throw new GeneralSecurityException("No password");
        Log.i(this.getClass().getSimpleName(), "update " + record);
        record.setTimeUpdated(System.currentTimeMillis());
        dbHelper.updateTextAndTimestamp(record, crypto);
        dbHelper.updateRecordTagMappings(record);
        updateRecordTagsCache(record);
    }

    private void updateRecordTagsCache(@NonNull Record record) {
        List<Long> tagIds = recordIdToTagIds.get(record.getId());
        if (tagIds == null) {
            tagIds = new LinkedList<>();
            recordIdToTagIds.put(record.getId(), tagIds);
        } else {
            tagIds.clear();
        }
        for (Tag tag : record.getTags()) {
            tagIds.add(tag.getId());
        }
    }

    @NonNull
    public synchronized Record createNewRecord(int diaryId, GeoTag geoTag) throws GeneralSecurityException, DataException {
        if (crypto == null)
            throw new GeneralSecurityException("No password");
        final Record record = new Record(-1);
        record.setDiaryId(diaryId);
        record.setGeoTag(geoTag);
        record.setTimeCreated(System.currentTimeMillis());
        record.setTimeUpdated(record.getTimeCreated());

        final Record recordWithId = dbHelper.create(record, crypto);
        Log.i(this.getClass().getCanonicalName(), "createNewRecord completed " + recordWithId);
        return recordWithId;
    }

    @NonNull
    public synchronized List<Record> getAllRecordsNoText(final int diaryId) throws GeneralSecurityException {
        if (crypto == null)
            return Collections.emptyList();
        final List<Record> records = dbHelper.getAllRecordsNoTextByTimeDesc(diaryId);
        populateRecordsWithTags(records);
        return records;
    }

    private void reloadRecordTagMappingsFromDb() {
        recordIdToTagIds = dbHelper.getAllRecordTagMappings();
    }

    /**
     * Populates the selected records with tags as per recordIdToTagIds
     */
    private void populateRecordsWithTags(@NonNull final List<Record> records) {
        ensureAllTagsLoaded();
        reloadRecordTagMappingsFromDb();
        Map<Long,Record> idToRecord = new HashMap<>();
        for (Record record : records) {
            idToRecord.put(record.getId(), record);
        }

        int tagsCount = 0;
        for (Map.Entry<Long, List<Long>> entry : recordIdToTagIds.entrySet()) {
            Record record = idToRecord.get(entry.getKey());
            for (long tagId : entry.getValue()) {
                Tag tag = getTagById(tagId);
                if (record != null && tag != null) {
                    record.getTags().add(tag);
                    tagsCount++;
                }
            }
        }
        Log.i(this.getClass().getSimpleName(), "Populated " + records.size() + " records with " + tagsCount + " tags");
    }

    /**
     * Reloads recordIdToTagIds from the database and
     * populates the record with the tags from it.
     * Any previously existing tags are cleared.
     */
    private void populateRecordWithTags(@NonNull final Record record) {
        ensureAllTagsLoaded();
        reloadRecordTagMappingsFromDb();

        record.getTags().clear();
        final List<Long> tagIds = recordIdToTagIds.get(record.getId());
        if (tagIds != null) {
            for (long tagId : tagIds) {
                Tag tag = getTagById(tagId);
                if (tag != null) {
                    record.getTags().add(tag);
                }
            }
        }
        Log.i(this.getClass().getSimpleName(), "Populated record " + record.getId() + " with " + record.getTags().size() + " tags");
    }

    @Nullable
    private synchronized Tag getTagById(long tagId) {
        ensureAllTagsLoaded();
        return idToTag.get(tagId);
    }

    @NonNull
    public synchronized List<Record> getAllRecords(final int diaryId) throws GeneralSecurityException {
        if (crypto == null)
            return Collections.emptyList();
        final List<Record> records = dbHelper.getAllRecordsByTimeDesc(diaryId, crypto);
        populateRecordsWithTags(records);
        return records;
    }

    public synchronized int deleteRecordAndTagMappings(long recordId) throws GeneralSecurityException {
        if (crypto == null)
            throw new GeneralSecurityException("No password");
        Log.i(this.getClass().getCanonicalName(), "delete " + recordId);
        int deletedRecords = dbHelper.deleteRecord(recordId);
        dbHelper.clearTagsForRecord(recordId);
        reloadRecordTagMappingsFromDb();
        return deletedRecords;
    }

    public boolean isPasswordCorrect() {
        return dbHelper.isPasswordCorrect(crypto);
    }

    /**
     *
     * @return true if the database is initialized (password is set)
     */
    public boolean isPasswordSet() {
        boolean result = dbHelper.hasPwdCheck();
        Log.i(this.getClass().getSimpleName(), "isPasswordSet=" + result);
        return result;
    }

    @SuppressWarnings("unused") // For future use
    private synchronized void cleanup() {
        Log.i(this.getClass().getSimpleName(), "cleanup");
        // TODO zero length records
        // TODO delete orphan tag mappings
    }

    private int countMappings(@NonNull Tag tag) {
        Integer usages = getTagUsageCount().get(tag.getId());
        return usages != null ? usages : 0;
    }

    @SuppressWarnings("unused") // Reserved for future use
    private boolean safeDeleteTag(@NonNull Tag tag) {
        reloadRecordTagMappingsFromDb();
        final int mappings = countMappings(tag);
        if (mappings == 0) {
            dbHelper.deleteTag(tag.getId());
            idToTag.remove(tag.getId());
            tagStringToTag.remove(tag.getName());
            Log.i(this.getClass().getSimpleName(), "Tag " + tag + " deleted");
            return true;
        } else {
            Log.w(this.getClass().getSimpleName(), "Tag " + tag + " is still mapped to " + mappings + " records and cannot be deleted");
            return false;
        }
    }

    @NonNull
    public synchronized String getRecordText(long id) {
        return dbHelper.getRecordText(id, crypto);
    }

    @Override
    public synchronized void onAfterPasswordCleared() {
        crypto = null;
        clearTags();
    }

    private synchronized void clearTags() {
        idToTag.clear();
        tagStringToTag.clear();
        recordIdToTagIds.clear();
        allTagsLoaded = false;
    }

    @Override
    public synchronized void onPasswordSet() {
        crypto = getCryptoModule(TransientPasswordStorage.getPassword());
        ensureAllTagsLoaded();
    }

    @Nullable
    private CryptoModule getCryptoModule(final char[] password) {
        CryptoModule out = null;
        if (TransientPasswordStorage.isSet()) {
            try {
                out = new CryptoModuleImplV1(password);
                Log.i(this.getClass().getSimpleName(), "Crypto module set up");
            } catch (GeneralSecurityException e) {
                Log.wtf(this.getClass().getSimpleName(), "Can't setup crypto", e);
            }
        } else {
            Log.w(this.getClass().getSimpleName(), "Password is not set");
        }
        return out;
    }

    public synchronized int deleteRecords(@NonNull List<Record> recordsToDelete) throws GeneralSecurityException {
        int deletedRecords = 0;
        for (Record record : recordsToDelete) {
            deletedRecords += deleteRecordAndTagMappings(record.getId());
        }
        return deletedRecords;
    }

    public synchronized void globalChangePassword(String newPassword) throws GeneralSecurityException {
        if (newPassword == null || newPassword.trim().isEmpty())
            throw new GeneralSecurityException("Password must be non empty");
        Log.i(this.getClass().getSimpleName(), "Performing global password change");
        try {
            final CryptoModule newCryptoModule = getCryptoModule(newPassword.toCharArray());
            if (newCryptoModule == null)
                throw new GeneralSecurityException("Password is not set");

            dbHelper.savePwdCheck(newCryptoModule);
            List<Record> allRecords = dbHelper.getAllRecords(crypto);
            for (Record record : allRecords) {
                dbHelper.updateTextAndTimestamp(record, newCryptoModule);
            }
            Log.i(this.getClass().getSimpleName(), "Records updated: " + allRecords.size());

            List<Tag> allTags = dbHelper.getAllTags(crypto);
            for (Tag tag : allTags) {
                dbHelper.updateTagName(tag, newCryptoModule);
            }
            Log.i(this.getClass().getSimpleName(), "Tags updated: " + allTags.size());
            this.crypto = null;
            TransientPasswordStorage.clear();
            Log.i(this.getClass().getSimpleName(), "Global password change completed successfully");

        } catch (Exception e) {
            Log.e(this.getClass().getSimpleName(), "Change password failed", e);
        }
    }

    @NonNull
    public List<Tag> getAllTagsSortedByName() {
        return new ArrayList<>(tagStringToTag.values());
    }

    public synchronized @NonNull Map<Long, Integer> getTagUsageCount() {
        reloadRecordTagMappingsFromDb();

        final Map<Long,Integer> tagIdToCount = new HashMap<>();
        for (List<Long> tagIds : recordIdToTagIds.values()) {
            for (long tagId : tagIds) {
                Integer count = tagIdToCount.get(tagId);
                if (count == null) {
                    count = 0;
                }
                tagIdToCount.put(tagId, count + 1);
            }
        }

        return tagIdToCount;
    }

    public synchronized void bulkModifyTags(@NonNull List<Long> recordIds, @NonNull List<Long> tagsToSetIds,
                                            @NonNull List<Long> tagsToUnsetIds) throws GeneralSecurityException {
        if (crypto == null)
            throw new GeneralSecurityException("No password");
        Log.i(this.getClass().getSimpleName(), "Bulk set " + tagsToSetIds.size() + " unset " + tagsToUnsetIds.size() + " tags for " + recordIds.size() + " records");
        for (long recordId : recordIds) {
            for (long tagId : tagsToSetIds) {
                dbHelper.setTagForRecord(recordId, tagId);
            }
            for (long tagId : tagsToUnsetIds) {
                dbHelper.unsetTagForRecord(recordId, tagId);
            }
        }
        reloadRecordTagMappingsFromDb();
    }

    @Nullable
    public synchronized Tag getTag(long tagId) {
        return idToTag.get(tagId);
    }

    @NonNull
    public synchronized List<Tag> getTags(@NonNull Collection<Long> tagIds) {
        final List<Tag> out = new LinkedList<>();
        for (long tagId : tagIds) {
            Tag tag = idToTag.get(tagId);
            if (tag != null) {
                out.add(tag);
            }
        }
        Collections.sort(out);
        return out;
    }

    public void savePwdCheck() {
        try {
            dbHelper.savePwdCheck(crypto);
        } catch (GeneralSecurityException e) {
            Log.wtf(this.getClass().getSimpleName(), e.toString(), e);
        }
    }

    @Nullable
    public Tag getTagByName(String tagName) {
        return tagStringToTag.get(tagName);
    }

    public synchronized long getRecordsCount() {
        final long recordsCount;
        if (crypto == null) {
            recordsCount = 0;
        } else {
            recordsCount = dbHelper.getRecordsCount();
        }
        return recordsCount;
    }

    public synchronized int getTagsCount() {
        final int tagsCount;
        if (crypto == null) {
            tagsCount = 0;
        } else {
            tagsCount = tagStringToTag.size();
        }
        return tagsCount;
    }

    @Nullable
    public Record getFirstRecord() {
        return getFirstRecord(DBHelper.SortOrder.ASC);
    }

    @Nullable
    public Record getLastRecord() {
        return getFirstRecord(DBHelper.SortOrder.DESC);
    }

    @Nullable
    private synchronized Record getFirstRecord(@NonNull DBHelper.SortOrder sortOrder) {
        Record record = null;
        if (crypto != null) {
            try {
                record = dbHelper.getFirstRecord(sortOrder, crypto);
            } catch (GeneralSecurityException e) {
                Log.e(this.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
        return record;
    }

    public class DataStorageServiceBinder extends Binder {
        public DataStorageService getService() {
            return DataStorageService.this;
        }
    }

    @Override
    public void onCreate() {
        Log.i(this.getClass().getSimpleName(), "onCreate");
        this.dbHelper = new DBHelper(getApplicationContext());
        dbHelper.onServiceCreate();
        super.onCreate();

        TransientPasswordStorage.addListener(this);
    }

    private synchronized void ensureAllTagsLoaded() {
        if (!allTagsLoaded) {
            Log.i(this.getClass().getSimpleName(), "ensureAllTagsLoaded");
            try {
                final List<Tag> allTags = dbHelper.getAllTags(crypto);
                for (Tag tag : allTags) {
                    addToTagsCache(tag);
                }
                reloadRecordTagMappingsFromDb();
                allTagsLoaded = true;
                Log.i(this.getClass().getSimpleName(), "Loaded tags: " + allTags.size());

            } catch (Exception e) {
                Log.e(this.getClass().getSimpleName(), "Cannot load tags", e);
            }
        }
    }

    @NonNull
    public synchronized Tag getOrCreateTagByName(@NonNull String tagString) throws GeneralSecurityException {
        ensureAllTagsLoaded();

        final Tag existingTagWithSameName = tagStringToTag.get(tagString);
        if (existingTagWithSameName != null)
            return existingTagWithSameName;

        final Tag newTag = dbHelper.create(tagString, crypto);
        addToTagsCache(newTag);
        return newTag;
    }

    private synchronized void addToTagsCache(@NonNull final Tag tag) {
        if (tagStringToTag.containsKey(tag.getName())) {
            throw new IllegalArgumentException("Duplicate tag name: " + tagStringToTag.get(tag.getName()) + " and " + tag);
        }
        tagStringToTag.put(tag.getName(), tag);
        idToTag.put(tag.getId(), tag);
    }


    @Override
    public void onDestroy() {
        Log.i(this.getClass().getSimpleName(), "onDestroy");
        TransientPasswordStorage.removeListener(this);
        if (dbHelper != null) {
            dbHelper.onServiceDestroy();
        }
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(this.getClass().getSimpleName(), "onUnbind");
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        Log.i(this.getClass().getSimpleName(), "onRebind");
        super.onRebind(intent);
    }

    public synchronized void deleteAllData() {
        if (!TransientPasswordStorage.isSet())
            return;
        clearTags();
        dbHelper.recreateDb();
    }

    public enum BackupRestoreMode {
        REPLACE, ADD
    }

    public int restoreBackup(@NonNull List<Record> records, @NonNull BackupRestoreMode backupRestoreMode) {
        Log.i(this.getClass().getSimpleName(), "Restoring " + records.size() + " records from backup, mode " + backupRestoreMode);
        dbHelper.beginTransaction();
        try {
            if (backupRestoreMode == BackupRestoreMode.REPLACE) {
                dbHelper.deleteAll();
            }

            for (Record incomingRecord : records) {
                Set<Tag> incomingTagsCopy = new HashSet<>(incomingRecord.getTags());
                incomingRecord.getTags().clear();

                Record createdRecord = dbHelper.create(incomingRecord, crypto);

                for (Tag incomingTag : incomingTagsCopy) {
                    Tag existingTag = getOrCreateTagByName(incomingTag.getName());
                    createdRecord.getTags().add(existingTag);
                }
                dbHelper.updateRecordTagMappings(createdRecord);
            }

            reloadRecordTagMappingsFromDb();
            dbHelper.setTransactionSuccessful();
            return records.size();

        } catch (DataException|GeneralSecurityException e) {
            Log.e(this.getClass().getSimpleName(), "Cannot restore from backup, rolling back", e);
            return 0;

        } finally {
            dbHelper.endTransaction();
        }
    }
}
