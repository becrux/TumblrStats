package com.becrux.tumblrstats;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;

public class LogOutputStream extends OutputStream {
    private String appId;
    private String mem;

    public LogOutputStream(String appId) {
        super();

        this.appId = appId;
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

    @Override
    public void flush () {
        Log.v(appId, mem);
        mem = "";
    }
}
