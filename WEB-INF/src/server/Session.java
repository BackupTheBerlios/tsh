package server;

import common.Constants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import java.util.Date;
import java.util.Random;

/**
 * Clase que mantiene la informacion necesaria de una sesion de conexion con un
 * host remoto Fecha 18-oct-2003
 * 
 * @author jmgarcia
 */
public class Session {
    //Logger
    private static Log logger = LogFactory.getLog(Session.class);

    //Contador para generar los identificadores de sesion
    private static long contador = 0;

    //Generador de numeros de sesion
    private static final Random generador = new Random();

    //Identificador de sesion
    private long idSesion = -1;

    //Servicio
    private String service = null;

    //Socket de conexion con el servidor remoto
    private Socket socket = null;

    //InputStream del socket
    private InputStream input = null;

    //OutputStream del socket
    private OutputStream output = null;

    //Maquina
    private String host = null;

    //puerto
    private int port = -1;

    // Objeto utilizado para sincronizacion de acceso al socket
    private final Object lock = new Object();

    //Time que indica el ultimo acceso que se hizo a esta sesion
    private long lastAccessTime;

    //Timeout de la sesion
    private long timeout;

    /**
	 * Constructor de sesion
	 * 
	 * @param sinfo
	 *            Contiene la informacion de servicio que se proporciona por
	 *            esta session
	 */
    public Session(ServiceServerInfo sinfo) {
        contador++; //Se incrementa el numero de sesiones activas
        this.idSesion = generador.nextLong();

        this.service = sinfo.getName();
        this.host = sinfo.getHost();
        this.port = sinfo.getPort();
        this.timeout = sinfo.getSessionTimeout();
        this.resetTime();
    }

    /**
	 * Devuelve el id de la sesion
	 * 
	 * @return Id de la sesion
	 */
    public long getId() {
        return this.idSesion;
    }

    /**
	 * Getter
	 * 
	 * @return Devuelve el inputStream para leer datos del servidor remoto de
	 *         esta sesion
	 */
    public InputStream getInputStream() {
        return input;
    }

    /**
	 * Getter
	 * 
	 * @return Devuelve el outputStream para escribir datos en el servidor
	 *         remoto de esta sesion
	 */
    public OutputStream getOutputStream() {
        return output;
    }

    /**
	 * Cierra una sesion. Libera los recursos del socket
	 */
    public void close() {
        if (this.socket != null) {
            try {
                synchronized (this.lock) {
                    logger.info("Cerrando sesion " + this);
                    if (this.input != null) {
                        try {
                            input.close();
                        }
                        catch (IOException e) {
                            logger.debug("Error cerrando input " + this, e);
                        }
                    }
                    this.input = null;

                    if (this.output != null) {
                        try {
                            output.close();
                        }
                        catch (IOException e) {
                            logger.debug("Error cerrando output " + this, e);
                        }
                    }
                    this.output = null;

                    this.socket.close();
                    this.socket = null;

                }
            }
            catch (IOException e) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Error cerrando sesion " + this, e);
                }
            }
        }
    }

    /**
	 * Devuelve true si la sesion ha estado inactiva por mas tiempo del
	 * configurado
	 * 
	 * @return true si la sesion esta inactiva
	 */
    public boolean inactive() {
        return (new Date().getTime() - this.lastAccessTime) >= this.timeout;
    }

    /**
	 * Inicia una sesion, abre una conexion con el servidor remoto
	 * 
	 * @throws UnknownHostException
	 * @throws IOException
	 */
    public void open() throws UnknownHostException, IOException {
        this.socket = new Socket(InetAddress.getByName(this.host), this.port);
        this.socket.setSoTimeout(Constants.READ_TIMEOUT);
        this.input = new BufferedInputStream(this.socket.getInputStream());
        this.output = this.socket.getOutputStream();
        logger.info("Abriendo conexion con " + this.host + ":" + this.port);
    }

    /**
	 * Resetea el tiempo de acceso
	 */
    public void resetTime() {
        this.lastAccessTime = new Date().getTime();
    }

    /**
	 * Metodo toString
	 * 
	 * @return DOCUMENT ME!
	 */
    public String toString() {
        return "Sesion[" + this.idSesion + "," + this.service + "]";
    }
}