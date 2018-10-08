package proyectosma.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: enviarInfoOperacion
* @author ontology bean generator
* @version 2017/11/12, 21:55:08
*/
public class EnviarInfoOperacion implements Predicate {

   /**
* Protege name: parametros
   */
   private Operacion parametros;
   public void setParametros(Operacion value) { 
    this.parametros=value;
   }
   public Operacion getParametros() {
     return this.parametros;
   }

}
