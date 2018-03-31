package gubo.example.rdbms.cancel.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MysqlTest {

	volatile static Object waitForStmt = new Object();
	volatile static PreparedStatement theStmt;

	public static void runTest(String drivername, String connUrl, String username, String password, final int updates)
			throws SQLException, ClassNotFoundException, InterruptedException {

		try {
			Class.forName(drivername);

			Thread execThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {

						Connection con = DriverManager.getConnection(connUrl, username, password);
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
						System.out.println("Yeah! Query is cancelled! ");

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

	public static void main(String args[]) throws SQLException, ClassNotFoundException, InterruptedException {
		runTest("com.mysql.jdbc.Driver", "jdbc:mysql://localhost:3306/test", "root", "123qwe", 1000);
	}
}
