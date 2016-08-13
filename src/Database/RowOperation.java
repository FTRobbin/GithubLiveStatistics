package Database;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by RobbinNi on 7/8/16.
 */
public interface RowOperation {

    void operate(ResultSet row) throws SQLException;
}
