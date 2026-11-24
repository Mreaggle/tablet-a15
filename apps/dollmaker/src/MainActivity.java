package org.tableta15.dollmaker;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public final class MainActivity extends Activity {
    private static final String ASSET_HOST = "appassets.androidplatform.net";
    private static final String ASSET_PREFIX = "/assets/";
    private WebView webView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowContentAccess(false);
        webView.setWebViewClient(new OfflineClient());
        setContentView(webView);
        webView.loadUrl("https://" + ASSET_HOST + ASSET_PREFIX + "index.html");
    }

    private static boolean isLocal(Uri uri) {
        return "https".equals(uri.getScheme()) && ASSET_HOST.equals(uri.getHost())
                && uri.getPath() != null && uri.getPath().startsWith(ASSET_PREFIX);
    }

    private final class OfflineClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return !isLocal(request.getUrl());
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if (isLocal(uri)) {
                String name = uri.getPath().substring(ASSET_PREFIX.length());
                if (!name.contains("..") && !name.contains("\\")) {
                    try {
                        return new WebResourceResponse(mimeType(name),
                                name.endsWith(".png") ? null : "UTF-8", getAssets().open(name));
                    } catch (IOException missingAsset) {
                        // The original game probes its numbered image assets.
                    }
                }
            }
            return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", null,
                    new ByteArrayInputStream(new byte[0]));
        }

        private String mimeType(String name) {
            if (name.endsWith(".js")) return "application/javascript";
            if (name.endsWith(".css")) return "text/css";
            if (name.endsWith(".png")) return "image/png";
            if (name.endsWith(".html")) return "text/html";
            return "text/plain";
        }
    }
}
