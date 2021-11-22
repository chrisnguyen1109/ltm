/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controller;

import java.sql.*;
import java.util.ArrayList;
import java.util.logging.*;
import model.User;

/**
 *
 * @author HP
 */
public class UserDatabase {
    private Connection conn;
    public final String DATABASE_NAME = "ltm_final_project";
    public final String USERNAME = "root";
    public final String PASSWORD = "123456";
    public final String URL_MYSQL = "jdbc:mysql://localhost:33067/"+DATABASE_NAME;
    
    public final String USER_TABLE = "user";
    
    private PreparedStatement pst;
    private ResultSet rs;
    private Statement st;
    
    public Connection connect() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");    
            conn = DriverManager.getConnection(URL_MYSQL, USERNAME, PASSWORD);
            System.out.println("Connect database successfully!");
        } catch (SQLException ex) {
            Logger.getLogger(UserDatabase.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Error connection!");
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(UserDatabase.class.getName()).log(Level.SEVERE, null, ex);
        }
        return conn;
    }

    public int insertUser(User u) {
        try {
            pst = conn.prepareStatement("INSERT INTO "+USER_TABLE+" (username, password, score, total_win, total_match) VALUES ('"+u.getUsername()+"', '"+u.getPassword()+"', '"+u.getScore()+"', '"+u.getTotalWin()+"', '"+u.getTotalMatch()+"')");
            int result = pst.executeUpdate();
            if(result > 0) {
                System.out.println("Insert successfully!");
            }
            return result;
        } catch (SQLException ex) {
            Logger.getLogger(UserDatabase.class.getName()).log(Level.SEVERE, null, ex);
        }
        return -1;
    }
    
    public boolean checkUsername(String username, String password) {
        try {
            pst = conn.prepareStatement("SELECT * FROM "+USER_TABLE+" WHERE username = '" + username + "' AND password = '" + password +"'");
            rs = pst.executeQuery();
            
            if(rs.next()) {
                return true;
            }
        } catch (SQLException ex) {
            Logger.getLogger(UserDatabase.class.getName()).log(Level.SEVERE, null, ex);
            
        }
        return false;
    }
    
    public User getInfoUser(String username) {
        try {
            pst = conn.prepareStatement("SELECT * FROM "+USER_TABLE+" WHERE username = '" + username + "'");
            rs = pst.executeQuery();
            if(rs.next()) {
                return new User(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getInt(5));
            }
            return null;
        } catch (SQLException ex) {
            Logger.getLogger(UserDatabase.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public boolean updatePoint(String username, int point) {
        try {
            int win = point == 1 ? 1 : 0;
            pst = conn.prepareStatement("UPDATE "+USER_TABLE+" SET score = score + "+point+", total_match = total_match + 1, total_win = total_win + "+win+" WHERE username = '"+username+"'");
            int result = pst.executeUpdate();
            if(result > 0) {
                return true;
            }
            return false;
        } catch (SQLException ex) {
            Logger.getLogger(UserDatabase.class.getName()).log(Level.SEVERE, null, ex);
            
        }
        return false;        
    }
    
    public ArrayList<User> getPlayerRank() {
        ArrayList<User> res = new ArrayList<User>();
        try {
            pst = conn.prepareStatement("SELECT * FROM user ORDER BY score DESC, total_match ASC LIMIT 10");
            rs = pst.executeQuery();
            while(rs.next()) {
                res.add(new User(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getInt(4), rs.getInt(5)));
            }
            
        } catch (SQLException ex) {
            Logger.getLogger(UserDatabase.class.getName()).log(Level.SEVERE, null, ex);
            
        }
        
        return res;
    }
}
