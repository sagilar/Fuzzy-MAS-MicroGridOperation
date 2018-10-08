/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package paquetedbs;


public class PostgreSQLDAO extends DAO{
    private String port;
    private String ip;
    private String dbName;
    
    public PostgreSQLDAO(String nombreDB,String puerto)
    {
        dbName=nombreDB;
        port=puerto;
        ip="localhost";
    }
    
    public PostgreSQLDAO(String nombreDB,String puerto, String cadenaIp)
    {
        dbName=nombreDB;
        port=puerto;
        ip=cadenaIp;
    }
    
    @Override
    public String getUrl() {
        String cadenaUrl="jdbc:postgresql://" + ip + ":" + port + "/" + dbName;
        return cadenaUrl;
    }
    
}
