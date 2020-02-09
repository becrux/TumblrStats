package com.becrux.tumblrstats;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.tumblr.jumblr.JumblrClient;
import com.tumblr.jumblr.exceptions.JumblrException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final MainActivity me = this;

        findViewById(R.id.btnAuthenticate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(me, LoginActivity.class);
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

        TextView tv = findViewById(R.id.textView);
        tv.setText("");

        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        String tokenKey = sharedPref.getString(Constants.OAUTH_TOKEN_KEY, "");

        if (!tokenKey.isEmpty()) {
            JumblrClient client = new JumblrClient(getString(R.string.consumer_key), getString(R.string.consumer_secret));
            client.setToken(tokenKey, sharedPref.getString(Constants.OAUTH_TOKEN_SECRET_KEY, ""));

            try {
                String userName = client.user().getName();

                tv.append("Token: " + tokenKey + "\n");
                tv.append("Secret: " + sharedPref.getString(Constants.OAUTH_TOKEN_SECRET_KEY, "") + "\n");

                tv.append("User: " + userName + "\n");
                tv.append("Blog: " + client.user().getBlogs().get(0).getName() + "\n");

            } catch (JumblrException e) {
                getPreferences(Context.MODE_PRIVATE).edit()
                        .remove(Constants.OAUTH_TOKEN_KEY)
                        .remove(Constants.OAUTH_TOKEN_SECRET_KEY)
                        .commit();

                tv.append("Not authenticated\n");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.PERFORM_LOGIN) {
            if(resultCode == Activity.RESULT_OK){
                JumblrClient client = new JumblrClient(getString(R.string.consumer_key), getString(R.string.consumer_secret));
                client.setToken(data.getStringExtra(Constants.OAUTH_TOKEN_KEY), data.getStringExtra(Constants.OAUTH_TOKEN_SECRET_KEY));

                TextView tv = findViewById(R.id.textView);

                tv.append("Token: " + data.getStringExtra(Constants.OAUTH_TOKEN_KEY) + "\n");
                tv.append("Secret: " + data.getStringExtra(Constants.OAUTH_TOKEN_SECRET_KEY) + "\n");

                tv.append("User: " + client.user().getName() + "\n");
                tv.append("Blog: " + client.user().getBlogs().get(0).getName() + "\n");

                getPreferences(Context.MODE_PRIVATE).edit()
                        .putString(Constants.OAUTH_TOKEN_KEY, data.getStringExtra(Constants.OAUTH_TOKEN_KEY))
                        .putString(Constants.OAUTH_TOKEN_SECRET_KEY, data.getStringExtra(Constants.OAUTH_TOKEN_SECRET_KEY))
                        .commit();
            }
            else if (resultCode == Activity.RESULT_CANCELED) {
            }
        }
    }
}
