package biz.ftsdesign.paranoiddiary;

import android.widget.SectionIndexer;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import biz.ftsdesign.paranoiddiary.model.Record;

import static biz.ftsdesign.paranoiddiary.Formats.MONTH_FORMAT;

class RecordSectionIndexer implements SectionIndexer {
    private Map<String, List<Integer>> sectionsToItems = Collections.emptyMap();
    private String[] sections = new String[0];

    RecordSectionIndexer(@NonNull List<Record> records) {
        updateSections(records);
    }

    synchronized void updateSections(@NonNull List<Record> records) {
        Map<String, List<Integer>> newSectionsToItems = new LinkedHashMap<>();
        for (int position = 0; position < records.size(); position++) {
            Record record = records.get(position);
            final String sectionName = createSectionName(record);
            List<Integer> sectionItems = newSectionsToItems.get(sectionName);
            if (sectionItems == null) {
                sectionItems = new LinkedList<>();
                newSectionsToItems.put(sectionName, sectionItems);
            }
            sectionItems.add(position);
        }

        String[] newSections = new String[newSectionsToItems.size()];
        int i = 0;
        for (String sectionName : newSectionsToItems.keySet()) {
            newSections[i++] = sectionName;
        }

        sectionsToItems = newSectionsToItems;
        sections = newSections;
    }

    private String createSectionName(@NonNull Record record) {
        return Formats.format(MONTH_FORMAT, record.getTimeCreated());
    }

    @Override
    public synchronized Object[] getSections() {
        return sections;
    }

    @Override
    public synchronized int getPositionForSection(int sectionIndex) {
        String section = sections[sectionIndex];
        List<Integer> positions = sectionsToItems.get(section);
        if (positions != null) {
            if (!positions.isEmpty()) {
                return positions.get(0);
            }
        }
        return -1;
    }

    @Override
    public synchronized int getSectionForPosition(int position) {
        int sectionIndex = 0;
        for (Map.Entry<String,List<Integer>> entry : sectionsToItems.entrySet()) {
            List<Integer> positions = entry.getValue();
            if (positions.contains(position)) {
                return sectionIndex;
            }
            sectionIndex++;
        }
        return -1;
    }
}
