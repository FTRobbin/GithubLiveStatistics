package Data;

import com.google.gson.JsonObject;

import java.text.ParseException;
import java.util.Date;

import static Data.DataUtility.getDateFromGithubFormat;

/**
 * Created by RobbinNi on 7/6/16.
 */
public class GithubRepo {
    public int id, ownerid;
    public String name, url;
    public Date crawledAt, createdAt, updatedAt, clonedAt;

    @Deprecated
    public GithubRepo(int id, int ownerid, String name, String url, Date crawledAt, Date updatedAt, Date clonedAt) {
        this.id = id;
        this.ownerid = ownerid;
        this.name = name;
        this.url = url;
        this.crawledAt = crawledAt;
        this.updatedAt = updatedAt;
        this.clonedAt = clonedAt;
        this.createdAt = null;
    }

    public GithubRepo(JsonObject json) throws ParseException {
        this.id = json.get("id").getAsInt();
        this.ownerid = json.get("owner").getAsJsonObject().get("id").getAsInt();
        this.name = json.get("name").getAsString();
        this.url = json.get("git_url").getAsString();
        this.crawledAt = new Date();
        this.createdAt = getDateFromGithubFormat(json.getAsJsonPrimitive("created_at").getAsString());
        this.updatedAt = getDateFromGithubFormat(json.getAsJsonPrimitive("updated_at").getAsString());
        this.clonedAt = null;
    }
}
