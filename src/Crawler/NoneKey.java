package Crawler;

/**
 * Created by RobbinNi on 7/6/16.
 */
public final class NoneKey implements GithubKey {

    @Override
    public String authAPI(String url) {
        return url;
    }
}
