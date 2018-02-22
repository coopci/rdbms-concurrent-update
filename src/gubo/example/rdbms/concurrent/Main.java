package gubo.example.rdbms.concurrent;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class Main {

    public static String createTable = "CREATE TABLE `t_counter` (\n" +
            "  `id` bigint(11) NOT NULL,\n" +
            "  `name` varchar(45) NOT NULL,\n" +
            "  `counter` bigint(11) DEFAULT NULL,\n" +
            "  PRIMARY KEY (`id`),\n" +
            "  UNIQUE KEY `idx_t_counter_name` (`name`)\n" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8";

    // insert into t_counter(`name`, counter) values ('name1', 1);


    public static void runTest(String drivername, String connUrl, String username, String password) throws SQLException {

        try {
            Class.forName(drivername);
            clear(connUrl, username, password);
            Thread t1 = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        doUpdate("thread1", 10000, connUrl, username, password);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });


            Thread t2 = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        doUpdate("thread2", 10000, connUrl, username, password);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            });



            t1.start();
            t2.start();


            t2.join();
            t1.join();
        } catch (Exception e) {
            System.out.println(e);
        }
        report(connUrl, username, password);

    }

    static CyclicBarrier barrier = new CyclicBarrier(2);


    public static void main(String args[]) throws SQLException {
    }


    static public void report(String connUrl, String username, String password) throws SQLException {

        Connection con = DriverManager.getConnection(
                connUrl, username, password);
        con.setAutoCommit(true);
        try {

            PreparedStatement stmt = con.prepareStatement("select * from t_counter ");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {

                String name = rs.getString("name");
                long counter = rs.getLong("counter");
                System.out.println(name + ": " + counter);

            }
        } finally {
            con.close();
        }
    }

    static public void clear(String connUrl, String username, String password) throws SQLException {

        Connection con = DriverManager.getConnection(
                connUrl, username, password);
        con.setAutoCommit(false);
        try {

            PreparedStatement stmt = con.prepareStatement("update t_counter set counter = 0 where `name`='name1'");
            stmt.execute();
            con.commit();

        } finally {
            con.close();
        }
    }



    static public void doUpdate(String threadname, int times, String connUrl, String username, String password) throws SQLException, InterruptedException,
            BrokenBarrierException {

        Connection con = DriverManager.getConnection(
                connUrl, username, password);
        //con.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        //con.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        // con.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
        con.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);

        con.setAutoCommit(false);
        try {
            PreparedStatement stmt = con.prepareStatement("select * from t_counter where `name`='name1'");

            PreparedStatement stmt2 = con.prepareStatement("update t_counter set counter = counter+1 where `name`='name1'");
            for (int i = 0; i < times; ++i) {

                stmt2.execute();
                ResultSet rs = stmt.executeQuery();
                rs.next();
                long counter = rs.getLong("counter");
                rs.close();
                // barrier.await();
                System.out.println(threadname + ", counter: " + counter);

                con.commit();

            }
        } finally {
            con.close();
        }
    }


}

