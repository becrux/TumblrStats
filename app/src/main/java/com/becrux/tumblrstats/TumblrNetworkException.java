package com.becrux.tumblrstats;

import org.scribe.exceptions.OAuthException;

public class TumblrNetworkException extends TumblrException {
    private OAuthException e;

    public TumblrNetworkException(OAuthException e) {
        super(e.getMessage());

        this.e = e;
    }

    public OAuthException getException() {
        return e;
    }
}
