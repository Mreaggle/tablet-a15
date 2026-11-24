package org.tableta15.shorts;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.FrameLayout;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

public final class MainActivity extends Activity {
    private static final String SHORTS_URL = "https://m.youtube.com/shorts/?hl=pt-BR&gl=BR";
    private WebView web;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMediaPlaybackRequiresUserGesture(false);

        FrameLayout layout = new FrameLayout(this);
        layout.addView(web, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        ProgressBar progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        int height = Math.round(3 * getResources().getDisplayMetrics().density);
        layout.addView(progress, new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, height));
        setContentView(layout);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return request.isForMainFrame() && !isYouTube(request.getUrl());
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int percent) {
                progress.setProgress(percent);
                progress.setVisibility(percent == 100 ? View.GONE : View.VISIBLE);
            }
        });
        if (state == null || web.restoreState(state) == null) web.loadUrl(SHORTS_URL);
    }

    private static boolean isYouTube(Uri url) {
        String host = url.getHost();
        return "https".equals(url.getScheme()) && host != null
                && (host.equals("youtube.com") || host.endsWith(".youtube.com"));
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        web.saveState(state);
    }

    @Override
    protected void onPause() {
        web.onPause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        FrameLayout layout = (FrameLayout) web.getParent();
        layout.removeView(web);
        web.destroy();
        super.onDestroy();
    }
}
