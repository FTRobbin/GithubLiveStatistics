package Data;

import org.apache.commons.lang3.time.FastDateFormat;

import java.text.ParseException;
import java.util.Date;

/**
 * Created by RobbinNi on 7/7/16.
 */
public final class DataUtility {

    private static final FastDateFormat format = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public static Date getDateFromGithubFormat(String s) throws ParseException {
        return format.parse(s);
    }
}
