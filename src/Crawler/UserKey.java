package Crawler;

/**
 * Created by RobbinNi on 7/6/16.
 */
public class UserKey implements GithubKey {

    private final String user, token;

    public UserKey(String user, String token) {
        this.user = user;
        this.token = token;
    }

    @Override
    public String authAPI(String url) {
        return url + (url.contains("?") ? "&" : "?") + "access_token=" + token;
        //.replace("https://", "https://" + user + ":" + token + "@");
    }
}
