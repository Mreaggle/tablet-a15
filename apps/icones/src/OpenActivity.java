package org.tableta15.icones;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public final class OpenActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Targets.App app = Targets.App.find(getIntent().getStringExtra(Targets.EXTRA_ICON));
        if (app != null) {
            Intent launch = getPackageManager().getLaunchIntentForPackage(app.packageName);
            try {
                if (launch == null) throw new ActivityNotFoundException();
                startActivity(launch);
            } catch (ActivityNotFoundException | SecurityException error) {
                Toast.makeText(this, app.label + " indisponível.", Toast.LENGTH_SHORT).show();
            }
        }
        finish();
    }
}
