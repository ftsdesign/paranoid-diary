package biz.ftsdesign.paranoiddiary;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.Map;

import biz.ftsdesign.paranoiddiary.data.DataStorageService;
import biz.ftsdesign.paranoiddiary.model.Tag;

class TagListAdapter extends BaseAdapter {
    private final Activity activity;
    private List<Tag> allTags;
    private Map<Long, Integer> tagIdToUsageCount;
    private final DataStorageService dataStorageService;

    TagListAdapter(@NonNull Activity activity, @NonNull DataStorageService dataStorageService) {
        this.activity = activity;
        this.dataStorageService = dataStorageService;
        reload();
    }

    @Override
    public int getCount() {
        return allTags.size();
    }

    @Override
    public Object getItem(int position) {
        Tag tag = allTags.get(position);
        Integer usageCount = tagIdToUsageCount.get(tag.getId());
        if (usageCount == null)
            usageCount = 0;
        return "#" + tag.getName() + " (" + usageCount + ")";
    }

    @Override
    public long getItemId(int position) {
        return allTags.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(activity).
                    inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
        }

        CheckedTextView checkedTextView = (CheckedTextView) convertView;
        checkedTextView.setText((String) getItem(position));

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    void reload() {
        this.allTags = dataStorageService.getAllTagsSortedByName();
        this.tagIdToUsageCount = dataStorageService.getTagUsageCount();
        this.notifyDataSetChanged();
    }

    int getPosition(Tag tag) {
        return allTags.indexOf(tag);
    }
}
