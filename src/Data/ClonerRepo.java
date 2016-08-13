package Data;

import java.util.Date;

/**
 * Created by RobbinNi on 7/8/16.
 */
public class ClonerRepo {
    public int id, ownerid;
    public String url;
    public Date clonedAt;

    public ClonerRepo(int id, int ownerid, String url) {
        this.id = id;
        this.ownerid = ownerid;
        this.url = url;
        this.clonedAt = null;
    }

    public ClonerRepo(GithubRepo repo) {
        this.id = repo.id;
        this.ownerid = repo.ownerid;
        this.url = repo.url;
        this.clonedAt = null;
    }
}
