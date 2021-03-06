package com.becrux.tumblrstats;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.exceptions.OAuthException;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import java.util.Map;

public abstract class TumblrApi<T> {

    public interface OnCompletion<T> {
        void onSuccess(T result);
        void onFailure(TumblrException e);
    }

    private static class TumblrCall<T> extends AsyncTask<OAuthRequest, Void, String> {

        private OnCompletion<T> onCompletion;
        private OAuthException networkException;
        private TumblrApi<T> api;

        private TumblrCall(TumblrApi<T> api, OnCompletion<T> onCompletion) {
            super();

            this.api = api;
            this.onCompletion = onCompletion;
            this.networkException = null;
        }

        @Override
        protected String doInBackground(OAuthRequest... requests) {
            try {
                return requests[0].send().getBody();
            } catch(OAuthException e) {
                networkException = e;
            }

            return null;
        }

        @Override
        protected void onPostExecute(String jsonResponse) {
            if (onCompletion == null)
                return;

            if (networkException != null) {
                onCompletion.onFailure(new TumblrNetworkException(networkException));
                return;
            }

            try {
                Log.v(Constants.APP_NAME, "JSON Response: " + jsonResponse);

                JSONObject rootObj = new JSONObject(jsonResponse);

                JSONObject metaObj = rootObj.getJSONObject("meta");
                int responseCode = metaObj.getInt("status");
                String responseMessage = metaObj.getString("msg");

                switch (responseCode) {
                    case 200:
                    case 201:
                        onCompletion.onSuccess(
                                api.readData(rootObj.getJSONObject("response"))
                        );
                        break;

                    default:
                        onCompletion.onFailure(
                                new TumblrResponseException(
                                        responseCode,
                                        responseMessage
                                )
                        );
                        break;
                }
            } catch (JSONException e) {
                onCompletion.onFailure(new TumblrJsonException(e));
            }
        }
    }

    private Context context;
    private OAuthService service;
    private Token authToken;
    private String appId;
    private String appVersion;

    protected TumblrApi(
            Context context,
            OAuthService service,
            Token authToken,
            String appId,
            String appVersion) {
        super();

        this.context = context;
        this.service = service;
        this.authToken = authToken;
        this.appId = appId;
        this.appVersion = appVersion;
    }

    protected Context getContext() {
        return context;
    }

    protected abstract String getPath();
    protected Map<String, String> defaultParams() {
        return null;
    }
    protected abstract T readData(JSONObject jsonObject) throws JSONException;

    public void call(Map<String, ?> queryParams, OnCompletion<T> onCompletion) {
        OAuthRequest request = new OAuthRequest(Verb.GET, Constants.API_ENDPOINT + getPath());

        if (defaultParams() != null) {
            for (Map.Entry<String, ?> entry : defaultParams().entrySet()) {
                request.addQuerystringParameter(entry.getKey(), entry.getValue().toString());
            }
        }

        if (queryParams != null) {
            for (Map.Entry<String, ?> entry : queryParams.entrySet()) {
                request.addQuerystringParameter(entry.getKey(), entry.getValue().toString());
            }
        }

        request.addHeader("User-Agent", appId + "/" + appVersion);

        Log.v(Constants.APP_NAME, "Request: " + request.toString());

        service.signRequest(authToken, request);

        new TumblrCall<T>(this, onCompletion).execute(request);
    }

    public void call(OnCompletion<T> onCompletion) {
        call(null, onCompletion);
    }
}
