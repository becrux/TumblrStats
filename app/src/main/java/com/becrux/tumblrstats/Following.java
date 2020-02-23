package com.becrux.tumblrstats;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

public interface Following {
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
            offset  Number  Followed blog index to start at                    default: 0 (most-recently followed)
            */

            return "/blog/" + blogId + ".tumblr.com/following";
        }

        @Override
        protected String readData(JSONObject jsonObject) throws JSONException {
            // TODO: needs a run
            return "";
        }
    }
}
