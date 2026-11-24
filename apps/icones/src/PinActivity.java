package org.tableta15.icones;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.util.Collections;

public final class PinActivity extends Activity {
    private Targets.App pending;
    private LinearLayout actions;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        actions.setPadding(padding, padding, padding, padding);
        for (final Targets.App app : Targets.App.values()) {
            Button button = new Button(this);
            button.setText("Adicionar " + app.label);
            button.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) { pin(app); }
            });
            actions.addView(button);
        }
        setContentView(actions);
        if (state == null) pending = Targets.App.find(getIntent().getStringExtra(Targets.EXTRA_ICON));
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus || pending == null) return;
        Targets.App app = pending;
        pending = null;
        pin(app);
    }

    private void pin(Targets.App app) {
        ShortcutManager manager = getSystemService(ShortcutManager.class);
        if (!manager.isRequestPinShortcutSupported()) {
            Toast.makeText(this, "A tela inicial não permite fixar atalhos.", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent open = new Intent(this, OpenActivity.class)
                .setAction(Intent.ACTION_VIEW).putExtra(Targets.EXTRA_ICON, app.id);
        ShortcutInfo shortcut = new ShortcutInfo.Builder(this, app.id)
                .setShortLabel(app.label).setIcon(Icon.createWithResource(this, app.icon))
                .setIntent(open).build();
        for (ShortcutInfo pinned : manager.getPinnedShortcuts()) {
            if (app.id.equals(pinned.getId())) {
                manager.updateShortcuts(Collections.singletonList(shortcut));
                Toast.makeText(this, app.label + " já está na tela inicial.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        manager.requestPinShortcut(shortcut, null);
    }
}
