package biz.ftsdesign.paranoiddiary.data;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
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

import net.lingala.zip4j.io.inputstream.ZipInputStream;
import net.lingala.zip4j.io.outputstream.ZipOutputStream;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.EncryptionMethod;

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

    /**
     * Combines multiple byte arrays into one.
     */
    @NonNull
    static byte[] assemble(@NonNull byte[]... byteArrays) {
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

    @NonNull
    public static List<Record> fromJson(@NonNull byte[] jsonBytes) {
        Gson gson = new Gson();
        Record[] data = gson.fromJson(new InputStreamReader(new ByteArrayInputStream(jsonBytes)), Record[].class);
        return Arrays.asList(data);
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

    @NonNull
    public static List<Record> getRecordsFromBackupZip(@NonNull InputStream inputStreamFromZipFile, @NonNull char[] password) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(inputStreamFromZipFile, password)) {
            LocalFileHeader lfh = zis.getNextEntry();
            if (lfh == null) {
                throw new IOException("Backup zip file is empty");
            }
            int bytesExpected = (int) lfh.getUncompressedSize();

            int readLen;
            byte[] readBuffer = new byte[4096];

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while ((readLen = zis.read(readBuffer)) != -1) {
                baos.write(readBuffer, 0, readLen);
            }

            byte[] payload = baos.toByteArray();
            if (payload.length != bytesExpected) {
                throw new IOException("Bytes read " + payload.length + " != expected size " + bytesExpected);
            }
            return DataUtils.fromJson(payload);
        }
    }

    @NonNull
    public static byte[] createEncryptedZip(@NonNull byte[] recordsData, @NonNull String filename, @NonNull String password) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos, password.toCharArray());
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setEncryptFiles(true);
        zipParameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);
        zipParameters.setFileNameInZip(filename);
        zipParameters.setEntrySize(recordsData.length);
        zos.putNextEntry(zipParameters);
        zos.write(recordsData);
        zos.closeEntry();
        zos.close();
        return baos.toByteArray();
    }
}
