package org.tensorflow.lite.examples.detection;

import android.annotation.SuppressLint;
import android.os.StrictMode;
import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

class ConnectionClass {
    String ip = "10.10.15.222";
   // String class = "net.sourceforge.jtds.jdbc.Driver";
    String db = "deneme";
    String port ="1433";
    String username="login";
    String password="625";

    @SuppressLint("NewApi")
    public Connection CONN() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Connection conn = null;
        String ConnURL = null;
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");

          //  ConnURL = "jdbc:jtds:sqlserver://"+ip+":"+port+";"+"databasename="+db+";user"+username+";"+"password="+password+";";
            ConnURL= "jdbc:jtds:sqlserver://10.10.15.222/deneme;instance=SQLEXPRESS;user=login;password=625";
            //     ConnURL = "jdbc:jtds:sqlserver://" + ip + "/" + db;
            conn = DriverManager.getConnection(ConnURL);

//            Class.forName("net.sourceforge.jtds.jdbc.Driver");
//            ConnURL = "jdbc:jtds:sqlserver://"+ip +":"+ port + ";" + "databasename"+db + "; user="+username+";"+"password="+password+";";

       //     conn = DriverManager.getConnection(ConnURL);
        } catch (SQLException se) {
            Log.e("ERROR", se.getMessage());
        } catch (ClassNotFoundException e) {
            Log.e("ERROR", e.getMessage());
        } catch (Exception e) {
            Log.e("ERROR", e.getMessage());
        }
        return conn;
    }
}
//public class ConnectionHelper {
//    Connection con;
//    String uname, pass,ip,port,database;
//
//    public Connection connectionclass(){
//        ip = "10.10.15.222";
//        database= "proje";
//        port = "1433";
//
//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);
//        Connection connection = null;
//        String ConnectionURL = null;
//        try{
//            Class.forName("net.sourceforge.jtds.jdbc.Driver");
//            ConnectionURL = "jdbc:jtds:sqlserver://" + ip +":"+ port + ";" + "databasename"+database;
//            connection = DriverManager.getConnection(ConnectionURL);
//        }catch(Exception ex){
//            Log.e("Error",ex.getMessage());
//
//        }
//
//
//        return connection;
//
//    }


