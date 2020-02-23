package com.becrux.tumblrstats;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

import java.util.HashMap;
import java.util.Map;

public interface Likes {
    class Api extends TumblrApi<String> {

        private String blogId;

        public Api(
                Context context,
                OAuthService service,
                Token authToken,
                String appId,
                String appVersion,
                String[] additionalArgs) {
            super(context, service, authToken, appId, appVersion);

            this.blogId = additionalArgs[0];
        }

        @Override
        protected String getPath() {
            /*
            limit   Number  The number of results to return: 1–20, inclusive   default: 20
            offset  Number  Liked post number to start at                      default: 0 (first post)
            */

            return "/blog/" + blogId + ".tumblr.com/likes";
        }

        @Override
        protected Map<String, String> defaultParams() {
            return new HashMap<String, String>(){{
                put("api_key", getContext().getString(R.string.consumer_key));
            }};
        }

        @Override
        protected String readData(JSONObject jsonObject) throws JSONException {
            // TODO: needs a run
            return "";
        }
    }
}
