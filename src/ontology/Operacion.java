package proyectosma.ontology;


import jade.content.*;
import jade.util.leap.*;
import jade.core.*;

/**
* Protege name: Operacion
* @author ontology bean generator
* @version 2017/11/12, 21:55:08
*/
public class Operacion implements Concept {

   /**
* Protege name: potenciaDemanda
   */
   private float potenciaDemanda;
   public void setPotenciaDemanda(float value) { 
    this.potenciaDemanda=value;
   }
   public float getPotenciaDemanda() {
     return this.potenciaDemanda;
   }

   /**
* Protege name: modoOperacionParcial
   */
   private int modoOperacionParcial;
   public void setModoOperacionParcial(int value) { 
    this.modoOperacionParcial=value;
   }
   public int getModoOperacionParcial() {
     return this.modoOperacionParcial;
   }

   /**
* Protege name: modoOperacionInterno
   */
   private int modoOperacionInterno;
   public void setModoOperacionInterno(int value) { 
    this.modoOperacionInterno=value;
   }
   public int getModoOperacionInterno() {
     return this.modoOperacionInterno;
   }

   /**
* Protege name: estadoFallaRedExterna
   */
   private boolean estadoFallaRedExterna;
   public void setEstadoFallaRedExterna(boolean value) { 
    this.estadoFallaRedExterna=value;
   }
   public boolean getEstadoFallaRedExterna() {
     return this.estadoFallaRedExterna;
   }

   /**
* Protege name: potenciaGenerada
   */
   private float potenciaGenerada;
   public void setPotenciaGenerada(float value) { 
    this.potenciaGenerada=value;
   }
   public float getPotenciaGenerada() {
     return this.potenciaGenerada;
   }

   /**
* Protege name: aceptado
   */
   private boolean aceptado;
   public void setAceptado(boolean value) { 
    this.aceptado=value;
   }
   public boolean getAceptado() {
     return this.aceptado;
   }

   /**
* Protege name: modoOperacionApagado
   */
   private int modoOperacionApagado;
   public void setModoOperacionApagado(int value) { 
    this.modoOperacionApagado=value;
   }
   public int getModoOperacionApagado() {
     return this.modoOperacionApagado;
   }

   /**
* Protege name: modoOperacionVenta
   */
   private int modoOperacionVenta;
   public void setModoOperacionVenta(int value) { 
    this.modoOperacionVenta=value;
   }
   public int getModoOperacionVenta() {
     return this.modoOperacionVenta;
   }

   /**
* Protege name: estadoFallaMicroRed
   */
   private boolean estadoFallaMicroRed;
   public void setEstadoFallaMicroRed(boolean value) { 
    this.estadoFallaMicroRed=value;
   }
   public boolean getEstadoFallaMicroRed() {
     return this.estadoFallaMicroRed;
   }

   /**
* Protege name: potenciaAlmacenada
   */
   private float potenciaAlmacenada;
   public void setPotenciaAlmacenada(float value) { 
    this.potenciaAlmacenada=value;
   }
   public float getPotenciaAlmacenada() {
     return this.potenciaAlmacenada;
   }

   /**
* Protege name: modoOperacionExterno
   */
   private int modoOperacionExterno;
   public void setModoOperacionExterno(int value) { 
    this.modoOperacionExterno=value;
   }
   public int getModoOperacionExterno() {
     return this.modoOperacionExterno;
   }

   /**
* Protege name: modoOperacionAlmacenamiento
   */
   private int modoOperacionAlmacenamiento;
   public void setModoOperacionAlmacenamiento(int value) { 
    this.modoOperacionAlmacenamiento=value;
   }
   public int getModoOperacionAlmacenamiento() {
     return this.modoOperacionAlmacenamiento;
   }

}
