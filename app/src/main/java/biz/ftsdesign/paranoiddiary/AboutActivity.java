package biz.ftsdesign.paranoiddiary;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView about = findViewById(R.id.textViewAbout);
        about.setText(Html.fromHtml(getString(R.string.about_text)));
        about.setMovementMethod(new LinkMovementMethod());
        about.setFocusable(true);
        about.setTextIsSelectable(true);
    }
}
