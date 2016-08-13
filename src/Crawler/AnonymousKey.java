package Crawler;

/**
 * Created by RobbinNi on 7/6/16.
 */
public final class AnonymousKey implements GithubKey {

    private final String client_id, client_secret;

    public AnonymousKey(String client_id, String client_secret) {
        this.client_id = client_id;
        this.client_secret = client_secret;
    }

    @Override
    public String authAPI(String url) {
        return url + (url.contains("?") ? "&" : "?") + "client_id=" + client_id + "&" + "client_secret=" + client_secret;
    }
}
