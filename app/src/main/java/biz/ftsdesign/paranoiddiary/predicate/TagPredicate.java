package biz.ftsdesign.paranoiddiary.predicate;

import biz.ftsdesign.paranoiddiary.model.Record;
import biz.ftsdesign.paranoiddiary.model.Tag;

public class TagPredicate extends NamedPredicate<Record> {
    private final Tag tag;

    public TagPredicate(Tag tag) {
        super("#" + tag.getName());
        this.tag = tag;
    }

    @Override
    public boolean test(Record record) {
        return record.getTags().contains(tag);
    }
}
