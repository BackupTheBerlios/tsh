package org.tsh.client;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tsh.common.Constants;
import org.tsh.common.IStat;

/**
 * Maneja una sesion entre el cliente local y el servidor remoto a traves del protocolo http Fecha
 * 08-oct-2003
 * 
 * @author jmgarcia
 */
public class ProxyHost implements Runnable {
    /** objeto de log */
    private Log logger = LogFactory.getLog(ProxyHost.class);

    /** ServiceManager */
    private ServiceManager serviceManager = null;

    /** Socket con el cliente */
    private Socket clientSocket = null;

    /** Id del Proxy */
    private long id = Constants.NO_SESSION;

    /** Stream de lectura del cliente */
    private InputStream clientInput = null;

    /** Stream de escritura en cliente */
    private OutputStream clientOutput = null;

    /** Manejador de conexiones para este proxy */
    private ConnectionManager connectionManager = null;

    /** Indica que el proxy esta ejecutandose */
    private boolean running = true;

    /** Objeto para acumular las estadisticas */
    private IStat stat = null;

    private String remoteAddress = null;
    private String service = null;

    /**
	 * Objeto que hace de proxy de la conexion
	 * 
	 * @param c ServiceManager que ha creado este objeto
	 * @param s Socket de conexion con el cliente
	 * @param connectionManager Manejador de conexiones con el servidor remoto
	 * @param stat Objeto para acumular las estadisticas de este Proxy
	 * @throws IOException Error de Entrada salida
	 */
    public ProxyHost(ServiceManager c, Socket s, ConnectionManager connectionManager, IStat stat)
        throws IOException {
        this.serviceManager = c;
        this.clientSocket = s;
        this.connectionManager = connectionManager;
        this.stat = stat;
        stat.startConn();

        s.setSoTimeout(Constants.READ_TIMEOUT);

        //Para trazas
        remoteAddress = this.clientSocket.getRemoteSocketAddress().toString();
        service = this.serviceManager.getService();

        //Obtener stream de lectura y de escritura
        this.clientInput = new BufferedInputStream(this.clientSocket.getInputStream());
        this.clientOutput = this.clientSocket.getOutputStream();

        //Se registra en el service manager
        this.serviceManager.register(this);
    }

    /**
	 * Devuelve el id de este proxy
	 * 
	 * @return id del proxy
	 */
    public long getId() {
        return id;
    }

    /**
	 * Se cierra este Proxy. Se liberan los recursos locales y con el servidor remoto
	 */
    public void close() {
        if (logger.isDebugEnabled()) {
            logger.debug("Cerrando " + this);
        }

        //No hay que seguir ejecutando
        this.running = false;

        //Cierro las conexiones
        closeLocalConnection();

        stat.stopConn();
        
        //Se borra del ServiceManager
        serviceManager.remove(this);
        
    }

    /**
	 * Se cierran las conexiones locales
	 */
    private void closeLocalConnection() {
        if (this.clientInput != null) {
            try {
                this.clientInput.close();
                this.clientInput = null;
            } catch (IOException e1) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Cerrando clientInput " + this, e1);
                }
            }
        }

        if (this.clientOutput != null) {
            try {
                this.clientOutput.close();
                this.clientOutput = null;
            } catch (IOException e1) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Cerrando clientOutput " + this, e1);
                }
            }
        }

        if (this.clientSocket != null) {
            try {
                this.clientSocket.close();
                this.clientSocket = null;
            } catch (IOException e) {
                logger.warn("Error cerrando conexion con cliente " + this, e);
            }
        }
    }

    /**
	 * Metodo principal. Maneja la sesion entre cliente local y servidor remoto
	 * 
	 * @see java.lang.Runnable#run()
	 */
    public void run() {
        //Creo conexion Http remota
        RemoteConn rc =
            new RemoteConn(
                this.clientInput,
                this.clientOutput,
                connectionManager,
                service,
                this.stat,this);

        //Arranco la conexion remota
        rc.run();

        //La sesion se ha cerrado (o dejado de utilizar)
        //setId(Constants.NO_SESSION);

        //Se liberan recursos
        this.close();
    }

    /**
	 * Devuelve informacion del ProxyHost
	 * 
	 * @return String
	 */
    public String toString() {
        return " [" + service + "," + remoteAddress + "," + this.id + "] ";
    }

    /**
	 * Establece el id del proxy. Se envia en las sucesivas peticiones al servidor
	 * 
	 * @param l Id de la conexion.
	 */
    void setId(long l) {
        id = l;
    }

}