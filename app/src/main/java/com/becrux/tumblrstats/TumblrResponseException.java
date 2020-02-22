package com.becrux.tumblrstats;

public class TumblrResponseException extends TumblrException {
    private int responseCode;
    private String responseMessage;

    public TumblrResponseException(int responseCode, String responseMessage) {
        super(responseMessage);
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }
}
