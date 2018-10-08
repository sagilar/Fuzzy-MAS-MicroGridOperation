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
import interfaz.FrameInferencia;
import jade.content.ContentElement;
import jade.content.abs.AbsContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sourceforge.jFuzzyLogic.FIS;
import net.sourceforge.jFuzzyLogic.FunctionBlock;
import net.sourceforge.jFuzzyLogic.defuzzifier.Defuzzifier;
import net.sourceforge.jFuzzyLogic.membership.MembershipFunction;
import net.sourceforge.jFuzzyLogic.plot.JFuzzyChart;
import net.sourceforge.jFuzzyLogic.rule.LinguisticTerm;
import net.sourceforge.jFuzzyLogic.rule.Variable;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataset;
import org.json.JSONObject;
import paquetedbs.DAO;
import paquetedbs.PostgreSQLDAO;
import proyectosma.ontology.*;

public class AgenteMercadoDeEnergia extends Agent {

    String timeStr = "";
    long timeMilis = 0;
    String priceStr = "";
    Timestamp fechahora;
    double price = 0;
    Date fecha = new Date();
    DAO baseDeDatos = null;
    Connection conDB = null;
    ResultSet rs;
    XYDataset dataset;
    JFreeChart chart;
    ChartPanel chartPanel;
    Statement stmt = null;
    DatabaseMetaData dbmd = null;
    PreparedStatement pstmt = null;
    boolean flagConexion = false;
    double potenciaGenerada = 0;
    double demandaPotencia = 0;
    double potenciaAlmacenada = 0;
    int modoOperacion = 0;
    int modoOperacionApagado = 0;
    int modoOperacionInterno = 0;
    int modoOperacionExterno = 0;
    int modoOperacionParcial = 0;
    int modoOperacionAlmacenamiento = 0;
    int modoOperacionVenta = 0;

    int[] modosOperacion;

    boolean flancoEstado0 = true;
    boolean flancoEstado1 = false;
    boolean flancoEstado2 = false;
    boolean flancoEstado3 = false;
    boolean flancoEstado4 = false;
    boolean flancoEstado5 = false;
    boolean fallaMR = false;
    boolean fallaRE = false;
    FrameInferencia finf=new FrameInferencia();
    JFuzzyChart jfc;
    ComportamientoModoOperacion comportamientoModoOperacion = new ComportamientoModoOperacion();

    private Codec codec = new SLCodec();

    private Ontology ontologia = ProyectoSMAOntology.getInstance();

    @Override
    protected void setup() {
        baseDeDatos = new PostgreSQLDAO("pruebas", "5432");
        String urlDB = baseDeDatos.getUrl();
        conDB = baseDeDatos.conectar(urlDB, "SantiagoGil", "adminpass");
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontologia);
        this.addBehaviour(new ComportamientoPrecio(this, 30000));
        //this.addBehaviour(comportamientoModoOperacion);
        this.addBehaviour(new ComportamientoEscucharOperacion());
        
        
        
    }

    private class ComportamientoPrecio extends TickerBehaviour {

        public ComportamientoPrecio(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            System.out.println("Agente mercado de energía entrando al comportamiento precio");
            try {
                String uri = "https://hourlypricing.comed.com/api?type=currenthouraverage&format=json";
                URL obj = new URL(uri);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("GET");
                String responseEncode = con.getContentEncoding();
                String respuesta = "";

                //Socket socket=new Socket(urlComplete,80);
                try {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(con.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    //print result
                    respuesta = response.toString();
                    respuesta = respuesta.replace("\"", "");
                    respuesta = respuesta.replace("[", "");
                    respuesta = respuesta.replace("]", "");

                    JSONObject jsonObject = new JSONObject(respuesta);

                    timeStr = jsonObject.get("millisUTC").toString();
                    priceStr = jsonObject.get("price").toString();
                    price = Math.abs(Double.parseDouble(priceStr));
                    timeMilis = Long.parseLong(timeStr);
                    boolean flagGuarda = true;
                    if (price < 1.1) {
                        flagGuarda = false;
                    }

                    //if (flagGuarda) {
                    fecha.setTime(timeMilis);

                    fechahora = new Timestamp(timeMilis);

                    almacenarDB(fechahora, price);
                    addBehaviour(new ComportamientoModoOperacion());
                    
                    //}

                } catch (Exception e) {
                    Logger.getLogger(AgenteMercadoDeEnergia.class.getName()).log(Level.SEVERE, null, e);
                }
            } catch (MalformedURLException ex) {
                Logger.getLogger(AgenteMercadoDeEnergia.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ProtocolException ex) {
                Logger.getLogger(AgenteMercadoDeEnergia.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(AgenteMercadoDeEnergia.class.getName()).log(Level.SEVERE, null, ex);
            }

        }

    }

    private void almacenarDB(Timestamp tiempo, double precio) {

        String st;
        try {

            st = "INSERT INTO precioenergia VALUES (?,?)";
            pstmt = conDB.prepareStatement(st);
            pstmt.setTimestamp(1, tiempo);
            pstmt.setDouble(2, precio);
            pstmt.executeUpdate();

        } catch (SQLException ex) {
            /*try {
                st = "UPDATE precioenergia SET timest = ?, valorprecio = ? WHERE timest = ?";
                pstmt = conDB.prepareStatement(st);
                pstmt.setTimestamp(1, tiempo);
                pstmt.setDouble(2, precio);
                pstmt.setTimestamp(3, tiempo);
                pstmt.executeUpdate();

                //Logger.getLogger(VentanaPrincipal.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SQLException ex1) {
                Logger.getLogger(VentanaPrincipal.class.getName()).log(Level.SEVERE, null, ex1);
            }*/
            //Logger.getLogger(VentanaPrincipal.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private class ComportamientoModoOperacion extends OneShotBehaviour {

        @Override
        public void action() {
            try {
                //System.out.println("Agente mercado de energía entrando al comportamiento modo de operación");
                int[] modosEnvio = inferenciaModoOperacion();
                ACLMessage mensaje = new ACLMessage();
                AID r = new AID();
                r.setLocalName("MicrogridAgent");
                mensaje.setSender(getAID());
                mensaje.addReceiver(r);
                mensaje.setLanguage(codec.getName());
                mensaje.setOntology(ontologia.getName());
                mensaje.setPerformative(ACLMessage.INFORM);
                Operacion operacion = new Operacion();
                operacion.setModoOperacionApagado(modosEnvio[0]);
                operacion.setModoOperacionInterno(modosEnvio[1]);
                operacion.setModoOperacionExterno(modosEnvio[2]);
                operacion.setModoOperacionParcial(modosEnvio[3]);
                operacion.setModoOperacionVenta(modosEnvio[4]);
                operacion.setModoOperacionAlmacenamiento(modosEnvio[5]);
                EnviarInfoOperacion enviarModoOperacion = new EnviarInfoOperacion();
                enviarModoOperacion.setParametros(operacion);
                //mensaje.setContentObject(enviarModoOperacion);
                getContentManager().fillContent(mensaje, (ContentElement) enviarModoOperacion);
                send(mensaje);
            } catch (Codec.CodecException ex) {
                Logger.getLogger(AgenteMercadoDeEnergia.class.getName()).log(Level.SEVERE, null, ex);
            } catch (OntologyException ex) {
                Logger.getLogger(AgenteMercadoDeEnergia.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
            try {
                
                
                ACLMessage msjiinterfaz = new ACLMessage();
                AID r = new AID();
                r.setLocalName("InterfaceAgent");
                msjiinterfaz.setSender(getAID());
                msjiinterfaz.addReceiver(r);
                msjiinterfaz.setLanguage(codec.getName());
                msjiinterfaz.setOntology(ontologia.getName());
                msjiinterfaz.setPerformative(ACLMessage.PROPAGATE);
                Precio precioEnvio= new Precio();
                precioEnvio.setValor((float) price);
                precioEnvio.setHora(fecha.toString());
                EnviarPrecio enviarPrecio = new EnviarPrecio();
                enviarPrecio.setInfoPrecio(precioEnvio);
                
                //mensaje.setContentObject(enviarModoOperacion);
                getContentManager().fillContent(msjiinterfaz, (ContentElement) enviarPrecio);
                send(msjiinterfaz);
            } catch (Codec.CodecException ex) {
                Logger.getLogger(AgenteMercadoDeEnergia.class.getName()).log(Level.SEVERE, null, ex);
            } catch (OntologyException ex) {
                Logger.getLogger(AgenteMercadoDeEnergia.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }

    }

    private class ComportamientoEscucharOperacion extends CyclicBehaviour {

        @Override
        public void action() {
            //System.out.println("Agente mercado de energía entrando al comportamiento escuchar operación");

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
                            potenciaAlmacenada = seleccion.getPotenciaAlmacenada();
                            potenciaGenerada = seleccion.getPotenciaGenerada();
                            demandaPotencia = seleccion.getPotenciaDemanda();
                            fallaMR = seleccion.getEstadoFallaMicroRed();
                            fallaRE = seleccion.getEstadoFallaRedExterna();

                            /*System.out.println("Las potencias del sistema son: Potencia almacenada = " + potenciaAlmacenada + ", potencia "
                                    + "generada = " + potenciaGenerada + ", potencia de demanda = " + demandaPotencia);
                            System.out.println("Las condiciones de falla enviadas por el agente " + msg.getSender().getName() + " son: Falla en la micro-red : " + fallaMR + ", falla en la red externa: " + fallaRE);
                             */
                        }
                    } catch (Codec.CodecException ex) {
                        Logger.getLogger(AgenteMicroRed.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (OntologyException ex) {
                        Logger.getLogger(AgenteMicroRed.class.getName()).log(Level.SEVERE, null, ex);
                    }

                } else if (msg.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    System.out.println("El agente micro-red ha aceptado el modo de operación sugerido.");
                } else if (msg.getPerformative() == ACLMessage.REJECT_PROPOSAL) {
                    System.out.println("El agente micro-red ha rechazado el modo de operación sugerido.");
                }
            } else {
                block();
            }

        }
    }

    private int[] inferenciaModoOperacion() {
        double relacionPotencia = potenciaGenerada - demandaPotencia;
        double potenciadisponible = potenciaGenerada + potenciaAlmacenada;
        double salidalibre=1;
        double salidaexterno=0;
        
        
        if (potenciaAlmacenada == 0 && potenciaGenerada == 0) {
            System.out.println("Condiciones cumplen modo de operación 0");
            modoOperacion = 0;
            return new int[]{1, 0, 0, 0, 0, 0};
        }
        
        if(relacionPotencia >=0)
        {
            modoOperacionInterno=1;
            modoOperacionExterno=0;
            modoOperacionParcial = 0;
            FIS fislibre = FIS.load("src/agentes/fis1.fcl", true); // Load from 'FCL' file
            fislibre.setVariable("positive_relation", "price", price);// Set inputs
            fislibre.setVariable("positive_relation","stored_energy", potenciaAlmacenada);
        

            fislibre.evaluate(); // Evaluate
            salidalibre=fislibre.getFunctionBlock("positive_relation").getVariable("free_operation").getValue();
            System.out.println("Salida para venta o almacenamiento: " + salidalibre);
            JFuzzyChart.get().chart(fislibre.getFunctionBlock("positive_relation"));
            if(salidalibre>0.5)
            {
                modoOperacionAlmacenamiento=1;
                modoOperacionVenta=0;
            }else
            {
                modoOperacionAlmacenamiento=0;
                modoOperacionVenta=1;
            }
        }else
        {
            modoOperacionInterno=0;
            modoOperacionVenta=0;
            modoOperacionAlmacenamiento=0;
            FIS fisexterno = FIS.load("src/agentes/fis1.fcl", true); // Load from 'FCL' file
            fisexterno.setVariable("negative_relation","price", price); // Set inputs
            fisexterno.setVariable("negative_relation","available_power", potenciadisponible);
        
            fisexterno.evaluate(); // Evaluate
            salidaexterno=fisexterno.getFunctionBlock("negative_relation").getVariable("external_operation").getValue();
            System.out.println("Salida para consumo externo total o parcial: " + salidaexterno);
            
            JFuzzyChart.get().chart(fisexterno.getFunctionBlock("negative_relation"));
            if(salidaexterno>0.5)
            {
                modoOperacionParcial=1;
                modoOperacionExterno=0;
            }else
            {
                modoOperacionParcial=0;
                modoOperacionExterno=1;
                modoOperacionAlmacenamiento=1;
            }
        }
        
        /*System.out.println("Variables de inferencia: Potencia generada: " + potenciaGenerada + " potencia demanda: " + demandaPotencia + " relacionpotencia: "
                + "" + relacionPotencia + " potencia almacenada: " + potenciaAlmacenada + " Precio: " + price);
        if (potenciaAlmacenada == 0 && potenciaGenerada == 0) {
            System.out.println("Condiciones cumplen modo de operación 0");
            modoOperacion = 0;
            return new int[]{1, 0, 0, 0, 0, 0};
        }
        if (relacionPotencia + potenciaAlmacenada > 0) {
            System.out.println("Condiciones cumplen modo de operación 1");
            modoOperacion = 1;
            modoOperacionInterno = 1;
            modoOperacionExterno = 0;
            modoOperacionParcial = 0;
        } else if (price > 2.0 && relacionPotencia < (-potenciaAlmacenada)) {
            System.out.println("Condiciones cumplen modo de operación 2");
            modoOperacion = 2;
            modoOperacionInterno = 0;
            modoOperacionExterno = 1;
            modoOperacionParcial = 0;
        } else if (price < 2.0 && relacionPotencia < (-potenciaAlmacenada)) {
            System.out.println("Condiciones cumplen modo de operación 3");
            modoOperacion = 3;
            modoOperacionInterno = 0;
            modoOperacionExterno = 0;
            modoOperacionParcial = 1;
        }

        if ((relacionPotencia > 0 && potenciaAlmacenada > 200.0) || (relacionPotencia > 0 && price > 3.5)) {
            System.out.println("Condiciones cumplen modo de operación venta");
            modoOperacion = 4;
            modoOperacionVenta = 1;
        } else {
            modoOperacionVenta = 0;
        }
        if ((relacionPotencia > 0 && potenciaAlmacenada < 300.0 && price < 2.5) || (relacionPotencia > 0 && price < 2.2)) {
            System.out.println("Condiciones cumplen modo de operación almacenamiento");
            modoOperacion = 5;
            modoOperacionAlmacenamiento = 1;
        } else {
            modoOperacionAlmacenamiento = 0;
        }*/

        return new int[]{0, modoOperacionInterno, modoOperacionExterno, modoOperacionParcial, modoOperacionVenta, modoOperacionAlmacenamiento};
    }

    

}
