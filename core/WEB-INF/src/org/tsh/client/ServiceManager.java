package org.tsh.client;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.tsh.common.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author juanma
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
/**
 * Objeto para aceptar peticiones en un puerto determinado, realiza la conexion
 * contra un servicio remoto especifico Fecha 08-oct-2003
 * 
 * @author jmgarcia
 */
public class ServiceManager implements Runnable {
    private Log logger = LogFactory.getLog(ServiceManager.class);

    //Puerto de escucha de peticiones
    private int port = -1;

    // Servicio remoto al que se conecta
    private String service = null;

    //Indica si hay que terminar esta conexion, ya no aceptara mas peticiones
    private boolean terminate = false;

    // Conexion con el servidor http
    private ConnectionManager connManager = null;

    //Numero de conexiones abiertas para este servicio
    private int numConn = 0;

    //Numero maximo de conexiones simultaneas para este servicio
    private int maxConn = 0;

    //Conexiones abiertas para este servicio
    private Map conns = new HashMap();
    
    //Estadisticas
    private Map stats = new HashMap();

    private long maxConnTime;

    /**
	 * Crea un manejador para este servicio
	 * 
	 * @param service
	 *            Informacion del servicio
	 */
    public ServiceManager(ServiceInfo service) {
        this.port = service.getLocalPort();
        this.service = service.getName();
        this.connManager = new ConnectionManager(service);
        this.maxConn = service.getMaxConnections();
        this.maxConnTime = service.getMaxConnectionTime();
    }
    
    public long getMaxConnectionTime() {
       return this.maxConnTime;
    }

    /**
	 * Devuelve el nombre del servicio que maneja este manager
	 * 
	 * @return Servicio remoto que maneja esta manager
	 */
    public String getService() {
        return service;
    }

    /**
	 * Establece el servicio como terminado
	 * 
	 * @param b
	 */
    public void setTerminate(boolean b) {
        terminate = b;
    }

    /**
	 * Bucle principal del manejador del servicio
	 */
    public void run() {
        ServerSocket server = null;

        try {
            //Se crea el socket de escucha de nuevas conexiones
            server = new ServerSocket(this.port);
            logger.info("Arrancado Servicio. Esperando Peticiones " + this);

            //Bucle de aceptacion de nuevas conexiones
            Socket s = null;

            while (!this.terminate) {
                s = server.accept();
                logger.info(" Acceptada peticion de " + s.getRemoteSocketAddress());

                //Crea un objeto para manejar la peticion
                IStat stat = new StatProxyHost();
                stats.put(new ProxyHost(this, s, connManager,stat),stat);

            }

            logger.info("Servicio Terminado " + this);
        }
        catch (IOException e) {
            logger.warn("Error en servicio " + this, e);
        }
        finally {
            //Terminar proxys de este servicio
            this.terminateAll();
            //Liberar recursos
            if (server != null) {
                try {
                    server.close();
                    server = null;
                }
                catch (IOException e1) {
                    logger.debug("Error cerrando conexion " + this, e1);
                }
            }

        }
    }

    /**
	 * Representacion del objeto para poder imprimirlo en mensajes de log
	 * 
	 * @return string con maquina, puerto y servicio
	 */
    public String toString() {
        return " [" + this.service + " -> localhost:" + this.port + ", " + numConn + "/" + maxConn
                + "]";
    }

    /**
	 * Registra y arranca un proxy en este manejador de servicios
	 * 
	 * @param proxy
	 *            Proxy a registrar y arrancar
	 */
    public void register(ProxyHost proxy) {
        synchronized (this.conns) {
            if (this.numConn < this.maxConn) {
                //Se registra el proxy
                Thread t = new Thread(proxy);
                this.conns.put(proxy, t);
                this.numConn++;
                //Arranca el proxy de la peticion
                t.start();
                logger.info(this + " Registrado proxy " + proxy);
            }
            else {
                logger.info("Maximo numero de conexiones excedidas para " + this.service + ":"
                        + this.maxConn);
                proxy.close();
            }
        }
    }

    /**
	 * Elimina un proxy de este manejador de servicios
	 * 
	 * @param proxy
	 *            Proxy a eliminar
	 */
    public void remove(ProxyHost proxy) {
        synchronized (this.conns) {
            Thread t = (Thread) this.conns.remove(proxy);

            if (t != null) {
                this.numConn--;
                if (proxy != null) {                    
                    //proxy.stop();
                    //Recupero objeto de estadisticas y las pinta
                    IStat stat = (IStat)this.stats.get(proxy);
                    if ( stat != null ) {
                        logger.info(proxy + " " + stat);
                    }
                }
                logger.info("Eliminado proxy de servicio " + this.service + ":" + this.numConn
                        + "/" + this.maxConn);
            }
        }
    }

    /**
	 * Termina todos los proxy manejados por este manager.
	 */
    private void terminateAll() {
        synchronized (this.conns) {
            if (this.conns.size() > 0) {

                ProxyHost proxy = null;
                Iterator it = this.conns.values().iterator();

                while (it.hasNext()) {
                    proxy = (ProxyHost) it.next();
                    remove (proxy);
                }
                
                conns.clear();
            }
        }

    }

}