package gubo.example.rdbms.concurrent.mysql;

import java.sql.SQLException;

public class MysqlTest {

    public static void main(String args[]) throws SQLException {
        gubo.example.rdbms.concurrent.Main.runTest("com.mysql.jdbc.Driver",
                "jdbc:mysql://localhost:3306/test", "root", "123qwe", 1000);
    }
}
