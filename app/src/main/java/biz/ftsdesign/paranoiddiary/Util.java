package biz.ftsdesign.paranoiddiary;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.flexbox.FlexboxLayout;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import biz.ftsdesign.paranoiddiary.model.Record;
import biz.ftsdesign.paranoiddiary.model.Tag;
import biz.ftsdesign.paranoiddiary.predicate.TagPredicate;

public class Util {
    @SuppressWarnings("SameParameterValue")
    static void toastShort(@Nullable Activity activity, @NonNull String message) {
        if (activity != null) {
            Toast toast = Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_SHORT);
            toast.show();
        }
    }

    static void toastException(@Nullable Activity activity, @NonNull Exception e) {
        if (activity != null) {
            String message = e.getMessage() != null ? e.getMessage() : e.toString();
            toastError(activity, message);
        }
    }

    static void toastError(@NonNull Activity activity, @NonNull String message) {
        Log.e(activity.getClass().getCanonicalName(), message);
        Toast toast = Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_SHORT);
        toast.show();
    }

    static void setThemeGlobal(String theme) {
        final int value;
        switch (theme) {
            case "MODE_NIGHT_NO":
                value = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case "MODE_NIGHT_YES":
            default:
                value = AppCompatDelegate.MODE_NIGHT_YES;
                break;
        }
        AppCompatDelegate.setDefaultNightMode(value);
    }

    @SuppressLint("SetTextI18n")
    static void setTagsBox(@NonNull Activity activity, @NonNull FlexboxLayout tagsBox,
                           @NonNull Record record, @Nullable RecordPredicateListener recordPredicateListener) {
        Log.d(Util.class.getSimpleName(), "setTagsBox " + record + " " + record.getTags());
        tagsBox.removeAllViewsInLayout();
        if (!record.getTags().isEmpty()) {
            int index = 0;
            for (Tag tag : record.getTags()) {
                TextView textViewTag = (TextView) activity.getLayoutInflater().inflate(R.layout.tag_button_layout, null);
                textViewTag.setText("#" + tag.getName());
                FlexboxLayout.LayoutParams layoutParams = new FlexboxLayout.LayoutParams(
                        FlexboxLayout.LayoutParams.WRAP_CONTENT,
                        FlexboxLayout.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(0, 0, 10, 10);
                textViewTag.setLayoutParams(layoutParams);
                if (recordPredicateListener != null) {
                    textViewTag.setOnClickListener(v -> recordPredicateListener.setRecordPredicate(new TagPredicate(tag)));
                }
                tagsBox.addView(textViewTag, index++);
            }
            tagsBox.setVisibility(View.VISIBLE);
        } else {
            tagsBox.setVisibility(View.GONE);
        }
    }

    static <T> void replaceInList (@NonNull T t, @NonNull List<T> list) {
        int i = list.indexOf(t);
        if (i != -1) {
            list.set(i, t);
        }
    }

    @NonNull
    static Map<Tag, MultiSelectionState> getTagSelectionState(@NonNull Record record) {
        final Map<Tag, MultiSelectionState> tagIdToSelectionState = new HashMap<>();
        for (Tag tag : record.getTags()) {
            tagIdToSelectionState.put(tag, MultiSelectionState.ALL);
        }
        return tagIdToSelectionState;
    }

    static String composeRecordHeader(Record record) {
        return RecordHeaderFormat.FORMAT_DEFAULT.format(record);
    }

    @SuppressWarnings("SameParameterValue")
    @NonNull
    static String cutString(@NonNull String text, int numberOfCharacters) {
        if (text.length() <= numberOfCharacters) {
            return text;
        } else {
            return text.substring(0, numberOfCharacters - 1) + "…";
        }
    }

    public static String toString(@NonNull Tag tag) {
        return "#" + tag.getName();
    }

    public static String toString(@NonNull Collection<Tag> tags) {
        StringBuilder sb = new StringBuilder();
        for (Tag tag : tags) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append(toString(tag));
        }
        return sb.toString();
    }
}
