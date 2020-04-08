package biz.ftsdesign.paranoiddiary.predicate;

import androidx.annotation.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import biz.ftsdesign.paranoiddiary.Util;
import biz.ftsdesign.paranoiddiary.model.Record;
import biz.ftsdesign.paranoiddiary.model.Tag;

public class TagPredicate extends NamedPredicate<Record> {
    private final Set<Tag> tagsToFilter;

    public TagPredicate(@NonNull Collection<Tag> tags) {
        super(Util.toString(tags));
        this.tagsToFilter = new HashSet<>(tags);
    }

    public TagPredicate(@NonNull Tag tag) {
        super(Util.toString(tag));
        this.tagsToFilter = Collections.singleton(tag);
    }

    /**
     * The rationale behind the AND logic is that the more search criteria
     * (in this case, tags) we specify, the narrower (not broader)
     * the search result gets.
     */
    @Override
    public boolean test(Record record) {
        if (record == null)
            return false;

        int countMatches = 0;
        for (Tag filterTag : tagsToFilter) {
            if (record.getTags().contains(filterTag)) {
                countMatches++;
            }
        }

        return countMatches == tagsToFilter.size();
    }
}
