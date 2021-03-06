package com.becrux.tumblrstats;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.util.Log;

import org.scribe.builder.ServiceBuilder;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public final class TumblrClient {

    public interface OnCompletion<T> {
        void onSuccess(T result);
    }

    public interface OnFailureListener {
        void onFailure(TumblrException e);
        void onNetworkFailure(OAuthException e);
    }

    public interface OnLoginListener {
        void onAccessGranted();
        void onAccessRequest(
                TumblrAuthenticate authenticator,
                Token requestToken,
                String authenticationUrl);
        void onAccessDenied();
    }

    private Context context;
    private String appName;
    private String appVersion;

    private Token authToken;
    private OAuthService oAuthService;
    private OnLoginListener onLoginListener;
    private OnFailureListener onFailureListener;

    private UserInfo.Data me;

    public TumblrClient(Context context) {
        super();

        this.context = context;

        authToken = null;
        oAuthService = new ServiceBuilder()
                .provider(OAuthTumblrApi.class)
                .apiKey(context.getString(R.string.consumer_key))
                .apiSecret(context.getString(R.string.consumer_secret))
                .build();
        onLoginListener = null;
        onFailureListener = null;

        this.me = null;

        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            appName = pInfo.applicationInfo.name;
            appVersion = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen, anyway it isn't a big deal
        }
    }

    private void doLogin() {
        final TumblrAuthenticate auth = new TumblrAuthenticate(appName, context);

        auth.setOnAuthenticationListener(new TumblrAuthenticate.OnAuthenticationListener() {
            @Override
            public void onAuthenticationRequest(
                    TumblrAuthenticate authenticator,
                    Token requestToken,
                    String authenticationUrl) {
                onLoginListener.onAccessRequest(authenticator, requestToken, authenticationUrl);
            }

            @Override
            public void onAuthenticationGranted(Token accessToken) {
                // redo user request, this time should work
                login(accessToken);
            }

            @Override
            public void onFailure(OAuthException exception) {
                onLoginListener.onAccessDenied();
            }
        });
        auth.request();
    }

    private void login(Token authToken) {
        this.authToken = authToken;

        new UserInfo.Api(context, oAuthService, authToken, appName, appVersion, null)
                .call(new TumblrApi.OnCompletion<UserInfo.Data>() {
                    @Override
                    public void onSuccess(UserInfo.Data result) {
                        me = result;

                        if (onLoginListener != null)
                            onLoginListener.onAccessGranted();
                    }

                    @Override
                    public void onFailure(TumblrException e) {
                        if (e instanceof TumblrNetworkException) {
                            // we cannot reach Tumblr, fail but do not remove our tokens
                            onFailureListener.onNetworkFailure(((TumblrNetworkException) e).getException());
                        } else {
                            // we can reach Tumblr, but we cannot access it. So, throw away our tokens, and
                            // let's ask a new authentication
                            Log.v(Constants.APP_NAME, "Auth token not valid");

                            context.getSharedPreferences(Constants.APP_NAME, Context.MODE_PRIVATE)
                                    .edit()
                                    .remove(Constants.OAUTH_TOKEN_KEY)
                                    .remove(Constants.OAUTH_TOKEN_SECRET_KEY)
                                    .apply();

                            doLogin();
                        }
                    }
                });
    }

    public void login() {

        SharedPreferences prefs = context.getSharedPreferences(Constants.APP_NAME, Context.MODE_PRIVATE);

        if (prefs.contains(Constants.OAUTH_TOKEN_KEY) && prefs.contains(Constants.OAUTH_TOKEN_SECRET_KEY)) {

            // ok, we already have authentication tokens, let's try them first
            authToken = new Token(
                    prefs.getString(Constants.OAUTH_TOKEN_KEY, ""),
                    prefs.getString(Constants.OAUTH_TOKEN_SECRET_KEY, "")
            );
            Log.v(Constants.APP_NAME, "Stored Access Token: " + authToken);

            login(authToken);
        } else {
            // never logged in before, do that
            doLogin();
        }
    }

    public <T> void call(
            Class<? extends TumblrApi<T>> clazz,
            Map<String, ?> queryParams,
            final OnCompletion<T> onCompletion,
            String ... additionalArgs) {

        try {
            Class[] cArg = new Class[] {
                    Context.class,
                    OAuthService.class,
                    Token.class,
                    String.class,
                    String.class,
                    String[].class
                };

            clazz.getDeclaredConstructor(cArg)
                    .newInstance(
                            context,
                            oAuthService,
                            authToken,
                            appName,
                            appVersion,
                            additionalArgs)
                    .call(queryParams, new TumblrApi.OnCompletion<T>() {
                        @Override
                        public void onSuccess(T result) {
                            if (onCompletion != null)
                                onCompletion.onSuccess(result);
                        }

                        @Override
                        public void onFailure(TumblrException e) {
                            if (onFailureListener != null)
                                onFailureListener.onFailure(e);
                        }
                    });
        } catch (IllegalAccessException |
                NoSuchMethodException |
                InvocationTargetException |
                InstantiationException e) {
            e.printStackTrace();
        }
    }

    public <T> void call(Class<? extends TumblrApi<T>> clazz,
                         OnCompletion<T> onCompletion,
                         String ... additionalArgs) {
        call(clazz, null, onCompletion, additionalArgs);
    }

    public void setOnLoginListener(OnLoginListener onLoginListener) {
        this.onLoginListener = onLoginListener;
    }

    public void setOnFailureListener(OnFailureListener onFailureListener) {
        this.onFailureListener = onFailureListener;
    }

    public UserInfo.Data getMe() {
        return me;
    }
}
