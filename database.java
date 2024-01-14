package com.mycompany.echohttpserver;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;
import java.sql.SQLException;

import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author ntu-user
 */
public class database {
    
    public database(){}
    public static boolean verification=false;
    static private String dataBaseName="Users";
    static private String dataBaseTableName="User";
    static private String userName="root";
    static private String userPassword="java123";
    static Connection conn = null;
    static PreparedStatement prepareStat = null;
    static String returnUserType = "";

    /**
    * @brief  main method that populates the dataset, retrieves values from the dataset 
    * and attempts to authenticate
    * @param[in] argv - input arguments
    */
    public static void main(String[] argv) {

        try {
            makeJDBCConnection();
            
            log("\n---------- Register an account----------");
            register();


            log("\n---------- Login to the Database ----------");
            getDataFromDB();
            

            prepareStat.close();
            conn.close(); // connection close

        } catch (SQLException e) {

            e.printStackTrace();
        }
    }
    
    /**
    * @brief encode password method
    * @param[in] plain password of type String
    * @return encoded password of type String
    */
    static String encodePassword(String plainPassword){
        byte[] bPass = plainPassword.getBytes(StandardCharsets.UTF_8);
        byte[] passBase64 = Base64.getEncoder().encode(bPass);
        String encodedPassword = new String(passBase64, StandardCharsets.UTF_8);
        return encodedPassword;
    }
    
    /**
    * @brief  decode password method
    * @param[in] encoded password of type String
    * @return decoded password of type String
    */
    private static String decodePassword(String encodedPassword){
        String decodedString = new String(Base64.getDecoder().decode(encodedPassword));
        return decodedString;
    }
    
    static void closeJDBCConnection()
        {
        try {
            conn.close();
        } catch (SQLException ex) {
            Logger.getLogger(database.class.getName()).log(Level.SEVERE, null, ex);
        }
        }
    
    /**
    * @brief connect to the database method
    */
    static void makeJDBCConnection() {

        try {
            Class.forName("com.mysql.jdbc.Driver");
            log("Congrats - Seems your MySQL JDBC Driver Registered!");
        } catch (ClassNotFoundException e) {
            log("Sorry, couldn't find JDBC driver. Make sure you have added JDBC Maven Dependency Correctly");
            e.printStackTrace();
            return;
        }

        try {
            // DriverManager: The basic service for managing a set of JDBC drivers.
            conn = DriverManager.getConnection("jdbc:mysql://34.89.109.218:3306/"+dataBaseName+"?useSSL=false", userName, userPassword);
            if (conn != null) {
                log("Connection Successful! Enjoy. Now it's time to push data");
            } else {
                log("Failed to make connection!");
            }
        } catch (SQLException e) {
            log("MySQL Connection Failed!");
            e.printStackTrace();
            return;
        }

    }
    
    static void changeUserPassword(String user, String password)
        {
            String encodedPassword = encodePassword(password);
                try {
                    String modifyQueryStatement = "UPDATE " + dataBaseTableName + " SET Password = '" + encodedPassword + "' WHERE Username = '" + user + "'";

                    //System.out.println(modifyQueryStatement);
                    prepareStat = conn.prepareStatement(modifyQueryStatement);
                    
                    // execute insert SQL statement
                    prepareStat.executeUpdate();
                    log(user + " password changed successfully");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
        }
    
    static void changeUserType(String user, String userType)
        {
            
                try {
                    String modifyQueryStatement = "UPDATE " + dataBaseTableName + " SET UserType = '" + userType + "' WHERE Username = '" + user + "'";

                    prepareStat = conn.prepareStatement(modifyQueryStatement);
                    
                    // execute insert SQL statement
                    prepareStat.executeUpdate();
                    log(user + " user type changed successfully");
                } catch (SQLException e) {
                    e.printStackTrace();
                }
        }
    
    /**
    * @brief add data to the database method
    * @param[in] user name of type String
    * @param[in] user password of type String
    */
    static void addDataToDB(String user, String password, String userType) {
        try {
            String insertQueryStatement = "INSERT  IGNORE INTO "+dataBaseTableName+" VALUES  (?,?,?)";

            prepareStat = conn.prepareStatement(insertQueryStatement);
            prepareStat.setString(1, user);
            prepareStat.setString(2, encodePassword(password));
            prepareStat.setString(3, userType);

            // execute insert SQL statement
            prepareStat.executeUpdate();
            log(user + " added successfully");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    static void removeDataFromDB(String user)
        {
        try {
            String deleteQuery = "DELETE FROM " + dataBaseTableName + " WHERE Username = '" + user + "'";
            System.out.println(deleteQuery);
            prepareStat = conn.prepareStatement(deleteQuery);
            
            prepareStat.executeUpdate();
            log(user + " removed");
        } catch (SQLException ex) {
            Logger.getLogger(database.class.getName()).log(Level.SEVERE, null, ex);
        }
        }
    
    /**
    * @brief get data from the Database method
    */
    
    private static void getDataFromDB() {

        try {
            Scanner loginuser = new Scanner(System.in);
            
            System.out.println("Username: ");
             String dbusername = loginuser.nextLine();
            
            System.out.println("Password: ");
            String dbPassword = loginuser.nextLine();
            String encodedbPassword = encodePassword(dbPassword);
            loginuser.close();
            // MySQL Select Query Tutorial
            String getQueryStatement = "SELECT * FROM "+dataBaseTableName+ " WHERE Username = '"+dbusername+"' AND Password = '"+encodedbPassword+"';";
            prepareStat = conn.prepareStatement(getQueryStatement);

            // Execute the Query, and get a java ResultSet
            ResultSet rs = prepareStat.executeQuery();

            // Let's iterate through the java ResultSet
            while (rs.next()) {

                String user = rs.getString("Username");
                String pass = decodePassword(rs.getString("Password"));
                if (dbusername.equals(user)&& dbPassword.equals(pass)){
                   System.out.println("Account found");
                }
                else {
                    System.out.println("Account not found");
                }
   

                // Simply Print the results
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
    private static void register() {
        try{
             Scanner newaccount = new Scanner(System.in);
            System.out.print("Welcome to create your Account \n");
            System.out.print("Enter a username:  \n");
            
            String newusername = newaccount.next();
            System.out.print("Now enter a password: \n");
            String newpassword = newaccount.next();
            System.out.print(newusername + newpassword);
            addDataToDB(newusername, encodePassword(newpassword), "Standard");
        } catch (Exception e){
            e.printStackTrace();
        }
        
    }
    /**
    * @brief decode password method
    * @param[in] user name as type String
    * @param[in] plain password of type String
    * @return true if the credentials are valid, otherwise false
    */
    public static boolean validateUser(String user, String pass){
        try {
            // MySQL Select Query Tutorial
            String getQueryStatement = "SELECT Username, Password, UserType FROM "+dataBaseTableName;

            prepareStat = conn.prepareStatement(getQueryStatement);

            // Execute the Query, and get a java ResultSet
            ResultSet rs = prepareStat.executeQuery();

            // Let's iterate through the java ResultSet
            while (rs.next()) {
                if (user.equals(rs.getString("Username")) && pass.equals(decodePassword(rs.getString("Password")))){
                    verification = true;
                    returnUserType = rs.getString("UserType");
                    return true;
                }
                else
                    verification = false;
            }
        } 
        
        catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }

    /**
    * @brief print a message on screen method
    * @param message of type String
    */
    private static void log(String message) {
        System.out.println(message);

    }
}