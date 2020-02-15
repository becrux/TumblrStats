package com.becrux.tumblrstats;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.scribe.model.Token;

public class LoginActivity extends Activity {

    class CustomWebViewClient extends WebViewClient {
        private Token token;

        public CustomWebViewClient(Token token) {
            super();

            this.token = token;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            if (url.toLowerCase().contains(Constants.CALLBACK_URL.toLowerCase())) {
                Uri uri = Uri.parse(url);
                for (String strQuery : uri.getQueryParameterNames())
                    if (strQuery.contentEquals(Constants.OAUTH_VERIFIER)) {

                        Log.v(Constants.APP_NAME, "Auth Verifier: " + uri.getQueryParameter(strQuery));

                        Intent returnIntent = new Intent();
                        returnIntent.putExtra(Constants.REQUEST_TOKEN, token);
                        returnIntent.putExtra(Constants.OAUTH_VERIFIER, uri.getQueryParameter(strQuery));
                        setResult(Activity.RESULT_OK, returnIntent);
                        finish();

                        return true;
                    }
            }

            return false;
        }
    }

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new CustomWebViewClient(
                (Token) getIntent().getSerializableExtra(Constants.REQUEST_TOKEN))
        );

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(getIntent().getStringExtra(Constants.AUTH_URL));
            }
        });
    }
}
