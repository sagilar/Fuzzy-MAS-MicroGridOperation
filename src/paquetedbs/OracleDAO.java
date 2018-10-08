/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package paquetedbs;


public class OracleDAO extends DAO {
    private String port;
    private String ip;
    private String sid;
    
    
    public OracleDAO(String sidService, String puerto)
    {
        sid=sidService;
        port=puerto;
        ip="localhost";
    }
    
    public OracleDAO(String sidService,String puerto,String cadenaIp)
    {
        sid=sidService;
        port=puerto;
        ip=cadenaIp;
    }

    @Override
    public String getUrl() {
        String cadenaUrl="jdbc:oracle:thin:@" + ip + ":" + port + ":" + sid;
        return cadenaUrl;
    }
    
    
    
}
