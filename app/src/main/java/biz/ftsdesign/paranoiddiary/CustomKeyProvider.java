package biz.ftsdesign.paranoiddiary;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.widget.RecyclerView;

class CustomKeyProvider extends ItemKeyProvider<Long>{
    private final RecordsViewAdapter adapter;
    private final RecyclerView recyclerView;

    CustomKeyProvider(RecyclerView recyclerView, RecordsViewAdapter adapter) {
        super(SCOPE_MAPPED);
        this.recyclerView = recyclerView;
        this.adapter = adapter;
    }

    @Override
    @Nullable
    public Long getKey(int position) {
        return adapter.getItemId(position);
    }

    @Override
    public int getPosition(@NonNull Long key) {
        RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForItemId(key);
        return viewHolder == null ? RecyclerView.NO_POSITION : viewHolder.getLayoutPosition();
    }
}
