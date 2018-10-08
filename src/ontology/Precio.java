package proyectosma.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: Precio
* @author ontology bean generator
* @version 2017/11/12, 21:55:08
*/
public class Precio implements Concept {

   /**
* Protege name: valor
   */
   private float valor;
   public void setValor(float value) { 
    this.valor=value;
   }
   public float getValor() {
     return this.valor;
   }

   /**
* Protege name: hora
   */
   private String hora;
   public void setHora(String value) { 
    this.hora=value;
   }
   public String getHora() {
     return this.hora;
   }

}
