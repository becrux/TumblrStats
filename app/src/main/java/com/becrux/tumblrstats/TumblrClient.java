package com.becrux.tumblrstats;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.util.Log;

import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.exceptions.JumblrException;

import org.scribe.builder.ServiceBuilder;
import org.scribe.exceptions.OAuthConnectionException;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.Token;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;

import java.io.IOException;
import java.io.OutputStream;

public final class TumblrClient {

    public interface OnFailureListener {
        void onFailure(JumblrException e);
        void onNetworkFailure(OAuthException e);
    }

    private interface OnSuccessListener<T> {
        void onSuccess(T result);
    }

    private interface TumblrCommand<T> {
        T execute();
    }

    public interface OnLoginListener {
        void onAccessGranted();
        void onAccessDenied();
    }

    public interface OnAuthenticationListener {
        void onAuthentication(Token requestToken, String authenticationUrl);
    }

    private class RequestTokenTask extends AsyncTask<Void, Void, String> {
        private OnFailureListener onFailureListener;
        private OnAuthenticationListener onAuthenticationListener;
        private Token requestToken;
        private OAuthService oAuthService;
        private OAuthException networkExc;

        public RequestTokenTask(
                OAuthService oAuthService,
                OnFailureListener onFailureListener,
                OnAuthenticationListener onAuthenticationListener) {
            super();

            this.requestToken = null;
            this.oAuthService = oAuthService;
            this.onFailureListener = onFailureListener;
            this.onAuthenticationListener = onAuthenticationListener;
            this.networkExc = null;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                requestToken = oAuthService.getRequestToken();
                Log.v(Constants.APP_NAME, "Token is:" + requestToken.toString());

                return oAuthService.getAuthorizationUrl(requestToken);
            } catch (OAuthConnectionException e) {
                networkExc = e;
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                onFailureListener.onNetworkFailure(networkExc);
            } else {
                onAuthenticationListener.onAuthentication(requestToken, result);
            }
        }
    }

    private class AccessTokenTask extends AsyncTask<Void, Void, Token> {
        private OnLoginListener onLoginListener;
        private OnFailureListener onFailureListener;
        private String authVerifier;
        private Token requestToken;
        private OAuthService oAuthService;
        private OAuthException authExc;
        private OAuthException networkExc;

        public AccessTokenTask(
                String authVerifier,
                Token requestToken,
                OAuthService oAuthService,
                OnLoginListener onLoginListener,
                OnFailureListener onFailureListener) {
            super();

            this.authVerifier = authVerifier;
            this.requestToken = requestToken;
            this.oAuthService = oAuthService;
            this.onLoginListener = onLoginListener;
            this.onFailureListener = onFailureListener;
            this.authExc = null;
            this.networkExc = null;
        }

        @Override
        protected Token doInBackground(Void... voids) {
            try {
                return oAuthService.getAccessToken(requestToken, new Verifier(authVerifier));
            } catch (OAuthConnectionException e) {
                networkExc = e;
            } catch (OAuthException e) {
                authExc = e;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Token authToken) {
            if (authToken == null) {
                if (networkExc != null) {
                    if (onFailureListener != null)
                        onFailureListener.onNetworkFailure(networkExc);
                } else {
                    if (onLoginListener != null)
                        onLoginListener.onAccessDenied();
                }
            } else {
                context.getSharedPreferences(Constants.APP_NAME, Context.MODE_PRIVATE)
                        .edit()
                        .putString(Constants.OAUTH_TOKEN_KEY, authToken.getToken())
                        .putString(Constants.OAUTH_TOKEN_SECRET_KEY, authToken.getSecret())
                        .commit();

                if (onLoginListener != null)
                    onLoginListener.onAccessGranted();
            }
        }
    }

    private class ExecuteTumblrCommandTask<T> extends AsyncTask<Void, Void, T> {

        private TumblrCommand<T> cmd;
        private OnSuccessListener<T> onSuccessListener;
        private OnFailureListener onFailureListener;
        private JumblrException jumblrExc;
        private OAuthConnectionException networkExc;

        public ExecuteTumblrCommandTask(TumblrCommand<T> cmd,
                                        OnSuccessListener<T> onSuccessListener,
                                        OnFailureListener onFailureListener) {
            super();

            this.cmd = cmd;
            this.onSuccessListener = onSuccessListener;
            this.onFailureListener = onFailureListener;
        }

        @Override
        protected T doInBackground(Void... voids) {
            try {
                return cmd.execute();
            } catch (JumblrException e) {
                jumblrExc = e;
            } catch (OAuthConnectionException e) {
                networkExc = e;
            }

            return null;
        }

        @Override
        protected void onPostExecute(T result) {
            if (networkExc != null) {
                onFailureListener.onNetworkFailure(networkExc);
            } else if (jumblrExc != null) {
                onFailureListener.onFailure(jumblrExc);
            } else {
                onSuccessListener.onSuccess(result);
            }
        }
    }

    private class LogOutputStream extends OutputStream {

        private String mem;

        public LogOutputStream() {
            mem = "";
        }

        @Override
        public void write(int b) throws IOException {
            char c = (char) (b & 0xff);
            if (c == '\n') {
                flush();
            } else {
                mem += new Character(c);
            }
        }

        public void flush () {
            Log.v(Constants.APP_NAME, mem);
            mem = "";
        }
    }

    private JumblrClient client;
    private Context context;
    private OAuthService oAuthService;
    private OnLoginListener onLoginListener;
    private OnFailureListener onFailureListener;
    private OnAuthenticationListener onAuthenticationListener;

    public TumblrClient(Context context) {
        super();

        this.context = context;
        this.oAuthService = null;
        this.onLoginListener = null;
        this.onFailureListener = null;
        this.onAuthenticationListener = null;

        Resources r = context.getResources();
        client = new JumblrClient(
                r.getString(R.string.consumer_key),
                r.getString(R.string.consumer_secret)
        );
    }

    private void doLogin() {
        if (onAuthenticationListener == null)
            return;

        oAuthService = new ServiceBuilder()
                .apiKey(context.getString(R.string.consumer_key))
                .apiSecret(context.getString(R.string.consumer_secret))
                .provider(OAuthTumblrApi.class)
                .callback(Constants.CALLBACK_URL)
                .debugStream(new LogOutputStream())
                .build();

        new RequestTokenTask(oAuthService, onFailureListener, onAuthenticationListener).execute();
    }

    public void login() {

        final SharedPreferences prefs = context.getSharedPreferences(Constants.APP_NAME, Context.MODE_PRIVATE);

        if (prefs.contains(Constants.OAUTH_TOKEN_KEY) && prefs.contains(Constants.OAUTH_TOKEN_SECRET_KEY)) {
            // ok, we already have authentication tokens, let's try them first
            client.setToken(
                    prefs.getString(Constants.OAUTH_TOKEN_KEY, ""),
                    prefs.getString(Constants.OAUTH_TOKEN_SECRET_KEY, "")
            );

            new ExecuteTumblrCommandTask<String>(new TumblrCommand<String>() {
                @Override
                public String execute() {
                    return client.user().getName();
                }
            }, new OnSuccessListener<String>() {
                @Override
                public void onSuccess(String result) {
                    // if it was allocated during an authentication phase, we don't need this anymore
                    oAuthService = null;

                    if (onLoginListener != null)
                        onLoginListener.onAccessGranted();
                }
            }, new OnFailureListener() {
                @Override
                public void onNetworkFailure(OAuthException e) {
                    // we cannot reach Tumblr, fail but do not remove our tokens
                    if (onFailureListener != null)
                        onFailureListener.onNetworkFailure(e);
                }

                @Override
                public void onFailure(JumblrException e) {
                    // we can reach Tumblr, but we cannot access it. So, throw away our tokens, and
                    // let's ask a new authentication
                    prefs.edit()
                            .remove(Constants.OAUTH_TOKEN_KEY)
                            .remove(Constants.OAUTH_TOKEN_SECRET_KEY)
                            .commit();

                    doLogin();
                }
            }).execute();
        } else {
            // never logged in before, do that
            doLogin();
        }
    }

    public void login(Token requestToken, String authVerifier) {
        if ((authVerifier == null) || authVerifier.isEmpty()) {
            if (onLoginListener != null)
                onLoginListener.onAccessDenied();

            return;
        }

        new AccessTokenTask(
                authVerifier,
                requestToken,
                oAuthService,
                new OnLoginListener() {
                    @Override
                    public void onAccessGranted() {
                        login();
                    }

                    @Override
                    public void onAccessDenied() {
                        if (onLoginListener != null)
                            onLoginListener.onAccessDenied();
                    }
                },
                onFailureListener).execute();
    }

    public void setOnLoginListener(OnLoginListener onLoginListener) {
        this.onLoginListener = onLoginListener;
    }

    public void setOnAuthenticationListener(OnAuthenticationListener onAuthenticationListener) {
        this.onAuthenticationListener = onAuthenticationListener;
    }

    public void setOnFailureListener(OnFailureListener onFailureListener) {
        this.onFailureListener = onFailureListener;
    }
}
