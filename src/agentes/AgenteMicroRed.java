/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agentes;

/**
 *
 * @author Santiago
 */
import com.mathworks.engine.EngineException;
import com.mathworks.engine.MatlabEngine;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONObject;
import proyectosma.ontology.*;

public class AgenteMicroRed extends Agent {
    
    double potenciaGenerada = 0;
    double demandaPotencia = 0;
    double potenciaAlmacenada = 0;
    double capacidadBateria = 330;
    double potenciaGeneradaMax = 660;
    int modoOperacion = 0;
    int modoOperacionApagado = 0;
    int modoOperacionInterno = 0;
    int modoOperacionExterno = 0;
    int modoOperacionParcial = 0;
    int modoOperacionAlmacenamiento = 0;
    int modoOperacionVenta = 0;
    
    
    int[] modosOperacion;
    
    
    Thread t;
    Timer tlectura = new Timer();
    MatlabEngine matlab;
    MatlabEngine eng;
    MatlabEngine motor;
    boolean flagConexion=false;
    boolean interruptorExterno = true, interruptorGeneradores = false, sincronizacion = true, interruptorBaterias = false, activacionRedVenta = false, interruptorGeneradoresABateria = false;
    double voltajeNominalMR = 120, voltajeNominalRE = 120, frecuenciaNominalMR = 60, frecuenciaNominalRE = 60;
    double voltajeMR = 120, voltajeRE = 120, frecuenciaMR = 60, frecuenciaRE = 60;
    boolean alarmaMR = false, alarmaRE = false;

    private Codec codec = new SLCodec();

    private Ontology ontologia = ProyectoSMAOntology.getInstance();

    @Override
    protected void setup() {
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontologia);
        this.addBehaviour(new ComportamientoRecibirModoOperacion());
        this.addBehaviour(new ComportamientoRevisarSistema(this, 400));
    }

    private class ComportamientoRecibirModoOperacion extends CyclicBehaviour {

        @Override
        public void action() {
            //System.out.println("Agente micro-red entrando al comportamiento recibir modo de operación");
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchLanguage(codec.getName()),
                    MessageTemplate.MatchOntology(ontologia.getName()));
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                

                if (msg.getPerformative() == ACLMessage.INFORM) {

                    try {
                        ContentElement ce = getContentManager().extractContent(msg);
                        
                        if (ce instanceof EnviarInfoOperacion) {
                            EnviarInfoOperacion enviar = (EnviarInfoOperacion) ce;
                            Operacion seleccion = enviar.getParametros();
                            modoOperacionApagado = seleccion.getModoOperacionApagado();
                            modoOperacionInterno = seleccion.getModoOperacionInterno();
                            modoOperacionExterno = seleccion.getModoOperacionExterno();
                            modoOperacionParcial = seleccion.getModoOperacionParcial();
                            modoOperacionVenta = seleccion.getModoOperacionVenta();
                            modoOperacionAlmacenamiento = seleccion.getModoOperacionAlmacenamiento();
                            int[] modosOperacion=new int[]{modoOperacionApagado,modoOperacionInterno,modoOperacionExterno,modoOperacionParcial,modoOperacionVenta,modoOperacionAlmacenamiento};
                            boolean condok = verificarCondicionesModoOp(modosOperacion);
                            if (condok) {

                                conmutarModoOperacion(modosOperacion);
                            }
                            ACLMessage respuesta = new ACLMessage();
                            AID r = new AID();
                            respuesta.setSender(getAID());
                            respuesta.addReceiver(msg.getSender());
                            respuesta.setLanguage(codec.getName());
                            respuesta.setOntology(ontologia.getName());
                            if(condok)
                            {
                                respuesta.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                            }else
                            {
                                respuesta.setPerformative(ACLMessage.REJECT_PROPOSAL);
                            }
                            Operacion operacion = new Operacion();
                            operacion.setAceptado(condok);
                            EnviarInfoOperacion enviarModoOperacion = new EnviarInfoOperacion();
                            enviarModoOperacion.setParametros(operacion);
                            getContentManager().fillContent(respuesta, (ContentElement) enviarModoOperacion);
                            //respuesta.setContentObject(enviarModoOperacion);
                            send(respuesta);

                        }
                    } catch (Codec.CodecException ex) {
                        Logger.getLogger(AgenteMicroRed.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (OntologyException ex) {
                        Logger.getLogger(AgenteMicroRed.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }
            }else
            {
                block();
            }

        }
    }

    private boolean verificarCondicionesModoOp(int[] modos) {
        boolean cambioAceptado=true;
        /*System.out.println("Potencia almacenada = " + potenciaAlmacenada + ", potencia "
                                    + "generada = " + potenciaGenerada + ", potencia de demanda = " + demandaPotencia);
                            System.out.println("Falla en la micro-red : " + alarmaMR + ", falla en la red externa: " + alarmaRE);*/
        /*switch (modo) {
            case 0:
                System.out.println("Modo de operación: sistema apagado");
                return true;

            case 1:
                if (potenciaGenerada + potenciaAlmacenada - demandaPotencia > 0) {
                    System.out.println("Si es posibe entrar al modo de operación 1: Entrando modo de operación consumo interno únicamente");
                    return true;
                } else {
                    System.out.println("No es posibe entrar al modo de operación 1: Volviendo al modo de operación anterior");
                    return false;
                }

            case 2:
                if (potenciaGenerada > 0 && potenciaAlmacenada > capacidadBateria - 70) {
                    System.out.println("No es posible entrar al modo de operación 2: Se perdería energía generada, volviendo al modo anterior");
                    return false;
                } else {
                    System.out.println("Entrando al modo de operación 2. Consumo externo únicamente");
                    return true;
                }

            case 3:
                if (potenciaGenerada + potenciaAlmacenada - demandaPotencia < 0 && potenciaGenerada + potenciaAlmacenada > 100) {
                    System.out.println("Si es posibe entrar al modo de operación 3: Entrando modo de operación consumo parcial interno-externo");
                    return true;
                } else {
                    System.out.println("No es posibe entrar al modo de operación 3: Volviendo al modo de operación anterior");
                    return false;
                }

            case 4:
                if (potenciaAlmacenada < capacidadBateria) {
                    System.out.println("Si es posibe entrar al modo de operación 4: Almacenando energía");
                    return true;
                } else {
                    System.out.println("No es posibe entrar al modo de operación 4, baterías completamente cargadas: Rechazando solicitud");
                    return false;
                }
            case 5:
                if (potenciaGenerada > 100 && potenciaAlmacenada > 100) {
                    System.out.println("Si es posibe entrar al modo de operación 5: Vendiendo energía");
                    return true;
                } else {
                    System.out.println("No es posibe entrar al modo de operación 5, no hay suficiente energía para vender: Rechazando solicitud");
                    return false;
                }

            default:
                System.out.println("Modo de operación desconocido");
                return false;
        }*/
        
        if(modos[0]==1)
        {
            System.out.println("Modo de operación: sistema apagado");
            
        }
        if(modos[1]==1){
            if (potenciaGenerada + potenciaAlmacenada - demandaPotencia > 0) {
                    System.out.println("Si es posibe entrar al modo de operación 1: Entrando modo de operación consumo interno únicamente");
                    
                } else {
                    System.out.println("No es posibe entrar al modo de operación 1: Volviendo al modo de operación anterior");
                    cambioAceptado=false;
                }
        }
        if(modos[2]==1)
        {
            if (potenciaGenerada > 0 && potenciaAlmacenada > capacidadBateria - 70) {
                    System.out.println("No es posible entrar al modo de operación 2: Se perdería energía generada, volviendo al modo anterior");
                    cambioAceptado= false;
                } else {
                    System.out.println("Entrando al modo de operación 2. Consumo externo únicamente");
                    
                }
        }
        
        if(modos[3]==1)
        {
            if (potenciaGenerada - demandaPotencia < 0 && potenciaGenerada + potenciaAlmacenada > 100) {
                    System.out.println("Si es posibe entrar al modo de operación 3: Entrando modo de operación consumo parcial interno-externo");
                    
                } else {
                    System.out.println("No es posibe entrar al modo de operación 3: Volviendo al modo de operación anterior");
                    cambioAceptado= false;
                }
        }
        
        if(modos[4]==1)
        {
            if (potenciaGenerada > 100 && potenciaAlmacenada > 100) {
                    System.out.println("Si es posibe entrar al modo de operación 4: Vendiendo energía");
                    
                } else {
                    System.out.println("No es posibe entrar al modo de operación 4, no hay suficiente energía para vender: Rechazando solicitud");
                    cambioAceptado=false;
                }
        }
        if(modos[5]==1)
        {
            if (potenciaAlmacenada < capacidadBateria) {
                    System.out.println("Si es posibe entrar al modo de operación 5: Almacenando energía");
                    
                } else {
                    System.out.println("No es posibe entrar al modo de operación 5, baterías completamente cargadas: Rechazando solicitud");
                    cambioAceptado = false;
                }
        }

        return cambioAceptado;
    }

    private void conmutarModoOperacion(int[] modos) {
        /*switch (modo) {
            case 0:
                System.out.println("Conmutando a modo de operación: apagado");
                break;
            case 1:
                System.out.println("Conmutando a modo de operación: consumo interno únicamente");
                break;
            case 2:
                System.out.println("Conmutando a modo de operación: consumo externo únicamente");
                break;
            case 3:
                System.out.println("Conmutando a modo de operación: Consumo interno parcial, consumo externo parcial");
                break;
            case 4:
                System.out.println("Conmutando a modo de operación: Almacenamiento de energía");
                break;
            case 5:
                System.out.println("Conmutando a modo de operación: Venta de energía");
                break;
            default:
                System.out.println("Solicitud errónea");
                break;
        }*/
        
        if(modos[0]==1)
        {
            System.out.println("Conmutando a modo de operación: apagado");
            
        }
        if(modos[1]==1){
            System.out.println("Conmutando a modo de operación: consumo interno únicamente");
        }
        if(modos[2]==1)
        {
           System.out.println("Conmutando a modo de operación: consumo externo únicamente");
        }
        
        if(modos[3]==1)
        {
           System.out.println("Conmutando a modo de operación: Consumo interno parcial, consumo externo parcial");
        }
        if(modos[4]==1)
        {
            System.out.println("Conmutando a modo de operación: Venta de energía");
        }
        if(modos[5]==1)
        {
            System.out.println("Conmutando a modo de operación: Almacenamiento de energía");
        }
    }

    private class ComportamientoRevisarSistema extends TickerBehaviour {

        public ComportamientoRevisarSistema(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            //System.out.println("Agente micro-red entrando al comportamiento revisar sistema");
            leerFromMatlab();
            if ((voltajeMR > 120 * 1.1 && voltajeMR < 120 * 0.9) || (frecuenciaMR > 60 * 1.1 && frecuenciaMR < 60 * 0.9)) {
                alarmaMR = true;
                System.out.println("Alarma de condiciones no deseadas de microred encendida");
            } else {
                alarmaMR = false;
            }

            if ((voltajeRE > 120 * 1.1 && voltajeRE < 120 * 0.9) || (frecuenciaRE > 60 * 1.1 && frecuenciaRE < 60 * 0.9)) {
                alarmaRE = true;
                System.out.println("Alarma de condiciones no deseadas de red externa encendida");
            } else {
                alarmaRE = false;
            }

            try {
                ACLMessage mensaje = new ACLMessage();
                AID r1 = new AID();
                r1.setLocalName("EnergyMarketAgent");
                AID r2 = new AID();
                r2.setLocalName("InterfaceAgent");
                mensaje.setSender(getAID());
                //mensaje.addReceiver(r);
                mensaje.addReceiver(r1);
                mensaje.addReceiver(r2);
                //mensaje.addReceiver(getDefaultDF());
                mensaje.setLanguage(codec.getName());
                mensaje.setOntology(ontologia.getName());
                mensaje.setPerformative(ACLMessage.INFORM);
                Operacion operacion = new Operacion();
                operacion.setEstadoFallaMicroRed(alarmaMR);
                operacion.setEstadoFallaRedExterna(alarmaRE);
                operacion.setPotenciaAlmacenada((float) potenciaAlmacenada);
                operacion.setPotenciaGenerada((float) potenciaGenerada);
                operacion.setPotenciaDemanda((float) demandaPotencia);
                operacion.setModoOperacionApagado(modoOperacionApagado);
                operacion.setModoOperacionInterno(modoOperacionInterno);
                operacion.setModoOperacionExterno(modoOperacionExterno);
                operacion.setModoOperacionParcial(modoOperacionParcial);
                operacion.setModoOperacionVenta(modoOperacionVenta);
                operacion.setModoOperacionAlmacenamiento(modoOperacionAlmacenamiento);

                EnviarInfoOperacion enviarModoOperacion = new EnviarInfoOperacion();
                enviarModoOperacion.setParametros(operacion);
                //mensaje.setContentObject(enviarModoOperacion);
                getContentManager().fillContent(mensaje, (ContentElement) enviarModoOperacion);
                

                send(mensaje);
            } catch (Codec.CodecException ex) {
                Logger.getLogger(AgenteMicroRed.class.getName()).log(Level.SEVERE, null, ex);
            } catch (OntologyException ex) {
                Logger.getLogger(AgenteMicroRed.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }
    
    private void leerFromMatlab() {
        //System.out.println("Agente micro-red entrando a la función de integración con matlab");
        if(!flagConexion)
        {
            try {
            Future<String[]> eFuture = MatlabEngine.findMatlabAsync();
            String[] engines = eFuture.get();
            Future<MatlabEngine> engFuture = MatlabEngine.connectMatlabAsync(engines[0]);
            motor = engFuture.get();
            flagConexion=true;
            
            
            t= new Thread(){
                @Override
                public void run()
                {
                    tlectura.purge();
                    tlectura.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                //motor.putVariableAsync("modo_operacion_apagado", modoOperacionApagado);
                                motor.putVariableAsync("internal_operation_mode", modoOperacionInterno);
                                motor.putVariableAsync("external_operation_mode", modoOperacionExterno);
                                motor.putVariableAsync("parcial_operation_mode", modoOperacionParcial);
                                motor.putVariableAsync("sale_operation_mode", modoOperacionVenta);
                                motor.putVariableAsync("storage_operation_mode", modoOperacionAlmacenamiento);
                                
                                Future<Double> potenciaActual = motor.getVariableAsync("generated_power");
                                
                                while(!potenciaActual.isDone())
                                {
                                    
                                }
                                
                                potenciaGenerada=potenciaActual.get();
                                
                                Future<Double> potenciaDemanda = motor.getVariableAsync("demand_power");
                                while(!potenciaDemanda.isDone())
                                {
                                    
                                }
                                
                                demandaPotencia=potenciaDemanda.get();
                                
                                
                                
                                Future<Double> baterias = motor.getVariableAsync("stored_energy");
                                while(!baterias.isDone())
                                {
                                    
                                }
                                potenciaAlmacenada=baterias.get();
                                
                                /*Future<Double> voltajeMRMatlab = motor.getVariableAsync("voltaje_MR");
                                while(!voltajeMRMatlab.isDone())
                                {
                                    
                                }
                                voltajeMR=voltajeMRMatlab.get();
                                
                                Future<Double> frecuenciaMRMatlab = motor.getVariableAsync("frecuencia_MR");
                                while(!frecuenciaMRMatlab.isDone())
                                {
                                    
                                }
                                frecuenciaMR=frecuenciaMRMatlab.get();
                                
                                Future<Double> voltajeREMatlab = motor.getVariableAsync("voltaje_RE");
                                while(!voltajeREMatlab.isDone())
                                {
                                    
                                }
                                voltajeRE=voltajeREMatlab.get();
                                
                                Future<Double> frecuenciaREMatlab = motor.getVariableAsync("frecuencia_RE");
                                while(!frecuenciaREMatlab.isDone())
                                {
                                    
                                }
                                frecuenciaRE=frecuenciaREMatlab.get();
                                */
                                
                                //System.out.println("Variables de simulación: Potencia generada = " + potenciaGenerada + " potencia demanda = " + demandaPotencia + " potencia almacenada " + potenciaAlmacenada);
                                
                                
                            } catch (InterruptedException ex) {
                                Logger.getLogger(AgenteMicroRed.class.getName()).log(Level.SEVERE, null, ex);
                            } catch (ExecutionException ex) {
                                Logger.getLogger(AgenteMicroRed.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        }
                    }, 200, 500);
                }
            };
            t.run();
            
            
            }catch(Exception e)
            {
            
            Logger.getLogger(AgenteMicroRed.class.getName()).log(Level.SEVERE, null, e);
            
        }/* catch (EngineException ex) {
            Logger.getLogger(AgenteMicroRed.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(AgenteMicroRed.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalStateException ex) {
            Logger.getLogger(AgenteMicroRed.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(AgenteMicroRed.class.getName()).log(Level.SEVERE, null, ex);
        }*/
        }
        

    }


}
