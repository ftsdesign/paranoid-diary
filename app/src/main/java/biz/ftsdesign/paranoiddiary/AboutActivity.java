package biz.ftsdesign.paranoiddiary;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView version = findViewById(R.id.textViewVersion);
        version.setText(getVersionString());

        TextView about = findViewById(R.id.textViewAbout);
        about.setText(Html.fromHtml(getString(R.string.about_text)));
        about.setMovementMethod(new LinkMovementMethod());
        about.setFocusable(true);
        about.setTextIsSelectable(true);
    }

    private String getVersionString() {
        try {
            PackageInfo pInfo = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0);
            String versionName = pInfo.versionName;
            int versionCode = pInfo.versionCode;
            return getString(R.string.version_info, versionName, versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            return getString(R.string.version_unknown);
        }
    }
}
