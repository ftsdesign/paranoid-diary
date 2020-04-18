package biz.ftsdesign.paranoiddiary;

import biz.ftsdesign.paranoiddiary.model.Record;

import static biz.ftsdesign.paranoiddiary.Formats.TIMESTAMP_FORMAT;

abstract class RecordHeaderFormat {
    abstract String format(Record record);

    static final RecordHeaderFormat FORMAT_DEFAULT = new RecordHeaderFormat() {
        @Override
        String format(Record record) {
            return Formats.format(TIMESTAMP_FORMAT, record.getTimeCreated());
        }
    };
}
