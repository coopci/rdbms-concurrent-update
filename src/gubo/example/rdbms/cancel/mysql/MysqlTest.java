package gubo.example.rdbms.cancel.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class MysqlTest {

	volatile static Object waitForStmt = new Object();
	volatile static PreparedStatement theStmt;

	public static void runTest( javax.sql.DataSource ds) throws SQLException, InterruptedException {
		waitForStmt = new Object();
		theStmt = null;
		
		
		try (Connection con = ds.getConnection()) {
			runTest( con );	
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public static void runTest(Connection con ) throws SQLException, InterruptedException {
		try {

			Thread execThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
						con.setAutoCommit(false);
						PreparedStatement stmt = con.prepareStatement("SELECT SLEEP(20);");

						theStmt = stmt;

						synchronized (waitForStmt) {
							System.out.println("waitForStmt.notifyAll()");
							waitForStmt.notifyAll();
						}

						ResultSet rs = stmt.executeQuery();

						rs.next();
						rs.close();

						con.commit();
					} catch (com.mysql.jdbc.exceptions.MySQLStatementCancelledException e) {
						System.err.println("Yeah! Query is cancelled! ");

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			execThread.start();

			Thread cancelThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						while (theStmt == null) {
							synchronized (waitForStmt) {
								System.out.println("waitForStmt.waitForStmt()");
								waitForStmt.wait();
							}
						}
						Thread.sleep(100);
						theStmt.cancel();

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			cancelThread.start();

			execThread.join();
			cancelThread.join();
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
