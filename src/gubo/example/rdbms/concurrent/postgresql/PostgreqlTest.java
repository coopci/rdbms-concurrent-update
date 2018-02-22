package gubo.example.rdbms.concurrent.postgresql;

import java.sql.SQLException;
import java.util.Date;

public class PostgreqlTest {

	public static void main(String args[]) throws SQLException {
		
		gubo.example.rdbms.concurrent.Main.runTest("org.postgresql.Driver",
				"jdbc:postgresql://localhost:5432/test", "postgres", "123qwe", 1000);
		

	}
}
