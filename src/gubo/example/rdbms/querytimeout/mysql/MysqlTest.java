package gubo.example.rdbms.querytimeout.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * 
 * Connector/J 5.0.0 and later include support for both Statement.cancel() and
 * Statement.setQueryTimeout(). Both require MySQL 5.0.0 or newer server, and
 * require a separate connection to issue the KILL QUERY statement. In the case
 * of setQueryTimeout(), the implementation creates an additional thread to
 * handle the timeout functionality.
 * 
 * 
 * Note
 * 
 * The MySQL statement KILL QUERY (which is what the driver uses to implement
 * Statement.cancel()) is non-deterministic; thus, avoid the use of
 * Statement.cancel() if possible. If no query is in process, the next query
 * issued will be killed by the server. This race condition is guarded against
 * as of Connector/J 5.1.18.
 * 
 **/
public class MysqlTest {

	volatile static Object waitForStmt = new Object();
	volatile static PreparedStatement theStmt;

	public static void runTest(javax.sql.DataSource ds) throws SQLException, InterruptedException {
		waitForStmt = new Object();
		theStmt = null;

		try (Connection con = ds.getConnection()) {
			runTest(con);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void runTest(Connection con) throws SQLException, InterruptedException {
		try {

			Thread execThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
						con.setAutoCommit(false);
						PreparedStatement stmt = con.prepareStatement("SELECT SLEEP(20);");
						stmt.setQueryTimeout(1);
						theStmt = stmt;

						

						ResultSet rs = stmt.executeQuery();
						
						rs.next();
						rs.close();

						con.commit();
					} catch (com.mysql.jdbc.exceptions.MySQLTimeoutException e) {
						System.err.println("Yeah! Query is timeout! ");

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			execThread.start();


			execThread.join();
		} finally {

		}

	}

	static final String url = "jdbc:mysql://localhost:3306/test";
	static final String username = "root";
	static final String password = "123qwe";

	public static HikariDataSource makeNewHikariDataSource() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(url);
		config.setUsername(username);
		config.setPassword(password);
		HikariDataSource ds = new HikariDataSource(config);
		return ds;
	}

	public static MysqlDataSource makeNewMysqlDataSource() {
		com.mysql.jdbc.jdbc2.optional.MysqlDataSource ds = new com.mysql.jdbc.jdbc2.optional.MysqlDataSource();
		ds.setUrl(url);
		ds.setUser(username);
		ds.setPassword(password);
		return ds;
	}

	public static void main(String args[]) throws SQLException, ClassNotFoundException, InterruptedException {

		MysqlDataSource MysqlDS = makeNewMysqlDataSource();

		System.err.println("Run on MysqlDS");
		runTest(MysqlDS);

		System.err.println("Run on HikariDS");
		HikariDataSource HikariDS = makeNewHikariDataSource();
		runTest(HikariDS);
		
	}
}
