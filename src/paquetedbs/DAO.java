/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package paquetedbs;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.mysql.jdbc.*;
import oracle.jdbc.driver.*;
import com.microsoft.sqlserver.jdbc.*;
import org.postgresql.*;
import org.sqlite.*;


public abstract class DAO {
    
    public Connection conectar(String url,String user, String pass)
    {
        Connection con=null;
       
        try {
            con = DriverManager.getConnection(url, user, pass);
        } catch (SQLException ex) {
            Logger.getLogger(DAO.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return con;
    }
    
    abstract public String getUrl();
    
}
