package proyectosma.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: enviarPrecio
* @author ontology bean generator
* @version 2017/11/12, 21:55:08
*/
public class EnviarPrecio implements Predicate {

   /**
* Protege name: infoPrecio
   */
   private Precio infoPrecio;
   public void setInfoPrecio(Precio value) { 
    this.infoPrecio=value;
   }
   public Precio getInfoPrecio() {
     return this.infoPrecio;
   }

}
