package biz.ftsdesign.paranoiddiary;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexboxLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import biz.ftsdesign.paranoiddiary.data.DataStorageService;
import biz.ftsdesign.paranoiddiary.model.Record;
import biz.ftsdesign.paranoiddiary.predicate.NamedPredicate;

import static biz.ftsdesign.paranoiddiary.data.DataUtils.PATTERN_HASHTAG;

class RecordsViewAdapter extends RecyclerView.Adapter<RecordsViewAdapter.RecordViewHolder> {
    private final MainActivity mainActivity;

    private List<Record> records;
    private List<Record> filteredRecords;

    private Map<Long,Record> idToRecord;
    private DataStorageService dataStorageService;
    private int tagColor = Color.YELLOW;
    private SelectionTracker<Long> selectionTracker;
    private NamedPredicate<Record> recordPredicate;

    RecordsViewAdapter(MainActivity mainActivity, List<Record> records) {
        this.mainActivity = mainActivity;
        this.records = records;
        this.filteredRecords = filterRecords(records, recordPredicate);
        this.idToRecord = buildIdToRecord(records);
        setHasStableIds(true);
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecordViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        // Bug workaround for losing text selection ability, see:
        // https://code.google.com/p/android/issues/detail?id=208169
        // https://stackoverflow.com/questions/37566303/edittext-giving-error-textview-does-not-support-text-selection-selection-canc/40140869
        holder.bodyView.setEnabled(false);
        holder.bodyView.setEnabled(true);
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.record_layout, parent, false);

        return new RecordViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        final Record record = filteredRecords.get(position);

        if (record != null) {
            holder.itemView.setActivated(selectionTracker.isSelected(record.getId()));
            if (!record.hasText()) {
                Log.d(this.getClass().getCanonicalName(), "Bind #" + record.getId());
                record.setText(dataStorageService.getRecordText(record.getId()));
            }
            holder.headerView.setText(Util.composeRecordHeader(record));

            if (record.getGeoTag() != null) {
                String geoTagText = String.format("%f %f", record.getGeoTag().getLat(), record.getGeoTag().getLon());
                holder.geoTagView.setText(geoTagText);
                holder.geoTagView.setVisibility(View.VISIBLE);
            } else {
                holder.geoTagView.setText("");
                holder.geoTagView.setVisibility(View.GONE);
            }

            Util.setTagsBox(mainActivity, holder.tagsBox, record, mainActivity);

            SpannableString spannableString = new SpannableString(record.getText());
            Matcher m = PATTERN_HASHTAG.matcher(record.getText());
            while (m.find()) {
                spannableString.setSpan(new ForegroundColorSpan(tagColor), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            holder.bodyView.setText(spannableString);
            holder.geoTagView.setOnClickListener(v -> mainActivity.shareGeoTag(record.getGeoTag()));
        }
    }

    @Override
    public int getItemCount() {
        return filteredRecords.size();
    }

    public int getItemCountUnfiltered() {
        return records.size();
    }

    @Override
    public long getItemId(int position) {
        final Record record = filteredRecords.get(position);
        return record.getId();
    }

    private List<Record> filterRecords(List<Record> records, NamedPredicate<Record> recordPredicate) {
        ArrayList<Record> out = new ArrayList<>();
        for (Record record : records) {
            if (recordPredicate == null || recordPredicate.test(record))
                out.add(record);
        }
        Log.i(this.getClass().getSimpleName(), "Filter " + recordPredicate + " " + records.size() + " -> " + out.size());
        return out;
    }

    void setRecords(List<Record> records) {
        this.records = records;
        this.idToRecord = buildIdToRecord(records);
        this.filteredRecords = filterRecords(records, recordPredicate);
        notifyDataSetChanged();
    }

    private Map<Long, Record> buildIdToRecord(@NonNull List<Record> records) {
        Map<Long, Record> out = new HashMap<>();
        for (Record record : records) {
            out.put(record.getId(), record);
        }
        return out;
    }

    void setDataStorageService(DataStorageService dataStorageService) {
        this.dataStorageService = dataStorageService;
    }

    void removeDataStorageService() {
        this.dataStorageService = null;
    }

    void setTagColor(int color) {
        this.tagColor = color;
    }

    void setSelectionTracker(SelectionTracker<Long> selectionTracker) {
        this.selectionTracker = selectionTracker;
    }

    Record getRecordById(long id) {
        return idToRecord.get(id);
    }

    void deleteRecords(List<Record> recordsToDelete) {
        if (recordsToDelete != null && !recordsToDelete.isEmpty()) {
            for (Record record : recordsToDelete) {
                records.remove(record);
                int position = filteredRecords.indexOf(record);
                if (position != -1) {
                    filteredRecords.remove(position);
                    idToRecord.remove(record.getId());
                    notifyItemRemoved(position);
                }
            }
        }
    }

    void setPredicate(final NamedPredicate<Record> recordPredicate) {
        Log.i(this.getClass().getSimpleName(), "Set predicate " + recordPredicate);
        this.recordPredicate = recordPredicate;
        this.filteredRecords = filterRecords(records, recordPredicate);
        // TODO
        notifyDataSetChanged();
        mainActivity.onRecordFilterUpdated();
    }

    void clearPredicate() {
        setPredicate(null);
    }

    void updateRecords(@NonNull List<Long> recordIds) {
        Log.i(this.getClass().getSimpleName(), "Update records " + recordIds);
        for (long recordId : recordIds) {
            updateRecord(recordId);
        }
    }

    private void updateRecord(final long recordId) {
        Log.i(this.getClass().getSimpleName(), "Updating record #" + recordId);
        // Reload the latest changes from the database
        final Record record = dataStorageService.getRecord(recordId);
        if (record != null) {
            replaceUpdatedRecord(record);
            int position = filteredRecords.indexOf(record);
            if (position >= 0) {
                Log.i(this.getClass().getSimpleName(), "Item changed at position " + position);
                notifyItemChanged(position);
            }
        }
    }

    private void replaceUpdatedRecord(@NonNull Record record) {
        Util.replaceInList(record, records);
        Util.replaceInList(record, filteredRecords);
    }

    void onNewRecordAdded() {
        clearPredicate();
        updateRecord(records.get(0).getId());
        this.notifyItemChanged(0);
    }

    final class RecordViewHolder extends RecyclerView.ViewHolder {
        final TextView headerView;
        final TextView geoTagView;
        final TextView bodyView;
        final FlexboxLayout tagsBox;

        RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            headerView = itemView.findViewById(R.id.textViewRecordHeader);
            geoTagView = itemView.findViewById(R.id.textViewRecordGeoTag);
            tagsBox = itemView.findViewById(R.id.tagsBox);
            bodyView = itemView.findViewById(R.id.textViewRecordBody);
        }

        ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
            return new ItemDetailsLookup.ItemDetails<Long>() {
                @Override
                public int getPosition() {
                    return getAdapterPosition();
                }

                @Override
                public Long getSelectionKey() {
                    return getItemId();
                }
            };
        }
    }

    final static class RecordDetailsLookup extends ItemDetailsLookup<Long> {
        private final RecyclerView recyclerView;

        RecordDetailsLookup(RecyclerView recyclerView) {
            this.recyclerView = recyclerView;
        }

        @Override
        public @Nullable ItemDetails<Long> getItemDetails(@NonNull MotionEvent e) {
            View view = recyclerView.findChildViewUnder(e.getX(), e.getY());
            if (view != null) {
                RecyclerView.ViewHolder holder = recyclerView.getChildViewHolder(view);
                if (holder instanceof RecordViewHolder) {
                    return ((RecordViewHolder) holder).getItemDetails();
                }
            }
            return null;
        }
    }
}
