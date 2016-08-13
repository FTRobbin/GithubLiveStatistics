package Data;

import com.google.gson.JsonObject;

import java.text.ParseException;
import java.util.Date;

/**
 * Created by RobbinNi on 7/6/16.
 */
public class GithubUser {
    public int id;
    public String login;
    public Date crawledAt;

    @Deprecated
    public GithubUser(int id, String login, Date crawledAt) {
        this.id = id;
        this.login = login;
        this.crawledAt = crawledAt;
    }

    public GithubUser(JsonObject json) {
        this.id = json.get("id").getAsInt();
        this.login = json.get("login").getAsString();
        this.crawledAt = new Date();
    }
}
