/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package paquetedbs;

import java.io.File;
import java.io.IOException;

public class SQLiteDAO extends DAO{
    private String dbName;
    private String dbPath;
    
    public SQLiteDAO(String nombreDB)
    {
        dbName=nombreDB;
        try {
            this.dbPath = new File(".").getCanonicalPath();
        } catch (IOException ex) {
            //Logger.getLogger(GUICamionera.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public SQLiteDAO(String nombreDB,String rutaDB)
    {
        dbName=nombreDB;
        dbPath=rutaDB;
        
    }
    
    @Override
    public String getUrl() {
        String cadenaUrl="";
       if(!dbPath.equals(""))
       {
           cadenaUrl="jdbc:sqlite:" + dbPath + "/" + dbName + ".db";
       }else
       {
           cadenaUrl="jdbc:sqlite:" + dbName + ".db";
       }
     return cadenaUrl;   
    }
    
}
