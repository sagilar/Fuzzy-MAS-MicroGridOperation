package proyectosma.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: enviarConexion
* @author ontology bean generator
* @version 2017/11/12, 21:55:08
*/
public class EnviarConexion implements Predicate {

   /**
* Protege name: conexionDB
   */
   private Conexion conexionDB;
   public void setConexionDB(Conexion value) { 
    this.conexionDB=value;
   }
   public Conexion getConexionDB() {
     return this.conexionDB;
   }

}
