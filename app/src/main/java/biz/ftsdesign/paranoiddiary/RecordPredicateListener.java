package biz.ftsdesign.paranoiddiary;

import biz.ftsdesign.paranoiddiary.model.Record;
import biz.ftsdesign.paranoiddiary.predicate.NamedPredicate;

interface RecordPredicateListener {
    void setRecordPredicate(NamedPredicate<Record> recordPredicate);
}
