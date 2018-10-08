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
import interfaz.InterfazMicroRed;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataset;
import paquetedbs.DAO;
import interfaz.InterfazMicroRed;
import jade.content.ContentElement;
import jade.content.lang.Codec;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.MessageTemplate;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.DecimalFormat;
import org.json.JSONObject;
import paquetedbs.PostgreSQLDAO;
import proyectosma.ontology.*;

public class AgenteInterfaz extends Agent {

    DAO baseDeDatos = null;
    Connection conDB = null;
    ResultSet rs;
    XYDataset dataset;
    InterfazMicroRed ventana = new InterfazMicroRed();
    Statement stmt = null;
    DatabaseMetaData dbmd = null;
    PreparedStatement pstmt = null;
    Map grafica = new HashMap();
    Map datosRecomendacion = new HashMap();
    Map datosRecomendacionSem = new HashMap();

    boolean flagConexion = false;
    double potenciaGenerada = 0;
    double demandaPotencia = 0;
    double potenciaAlmacenada = 0;
    int modoOperacionApagado = 0;
    int modoOperacionInterno = 0;
    int modoOperacionExterno = 0;
    int modoOperacionParcial = 0;
    int modoOperacionAlmacenamiento = 0;
    int modoOperacionVenta = 0;
    int[] modosOperacion;
    boolean fallaMR = false;
    boolean fallaRE = false;
    boolean flancoEstado0 = true;
    boolean flancoEstado1 = false;
    boolean flancoEstado2 = false;
    boolean flancoEstado3 = false;
    boolean flancoEstado4 = false;
    boolean flancoEstado5 = false;
    ComportamientoSugerencia csug = new ComportamientoSugerencia();

    private Codec codec = new SLCodec();

    private Ontology ontologia = ProyectoSMAOntology.getInstance();

    @Override
    protected void setup() {
        baseDeDatos = new PostgreSQLDAO("pruebas", "5432");
        String urlDB = baseDeDatos.getUrl();
        conDB = baseDeDatos.conectar(urlDB, "SantiagoGil", "adminpass");
        ventana.setVisible(true);
        getContentManager().registerLanguage(codec);
        getContentManager().registerOntology(ontologia);
        //this.addBehaviour(csug);
        this.addBehaviour(new ComportamientoSugerencia());
        this.addBehaviour(new ComportamientoGrafica(this, 2000));

        this.addBehaviour(new ComportamientoEscucharOperacion());
    }

    private class ComportamientoGrafica extends TickerBehaviour {

        public ComportamientoGrafica(Agent a, long period) {
            super(a, period);
        }

        @Override
        protected void onTick() {
            //System.out.println("Agente interfaz entrando al comportamiento gráfica");
            Calendar cactual = Calendar.getInstance();
            Calendar cmenos = Calendar.getInstance();
            cmenos.add(Calendar.DATE, -1);
            Timestamp timesactual = new Timestamp(cactual.getTimeInMillis());
            Timestamp timesmenos = new Timestamp(cmenos.getTimeInMillis());
            leerDB(timesmenos, timesactual);

        }

    }

    private class ComportamientoSugerencia extends OneShotBehaviour {

        @Override
        public void action() {
            //System.out.println("Agente interfaz entrando al comportamiento sugerencia de horarios");
            inferirRecomendacionesDia();
            inferirRecomendacionesSemana();
        }

    }

    private class ComportamientoEscucharOperacion extends CyclicBehaviour {

        @Override
        public void action() {
            //System.out.println("Agente interfaz entrando al comportamiento escuchar operación");
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchLanguage(codec.getName()),
                    MessageTemplate.MatchOntology(ontologia.getName()));
            //ACLMessage msg = blockingReceive(mt);
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
                            modoOperacionApagado = seleccion.getModoOperacionApagado();
                            modoOperacionInterno = seleccion.getModoOperacionInterno();
                            modoOperacionExterno = seleccion.getModoOperacionExterno();
                            modoOperacionParcial = seleccion.getModoOperacionParcial();
                            modoOperacionVenta = seleccion.getModoOperacionVenta();
                            modoOperacionAlmacenamiento = seleccion.getModoOperacionAlmacenamiento();
                            int[] modosOperacion = new int[]{modoOperacionApagado, modoOperacionInterno, modoOperacionExterno, modoOperacionParcial, modoOperacionVenta, modoOperacionAlmacenamiento};
                            /*System.out.println("El modo de operación en el que se encuentra la micro-red es: " + modoOperacion);
                            System.out.println("Las potencias del sistema son: Potencia almacenada = " + potenciaAlmacenada + ", potencia "
                                    + "generada = " + potenciaGenerada + ", potencia de demanda = " + demandaPotencia);
                            System.out.println("Las condiciones de falla enviadas por el agente " + msg.getSender().getName() + " son: Falla en la micro-red : " + fallaMR + ", falla en la red externa: " + fallaRE);
                             */
                            ventana.jLabel18.setText(String.valueOf(potenciaGenerada));
                            //ventana.jLabel18.setText(new DecimalFormat("##.##").format(potenciaGenerada));
                            ventana.jLabel21.setText(String.valueOf(demandaPotencia));
                            //ventana.jLabel21.setText(new DecimalFormat("##.##").format(demandaPotencia));
                            //ventana.jLabel22.setText(String.valueOf(modosOperacion));
                            ventana.jLabel22.setText("[" + modoOperacionApagado + ", " + modoOperacionInterno + ", " + modoOperacionExterno + ", " + modoOperacionParcial + ", " + modoOperacionVenta + ", "
                                    + modoOperacionAlmacenamiento + "]");
                            
                            ventana.jLabel26.setText(String.valueOf(potenciaAlmacenada));
                        }
                    } catch (Codec.CodecException ex) {
                        Logger.getLogger(AgenteInterfaz.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (OntologyException ex) {
                        Logger.getLogger(AgenteInterfaz.class.getName()).log(Level.SEVERE, null, ex);
                    }

                } else if (msg.getPerformative() == ACLMessage.PROPAGATE) {
                    try {
                        ContentElement ce = getContentManager().extractContent(msg);
                        if (ce instanceof EnviarPrecio) {
                            EnviarPrecio enviar = (EnviarPrecio) ce;
                            Precio selecPrecio = enviar.getInfoPrecio();
                            float valor = selecPrecio.getValor();
                            String tiempo = selecPrecio.getHora();
                            ventana.jLabel4.setText(String.valueOf(valor));
                            ventana.jLabel5.setText(tiempo);
                            inferirRecomendacionesDia();
                            inferirRecomendacionesSemana();
                        }
                    } catch (Codec.CodecException ex) {
                        Logger.getLogger(AgenteInterfaz.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (OntologyException ex) {
                        Logger.getLogger(AgenteInterfaz.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                block();
            }

        }
    }

    private void inferirRecomendacionesDia() {
        try {
            Date diaAnterior0 = new Date();
            Date diaAnterior24 = new Date();
            Calendar dAnt0 = Calendar.getInstance();
            Calendar dAnt24 = Calendar.getInstance();
            dAnt0.add(Calendar.DATE, -1);
            dAnt0.set(Calendar.HOUR_OF_DAY, 0);
            dAnt0.set(Calendar.MINUTE, 0);
            dAnt24.add(Calendar.DATE, -1);
            dAnt24.set(Calendar.HOUR_OF_DAY, 1);
            dAnt24.set(Calendar.MINUTE, 0);
            Timestamp ts1 = new Timestamp(dAnt0.getTimeInMillis());
            Timestamp ts2 = new Timestamp(dAnt24.getTimeInMillis());
            ArrayList<Double> contenidoMinimos = new ArrayList<Double>();
            for (int i = 0; i <= 23; i++) {
                dAnt0.set(Calendar.HOUR_OF_DAY, i);
                dAnt24.set(Calendar.HOUR_OF_DAY, i + 1);
                ts1 = new Timestamp(dAnt0.getTimeInMillis());
                ts2 = new Timestamp(dAnt24.getTimeInMillis());
                String st = "SELECT AVG(valorprecio) from precioenergia WHERE \"timest\" > ? AND \"timest\" <= ?";
                pstmt = conDB.prepareStatement(st);
                pstmt.setTimestamp(1, ts1);
                pstmt.setTimestamp(2, ts2);
                rs = pstmt.executeQuery();
                double prom = 0.0;
                while (rs.next()) {
                    prom = rs.getDouble(1);
                    contenidoMinimos.add(prom);

                }
                datosRecomendacion.put(i + ":00", prom);
                //Double min = Collections.min(contenidoMinimos);
                //System.out.println("Valor min: " + contenidoMinimos);
            }
            //System.out.println(datosRecomendacion);
            double horamin = 100;
            String thoramin = "";
            double horamax = 0;
            String thoramax = "";
            Iterator it = datosRecomendacion.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                if ((double) pair.getValue() < horamin) {
                    horamin = (double) pair.getValue();
                    thoramin = pair.getKey().toString();

                }
                if ((double) pair.getValue() > horamax) {
                    horamax = (double) pair.getValue();
                    thoramax = pair.getKey().toString();
                }
                //System.out.println(pair.getKey() + "=" + pair.getValue());
                it.remove(); // avoids a ConcurrentModificationException
            }
            /*System.out.println(thoramin);
            System.out.println(horamin);
            System.out.println(thoramax);
            System.out.println(horamax);*/

            ventana.jLabel10.setText(thoramin);
            ventana.jLabel10.setVisible(true);
            ventana.jLabel9.setVisible(true);
            ventana.jLabel7.setVisible(true);
            ventana.jLabel17.setText(thoramax);
            ventana.jLabel17.setVisible(true);
            ventana.jLabel12.setVisible(true);
            ventana.jLabel14.setVisible(true);
        } catch (SQLException ex) {
            Logger.getLogger(AgenteInterfaz.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void inferirRecomendacionesSemana() {
        Date fi1 = new Date();
        Date fi2 = new Date();
        double horaminsem = 10;
        double horamaxsem = 0;
        String thoraminsem = "";
        String thoramaxsem = "";
        double prom = 0;

        boolean flancopmin = false, flancopmax = false;
        boolean flancop = false;
        try {
            Date diaAnterior0 = new Date();
            Date diaAnterior24 = new Date();
            Calendar dAnt0 = Calendar.getInstance();
            Calendar dAnt24 = Calendar.getInstance();
            dAnt0.add(Calendar.DATE, -7);
            dAnt0.set(Calendar.HOUR_OF_DAY, 0);
            dAnt0.set(Calendar.MINUTE, 0);
            dAnt0.set(Calendar.SECOND, 0);
            dAnt24.add(Calendar.DATE, -7);
            dAnt24.set(Calendar.HOUR_OF_DAY, 0);
            dAnt24.set(Calendar.MINUTE, 0);
            dAnt24.set(Calendar.SECOND, 0);
            Timestamp ts1 = new Timestamp(dAnt0.getTimeInMillis());
            Timestamp ts2 = new Timestamp(dAnt24.getTimeInMillis());

            ArrayList<Double> contenidoMinimos = new ArrayList<Double>();
            Double[] promDia = new Double[24];
            for (int k = 0; k <= 23; k++) {
                promDia[k] = 0.0;
            }
            for (int j = 0; j <= 6; j++) {

                fi1.setTime(dAnt0.getTimeInMillis());
                fi2.setTime(dAnt24.getTimeInMillis());
                //System.out.println("Fecha inicio fi: " + fi1);
                //System.out.println("Fecha final fi: " + fi2);
                for (int i = 0; i <= 23; i++) {
                    dAnt0.set(Calendar.HOUR_OF_DAY, i);
                    dAnt24.set(Calendar.HOUR_OF_DAY, i + 1);
                    ts1 = new Timestamp(dAnt0.getTimeInMillis());
                    ts2 = new Timestamp(dAnt24.getTimeInMillis());
                    //System.out.println("Fecha inicio ts: " + ts1);
                    //System.out.println("Fecha final ts: " + ts2);
                    String st = "SELECT AVG(valorprecio) from precioenergia WHERE \"timest\" > ? AND \"timest\" <= ?";
                    pstmt = conDB.prepareStatement(st);
                    pstmt.setTimestamp(1, ts1);
                    pstmt.setTimestamp(2, ts2);
                    rs = pstmt.executeQuery();

                    while (rs.next()) {
                        prom = rs.getDouble(1);
                        if (!flancop) {
                            //System.out.println("Prom primera vez: " + prom);
                            promDia[i] = prom;
                        } else {
                            //System.out.println("Prom demás veces: " + prom);
                            promDia[i] = (prom + promDia[i]) / 2;
                        }

                    }
                    //System.out.println("prom día en día " + j + " hora " + i + ":00 " + promDia[i]);
                    datosRecomendacionSem.put(i + ":00", promDia[i]);
                    //Double min = Collections.min(contenidoMinimos);
                    //System.out.println("Valor min: " + contenidoMinimos);

                }

                flancop = true;
                dAnt0.add(Calendar.DATE, 1);
                //dAnt24.add(Calendar.DATE, 1);
            }

            Iterator it = datosRecomendacionSem.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                if ((double) pair.getValue() < horaminsem) {

                    horaminsem = (double) pair.getValue();
                    thoraminsem = pair.getKey().toString();

                }
                if ((double) pair.getValue() > horamaxsem) {
                    horamaxsem = (double) pair.getValue();
                    thoramaxsem = pair.getKey().toString();

                }
                //System.out.println(pair.getKey() + "=" + pair.getValue());
                it.remove(); // avoids a ConcurrentModificationException
            }

            ventana.jLabel2.setText(thoraminsem);
            ventana.jLabel15.setText(thoramaxsem);
            ventana.jLabel8.setVisible(true);
            ventana.jLabel2.setVisible(true);
            ventana.jLabel13.setVisible(true);
            ventana.jLabel15.setVisible(true);

        } catch (SQLException ex) {
            Logger.getLogger(AgenteInterfaz.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void leerDB(Timestamp temp1, Timestamp temp2) {

        try {

            grafica.clear();
            String st = "SELECT * FROM precioenergia WHERE \"timest\" > ? AND \"timest\" <= ? ORDER BY timest";
            pstmt = conDB.prepareStatement(st);
            pstmt.setTimestamp(1, temp1);
            pstmt.setTimestamp(2, temp2);
            rs = pstmt.executeQuery();
            //createDataset(rs);
            /*while(rs.next())
            {
                grafica.put(rs.getTimestamp("timest"), rs.getDouble("valorprecio")); 
                
            }
            System.out.println(grafica);*/
            //inferirRecomendacionesDia();

            ventana.llamarGrafica(rs);
            //inferirRecomendacionesDia();
        } catch (SQLException ex) {
            Logger.getLogger(AgenteInterfaz.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
