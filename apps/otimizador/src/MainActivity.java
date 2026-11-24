package org.tableta15.otimizador;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MainActivity extends Activity {
    private static final int GREEN = Color.rgb(18, 103, 86);
    private static final int DARK = Color.rgb(22, 43, 39);
    private static final AtomicBoolean BUSY = new AtomicBoolean();
    private TextView heading;
    private TextView detail;
    private TextView summary;
    private ProgressBar progress;
    private boolean scheduled;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        createScreen();

        if (Build.VERSION.SDK_INT >= 34) {
            heading.setText("Versão incompatível");
            detail.setText("Este atalho foi feito para o Android 9 do A15.");
            progress.setIndeterminate(false);
            return;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus || scheduled || Build.VERSION.SDK_INT >= 34) return;
        scheduled = true;
        // Allow the previous app's window to finish leaving the foreground.
        progress.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && !isDestroyed()) startCleaning();
            }
        }, 350);
    }

    private void startCleaning() {
        if (!BUSY.compareAndSet(false, true)) {
            finishAndRemoveTask();
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Cleanup.Result result = new Cleanup(getApplicationContext()).run(
                            new Cleanup.Progress() {
                                @Override
                                public void update(final int step, final int total, final String message) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (isFinishing() || isDestroyed()) return;
                                            progress.setIndeterminate(false);
                                            progress.setMax(total);
                                            progress.setProgress(step);
                                            detail.setText(message);
                                        }
                                    });
                                }
                            });
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!isFinishing() && !isDestroyed()) showResult(result);
                        }
                    });
                } catch (RuntimeException error) {
                    Log.w("OtimizarA15", "Falha ao executar a limpeza", error);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (isFinishing() || isDestroyed()) return;
                            heading.setText("Limpeza interrompida");
                            detail.setText("O Android não concluiu a operação.");
                            progress.setIndeterminate(false);
                        }
                    });
                } finally {
                    BUSY.set(false);
                }
            }
        }, "limpeza-a15").start();
    }

    private void createScreen() {
        getWindow().setStatusBarColor(GREEN);
        getWindow().setNavigationBarColor(GREEN);
        FrameLayout screen = new FrameLayout(this);
        screen.setBackgroundColor(Color.rgb(246, 250, 248));

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(32), dp(18), dp(32), dp(18));
        screen.addView(panel, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));

        heading = addText(panel, "Otimizando o A15", 29, DARK, 0);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        detail = addText(panel, "Identificando os apps…", 17, DARK, 12);
        detail.setAccessibilityLiveRegion(TextView.ACCESSIBILITY_LIVE_REGION_POLITE);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        progress.setProgressTintList(ColorStateList.valueOf(GREEN));
        progress.setIndeterminateTintList(ColorStateList.valueOf(GREEN));
        LinearLayout.LayoutParams bar = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(16));
        bar.topMargin = dp(12);
        panel.addView(progress, bar);

        summary = addText(panel, "", 16, DARK, 12);
        setContentView(screen);
    }

    private TextView addText(LinearLayout parent, String value, int size, int color, int margin) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(color);
        text.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layout.topMargin = dp(margin);
        parent.addView(text, layout);
        return text;
    }

    private void showResult(Cleanup.Result result) {
        progress.setProgress(progress.getMax());
        heading.setText(result.incomplete ? "Limpeza parcial" : "Concluído");
        detail.setText(result.requests == 0 ? "Nenhum app a verificar"
                : "Limpeza de segundo plano solicitada");

        StringBuilder text = new StringBuilder();
        if (result.ramBefore > 0 && result.ramAfter > 0) {
            text.append("RAM disponível  ").append(result.ramBefore / Cleanup.MIB)
                    .append(" → ").append(result.ramAfter / Cleanup.MIB).append(" MiB\n");
        }
        text.append(result.temporaryFiles == 0 ? "Sem temporários do atalho"
                : "Temporários removidos  " + result.temporaryFiles);
        if (result.storageAvailable > 0) {
            text.append("\n").append(String.format(new Locale("pt", "BR"),
                    "Espaço livre  %.2f GiB", result.storageAvailable / (1024.0 * Cleanup.MIB)));
        }
        if (result.storageRecoveryRequested) text.append("\nLimpeza de cache solicitada ao Android");
        if (result.lowStorage) text.append("\nArmazenamento ainda baixo");
        summary.setText(text);

        Log.i("OtimizarA15", "requests=" + result.requests + " ramBefore=" + result.ramBefore
                + " ramAfter=" + result.ramAfter + " temporaryFiles=" + result.temporaryFiles
                + " temporaryBytes=" + result.temporaryBytes
                + " storageRecoveryRequested=" + result.storageRecoveryRequested);

        progress.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing() && !isDestroyed()) finishAndRemoveTask();
            }
        }, result.incomplete || result.lowStorage ? 4200 : 2600);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
