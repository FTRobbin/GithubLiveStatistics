package Database;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by RobbinNi on 7/8/16.
 */
public class MarkIssued implements RowOperation {

    public static MarkIssued oper = null;

    private MarkIssued() {}

    @Override
    public void operate(ResultSet row) throws SQLException {
        row.updateBoolean("issued", true);
    }

    public static MarkIssued getOper() {
        if (oper == null) {
            oper = new MarkIssued();
        }
        return oper;
    }
}
