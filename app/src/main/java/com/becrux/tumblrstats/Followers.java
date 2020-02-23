package com.becrux.tumblrstats;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

public interface Followers {
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
            blog-identifier  String  Any blog identifier
            limit            Number  The number of results to return: 1–20, inclusive  default: 20
            offset           Number  Result to start at                                default: 0 (first follower)
            */

            return "/blog/" + blogId + ".tumblr.com/followers";
        }

        @Override
        protected String readData(JSONObject jsonObject) throws JSONException {
            // TODO: needs a run
            return "";
        }
    }
}
