package com.becrux.tumblrstats;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.model.Token;
import org.scribe.oauth.OAuthService;

import java.util.ArrayList;
import java.util.List;

public interface UserInfo {
    enum PostFormat {
        Html,
        MarkDown,
        Raw
    };

    enum Type {
        Public,
        Private
    };

    enum Tweet {
        Auto,
        Yes,
        No
    };

    class Blog {
        private String name;       // String - the short name of the blog
        private String url;        // String - the URL of the blog
        private String title;      // String - the title of the blog
        private boolean primary;   // Boolean - indicates if this is the user's primary blog
        private int followers;     // Number - total count of followers for this blog
        private Tweet tweet;     // String - indicate if posts are tweeted auto, Y, N
        private boolean facebook;  // String - indicate if posts are sent to facebook Y, N
        private Type type;         // String - indicates whether a blog is public or private

        public Blog(
                String name,
                String url,
                String title,
                boolean primary,
                int followers,
                Tweet tweet,
                boolean facebook,
                Type type) {
            this.name = name;
            this.url = url;
            this.title = title;
            this.primary = primary;
            this.followers = followers;
            this.tweet = tweet;
            this.facebook = facebook;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }

        public String getTitle() {
            return title;
        }

        public boolean isPrimary() {
            return primary;
        }

        public int getFollowers() {
            return followers;
        }

        public Tweet getTweet() {
            return tweet;
        }

        public boolean isFacebook() {
            return facebook;
        }

        public Type getType() {
            return type;
        }
    };

    class Data {
        private int following;                // Number - The number of blogs the user is following
        private PostFormat defaultPostFormat; // String - The default posting format - html, markdown, or raw
        private String name;                  // String - The user's tumblr short name
        private int likes;                    // Number - The total count of the user's likes
        private List<Blog> blogs;             // Array - Each item is a blog the user has permissions to post to

        public Data(int following, PostFormat defaultPostFormat, String name, int likes, List<Blog> blogs) {
            this.following = following;
            this.defaultPostFormat = defaultPostFormat;
            this.name = name;
            this.likes = likes;
            this.blogs = blogs;
        }

        public int getFollowing() {
            return following;
        }

        public PostFormat getDefaultPostFormat() {
            return defaultPostFormat;
        }

        public String getName() {
            return name;
        }

        public int getLikes() {
            return likes;
        }

        public List<Blog> getBlogs() {
            return blogs;
        }
    };

    class Api extends TumblrApi<Data> {

        /*
        "response": {
          "user": {
            "following": 263,
            "default_post_format": "html",
            "name": "derekg",
            "likes": 606,
            "blogs": [
              {
                "name": "derekg",
                "title": "Derek Gottfrid",
                "url": "https://derekg.org/",
                "tweet": "auto",
                "primary": true,
                "followers": 33004929,
              },
              {
                "name": "ihatehipstrz",
                "title": "I Hate Hipstrz",
                ...
              }
            ]
          }
        }
       */

        public Api(OAuthService service, Token authToken, String appId, String appVersion) {
            super(service, authToken, appId, appVersion);
        }

        @Override
        protected String getPath() {
            return "/user/info";
        }

        @Override
        protected Data readData(JSONObject jsonObject) throws JSONException {
            JSONObject userObj = jsonObject.getJSONObject("user");

            List<Blog> blogs = new ArrayList<Blog>();

            JSONArray blogsArray = userObj.getJSONArray("blogs");
            for (int idx = 0; idx < blogsArray.length(); ++idx) {
                JSONObject blog = blogsArray.getJSONObject(idx);

                String tweet = blog.getString("tweet");
                Tweet vTweet;

                String type = blog.getString("type");
                Type vType;

                if (type.equalsIgnoreCase("public")) {
                    vType = Type.Public;
                } else {
                    vType = Type.Private;
                }

                if (tweet.equalsIgnoreCase("auto")) {
                    vTweet = Tweet.Auto;
                } else if (tweet.equalsIgnoreCase("y")) {
                    vTweet = Tweet.Yes;
                } else {
                    vTweet = Tweet.No;
                }

                blogs.add(new Blog(
                        blog.getString("name"),
                        blog.getString("url"),
                        blog.getString("title"),
                        blog.getBoolean("primary"),
                        blog.getInt("followers"),
                        vTweet,
                        blog.getString("facebook").equalsIgnoreCase("y"),
                        vType
                ));
            }

            String postFormat = userObj.getString("default_post_format");
            PostFormat vPostFormat;

            if (postFormat.equalsIgnoreCase("html")) {
                vPostFormat = PostFormat.Html;
            } else if (postFormat.equalsIgnoreCase("markdown")) {
                vPostFormat = PostFormat.MarkDown;
            } else {
                vPostFormat = PostFormat.Raw;
            }

            return new Data(
                    userObj.getInt("following"),
                    vPostFormat,
                    userObj.getString("name"),
                    userObj.getInt("likes"),
                    blogs
            );
        }
    }
}
