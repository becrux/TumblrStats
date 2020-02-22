package com.becrux.tumblrstats;

import org.json.JSONException;

public class TumblrJsonException extends TumblrException {
    private JSONException e;

    public TumblrJsonException(JSONException e) {
        super(e.getMessage());
    }

    public JSONException getException() {
        return e;
    }
}
