package com.becrux.tumblrstats;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;
import oauth.signpost.commonshttp.CommonsHttpOAuthProvider;
import oauth.signpost.exception.OAuthCommunicationException;
import oauth.signpost.exception.OAuthExpectationFailedException;
import oauth.signpost.exception.OAuthMessageSignerException;
import oauth.signpost.exception.OAuthNotAuthorizedException;

public class LoginActivity extends Activity {

    private static final String CALLBACK_URL = "CallbackURL";

    private static final String REQUEST_TOKEN_END_POINT = "https://www.tumblr.com/oauth/request_token";
    private static final String ACCESS_TOKEN_URL = "https://www.tumblr.com/oauth/access_token";
    private static final String AUTHORIZE_URL = "https://www.tumblr.com/oauth/authorize";

    CommonsHttpOAuthConsumer commonsHttpOAuthConsumer;
    CommonsHttpOAuthProvider commonsHttpOAuthProvider;

    class CustomWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.toLowerCase().contains(CALLBACK_URL.toLowerCase())) {
                Uri uri = Uri.parse(url);
                for (String strQuery : uri.getQueryParameterNames())
                    if (strQuery.contentEquals("oauth_verifier")) {
                        try {
                            commonsHttpOAuthProvider.retrieveAccessToken(commonsHttpOAuthConsumer, uri.getQueryParameter(strQuery));
                        } catch (OAuthMessageSignerException e) {
                            e.printStackTrace();
                        } catch (OAuthNotAuthorizedException e) {
                            e.printStackTrace();
                        } catch (OAuthExpectationFailedException e) {
                            e.printStackTrace();
                        } catch (OAuthCommunicationException e) {
                            e.printStackTrace();
                        }

                        Intent returnIntent = new Intent();
                        returnIntent.putExtra(Constants.OAUTH_TOKEN_KEY, commonsHttpOAuthConsumer.getToken());
                        returnIntent.putExtra(Constants.OAUTH_TOKEN_SECRET_KEY, commonsHttpOAuthConsumer.getTokenSecret());
                        setResult(Activity.RESULT_OK,returnIntent);
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

        /* This is temporary */
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        webView = findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new CustomWebViewClient());

        commonsHttpOAuthConsumer =
                new CommonsHttpOAuthConsumer(getString(R.string.consumer_key), getString(R.string.consumer_secret));

        commonsHttpOAuthProvider
                = new CommonsHttpOAuthProvider(REQUEST_TOKEN_END_POINT, ACCESS_TOKEN_URL, AUTHORIZE_URL);

        try {
            webView.loadUrl(commonsHttpOAuthProvider.retrieveRequestToken(commonsHttpOAuthConsumer, CALLBACK_URL));
        } catch (OAuthMessageSignerException e) {
            e.printStackTrace();
        } catch (OAuthNotAuthorizedException e) {
            e.printStackTrace();
        } catch (OAuthExpectationFailedException e) {
            e.printStackTrace();
        } catch (OAuthCommunicationException e) {
            e.printStackTrace();
        }
    }
}
