package com.becrux.tumblrstats;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

public interface Avatar {
    class Api extends TumblrApi<String> {

        private String blogId;
        private String size;

        public Api(
                Context context,
                OAuthService service,
                Token authToken,
                String appId,
                String appVersion,
                String[] additionalArgs) {
            super(context, service, authToken, appId, appVersion);

            this.blogId = additionalArgs[0];
            this.size = additionalArgs[1];
        }

        @Override
        protected String getPath() {
            return "/blog/" + blogId + ".tumblr.com/avatar/" + size;
        }

        @Override
        protected String readData(JSONObject jsonObject) throws JSONException {
            return jsonObject.getString("avatar_url");
        }
    }
}
