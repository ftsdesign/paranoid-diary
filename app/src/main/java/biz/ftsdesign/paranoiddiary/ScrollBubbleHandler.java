package biz.ftsdesign.paranoiddiary;

import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Timer;
import java.util.TimerTask;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE;

class ScrollBubbleHandler extends RecyclerView.OnScrollListener {
    private static final int BUBBLE_OFF_DELAY_MS = 5000;
    private final LinearLayoutManager layoutManager;
    private final TextView textView;
    private final RecordsViewAdapter recordsViewAdapter;
    private final Timer timer;
    private final MainActivity mainActivity;
    private TimerTask timerTask;

    ScrollBubbleHandler(@NonNull LinearLayoutManager layoutManager, @NonNull TextView textView,
                               @NonNull RecordsViewAdapter recordsViewAdapter, @NonNull MainActivity mainActivity) {
        this.layoutManager = layoutManager;
        this.textView = textView;
        this.recordsViewAdapter = recordsViewAdapter;
        this.mainActivity = mainActivity;
        this.timer = new Timer("BubbleTimer");
    }

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        boolean visible = newState != SCROLL_STATE_IDLE;
        textView.setVisibility(visible ? VISIBLE : INVISIBLE);
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        String s = null;
        int topItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition();
        if (topItemPosition >= 0) {
            int section = recordsViewAdapter.getSectionForPosition(topItemPosition);
            s = recordsViewAdapter.getSections()[section].toString();
        }

        if (s != null) {
            mainActivity.setScrollBubble(true, s);

            if (timerTask != null) {
                timerTask.cancel();
            }

            timerTask = new TimerTask() {
                @Override
                public void run() {
                    mainActivity.setScrollBubble(false, "");
                }
            };
            timer.schedule(timerTask, BUBBLE_OFF_DELAY_MS);
        }
    }
}
