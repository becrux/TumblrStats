package com.becrux.tumblrstats;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.tumblr.jumblr.exceptions.JumblrException;

import org.scribe.exceptions.OAuthException;
import org.scribe.model.Token;

public class MainActivity extends AppCompatActivity {

    private TumblrClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        client = new TumblrClient(getApplicationContext());

        final TextView tv = findViewById(R.id.textView);
        tv.setText("");
        client.setOnLoginListener(new TumblrClient.OnLoginListener() {
            @Override
            public void onAccessGranted() {
                tv.append("Logged in!\n");
            }

            @Override
            public void onAccessDenied() {
                tv.append("Login failed\n");
            }
        });
        client.setOnFailureListener(new TumblrClient.OnFailureListener() {
            @Override
            public void onFailure(JumblrException e) {
                tv.append("Command failure\n");
            }

            @Override
            public void onNetworkFailure(OAuthException e) {
                tv.append("Network error\n");
            }
        });

        final Activity me = this;
        client.setOnAuthenticationListener(new TumblrClient.OnAuthenticationListener() {
            @Override
            public void onAuthentication(Token requestToken, String authenticationUrl) {
                Intent i = new Intent(me, LoginActivity.class);
                i.putExtra(Constants.REQUEST_TOKEN, requestToken);
                i.putExtra(Constants.AUTH_URL, authenticationUrl);
                startActivityForResult(i, Constants.PERFORM_LOGIN);
            }
        });

        findViewById(R.id.btnClearTokens).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPreferences(Context.MODE_PRIVATE).edit()
                        .remove(Constants.OAUTH_TOKEN_KEY)
                        .remove(Constants.OAUTH_TOKEN_SECRET_KEY)
                        .commit();
            }
        });

        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                client.login();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.v(Constants.APP_NAME, "ActivityResult");

        switch (requestCode) {
            case Constants.PERFORM_LOGIN:
                if (resultCode != RESULT_OK) {
                    ((TextView) findViewById(R.id.textView)).append("Login failed\n");
                    return;
                }

                client.login(
                        (Token) data.getSerializableExtra(Constants.REQUEST_TOKEN),
                        data.getStringExtra(Constants.OAUTH_VERIFIER)
                );
                break;

            default:
                break;
        }
    }
}
