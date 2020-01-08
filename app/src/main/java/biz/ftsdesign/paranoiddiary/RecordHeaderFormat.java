package biz.ftsdesign.paranoiddiary;

import java.util.Date;

import biz.ftsdesign.paranoiddiary.model.Record;

abstract class RecordHeaderFormat {
    abstract String format(Record record);

    static final RecordHeaderFormat FORMAT_DEV = new RecordHeaderFormat() {
        @Override
        String format(Record record) {
            return "#" + record.getId() + " " + Formats.TIMESTAMP_FORMAT.format(new Date(record.getTimeCreated()));
        }
    };

    static final RecordHeaderFormat FORMAT_DEFAULT = new RecordHeaderFormat() {
        @Override
        String format(Record record) {
            return Formats.TIMESTAMP_FORMAT.format(new Date(record.getTimeCreated()));
        }
    };
}
