package client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import common.*;
import common.Constants;

/**
 * @author jmgarcia
 *  
 */
/**
 * Maneja una sesion entre el cliente local y el servidor remoto a traves del
 * protocolo http Fecha 08-oct-2003
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

    /** Objeto con la conexion para realizar el post */
    HttpURLConnection post = null;

    OutputStreamWriter postOutput = null;
    
    private String remoteAddress = null;
    private String service = null;

    /**
	 * Objeto que hace de proxy de la conexion
	 * 
	 * @param c
	 *            ServiceManager que ha creado este objeto
	 * @param s
	 *            Socket de conexion con el cliente
	 * @param connectionManager
	 *            Manejador de conexiones con el servidor remoto
	 * @param stat Objeto para acumular las estadisticas de este Proxy
	 * @throws IOException
	 *             Error de Entrada salida
	 */
    public ProxyHost(ServiceManager c, Socket s, ConnectionManager connectionManager,IStat stat)
            throws IOException {
        this.serviceManager = c;
        this.clientSocket = s;
        this.connectionManager = connectionManager;
        this.stat = stat;
            
        //Para trazas
        remoteAddress = this.clientSocket.getRemoteSocketAddress().toString();
        service = this.serviceManager.getService();
        
        //El read del socket tendra un timeout
        this.clientSocket.setSoTimeout(Constants.READ_TIMEOUT);

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
	 * Se cierra este Proxy. Se liberan los recursos locales y con el servidor
	 * remoto
	 */
    public void close() {
        if (logger.isDebugEnabled()) {
            logger.debug("Cerrando " + this);
        }

        //No hay que seguir ejecutando
        this.running = false;

        if (getId() != Constants.NO_SESSION) {
            //Hay que cerrar conexion con el servidor remoto
            sendClose();
        }

        //Cierro las conexiones
        closeLocalConnection();

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
            }
            catch (IOException e1) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Cerrando clientInput " + this, e1);
                }
            }
        }

        if (this.clientOutput != null) {
            try {
                this.clientOutput.close();
                this.clientOutput = null;
            }
            catch (IOException e1) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Cerrando clientOutput " + this, e1);
                }
            }
        }

        if (this.clientSocket != null) {
            try {
                this.clientSocket.close();
                this.clientSocket = null;
            }
            catch (IOException e) {
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

        //Abrir conexion con la maquina remota -> enviar open
        long connectionId = sendOpen();

        //No se ha podido establece la conexion
        if (connectionId == Constants.NO_SESSION) {
            logger.warn("No se ha podido establecer conexion con servicio: "
                    + this.serviceManager.getService());
            setId(Constants.NO_SESSION);
            this.close();

            return;
        }

        //Conexion abierta correctamente con sistema remoto
        //Se establece el id de la sesion enviado por el servidor        
        setId(connectionId);
        logger.info("Creada sesion " + this);

        //Numero de veces que se ha producido timeout
        //Se utiliza para contar cuantas peticiones seguidas de timeout
        //se realizan sin que el servidor devuelva respuesta
        int timeout = 0;
        int serverNotWriting = 0;

        //bytes leidos del servidor
        long readedFromServer = 0;

        //bytes leidos
        int data = 0;

        //bytes leidos del cliente
        long readedFromClient = 0;

        boolean sendData = false;

        int state = 0;

        try {
            //Bucle principal de lectura y escritura de datos de la sesion
            while (this.running) {
                readedFromClient = 0;
                sendData = false;

                //Se abre una conexion con el servidor
                connect();

                switch (state) {
                    case 0 :
                        try {
                            //Se leen datos del cliente y se envia al
                            // servidor
                            while ((data = this.clientInput.read()) >= 0) {
                                if (!sendData) {
                                    //Añadir parametro de datos al POST
                                    setHeader(Constants.COMMAND_WRITE, this.serviceManager
                                            .getService(), getId());
                                    sendData = true;
                                }
                                readedFromClient++;
                                
                                //Acumula estadistica
                                stat.addSend(1);
                                
                                this.postOutput.write(data);
                                this.postOutput.flush();
                            }
                        }
                        catch (SocketTimeoutException e) {
                            if (logger.isDebugEnabled()) {
                                logger.debug(this + "Timeout en lectura del cliente");
                            }
                            timeout++;
                            if (!sendData) {
                                state = 1;
                            }
                        }

                        //Añadir informacion para un write
                        if (sendData) {
                            this.postOutput.flush();
                            if (logger.isInfoEnabled()) {
                                logger.debug(this + ":Enviados en Write " + readedFromClient
                                        + " bytes al servidor remoto");
                            }
                        }
                        break;

                    case 1 :
                        if (logger.isDebugEnabled()) {
                            logger.debug(this + ":Enviando Send");
                        }
                        //Se añaden datos para un sendWrite
                        setHeader(Constants.COMMAND_READ, this.serviceManager.getService(), getId());
                        sendData = true;
                        timeout = 0;
                        state = 0;
                }

                //Se han leido datos del cliente, se envia petición al
                // servidor
                if (sendData) {
                    //Leer datos y enviar respuesta al cliente
                    readedFromServer = writeResponse();
                    if (readedFromServer < 0) {
                        logger.debug(this + ":Error leyendo del servidor");
                        this.running = false;
                    }
                    else if (readedFromServer == 0) {
                        serverNotWriting++;
                    }
                    else {
                        //Resetea el contador de veces que el servidor no ha
                        // enviado
                        //respuesta
                        serverNotWriting = 0;
                        state = 1;
                        timeout = 0;
                    }
                    //Se desconecta
                    disconnect();

                }
                else if (timeout > 1) {
                    //state = (state+1)%2;
                }
                //Se ha cerrado conexion por parte del cliente
                if (data < 0) {
                    logger.debug(this + "Cliente ha cerrado la conexion");
                    this.running = false;
                }
            }
        }
        catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug(this + "Error en proxy ", e);
            }
            this.running = false;
        }
        finally {
            this.disconnect();
            this.close();
        }
    }

    /**
	 * Lee respuesta del servidor y la devuelve al cliente
	 * 
	 * @param post
	 * @return
	 */
    private long writeResponse() {
        BufferedInputStream input = null;

        try {
            input = new BufferedInputStream(post.getInputStream());

            //flag para comprobar el codigo de retorno del servidor
            boolean first = true;

            byte[] buffer = new byte[Constants.BUFFER];

            long maxReaded = 0;

            int readed = -1;

            //Se leen datos de la respuesta
            while ((readed = input.read(buffer)) >= 0) {
                if (readed > 0) {
                    //Acumula estadistica
                    stat.addRecieve(readed);
                    
                    //Se ha leido algun byte
                    if (first) {
                        //Es la primera linea de la respuesta.
                        //Se comprueba que no se devuelva el comando ko.
                        first = false;
                        if (readed == 3 && new String(buffer, 0, 2).equals(Constants.COMMAND_KO)) {
                            //Se ha producido un error en el servidor.
                            logger.debug("Recibido del servidor comando [" + Constants.COMMAND_KO
                                    + "]");
                            return -1;

                        }
                    }
                    //Acumula el numero de bytes leido
                    maxReaded += readed;
                    //Se escriben al cliente
                    this.clientOutput.write(buffer, 0, readed);
                }
            }

            logger.debug("Leidos " + maxReaded + " del servidor" + this);

            return maxReaded;
        }
        catch (IOException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error escribiendo respuesta al cliente local " + this, e);
            }

            return -1;
        }
        finally {
            if (input != null) {
                try {
                    input.close();
                }
                catch (IOException e1) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Error cerrando la conexion en el write " + this, e1);
                    }
                }
            }
        }
    }

    /**
	 * Establece la cabecera estandar de las peticiones
	 * 
	 * @param output
	 * @param action
	 * @param service
	 * @param id
	 * @throws IOException
	 */
    private void setHeader(String action, String service, long id) throws IOException {
        postOutput.write(Constants.PARAM_ACTION + "=" + action + "\n");
        postOutput.write(Constants.PARAM_SERVICE + "=" + service + "\n");
        postOutput.write(Constants.PARAM_ID + "=" + id + "\n");
        postOutput.flush();
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
	 * Establece el id del proxy. Se envia en las sucesivas peticiones al
	 * servidor
	 * 
	 * @param l
	 *            Id de la conexion.
	 */
    private void setId(long l) {
        id = l;
    }

    /**
	 * Envia una peticion de cerrar la conexion al servidor
	 */
    private void sendClose() {
        logger.debug("Enviando sendClose para sesion " + this);

        //Se pone id a Constants.NO_SESSION
        long id = this.getId();
        this.setId(Constants.NO_SESSION);

        BufferedReader reader = null;

        try {
            connect();

            //Se añaden la cabecera a la peticion
            setHeader(Constants.COMMAND_CLOSE, this.serviceManager.getService(), id);

            postOutput.flush();
            postOutput.close();

            //Get the response stream.
            reader = new BufferedReader(new InputStreamReader(post.getInputStream()));

        }
        catch (IOException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e);
            }
        }
        catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e);
            }
        }
        finally {

            //Liberar recursos
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e1) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(e1);
                    }
                }
            }

            //Desconecta
            disconnect();
        }
    }

    /**
	 * Envia una peticion al servidor para que abra una conexion con el host
	 * remoto
	 * 
	 * @return -1 si no se ha podido establecer la conexion con el host remoto
	 *         id de la conexion en otro caso.
	 */
    private long sendOpen() {
        BufferedReader reader = null;
        long connectionId = Constants.NO_SESSION;

        try {
            connect();

            //Se añaden datos la peticion
            setHeader(Constants.COMMAND_OPEN, this.serviceManager.getService(), getId());

            postOutput.flush();
            postOutput.close();

            // Get the response stream.
            reader = new BufferedReader(new InputStreamReader(post.getInputStream()));

            // If the post was successful, return the new id, otherwise
            // throw an exception.
            String result = reader.readLine();

            if (result != null && result.startsWith(Constants.COMMAND_OK)) {
                connectionId = Long.parseLong(reader.readLine());
                logger.debug("sendOpen OK");
            }
            else {
                throw new Exception("Unable to connect to remote host: " + result);
            }
        }
        catch (NumberFormatException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e);
            }
        }
        catch (IOException e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e);
            }
        }
        catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e);
            }
        }
        finally {
            //Liberar recursos
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (IOException e1) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(e1);
                    }
                }
            }

            disconnect();
        }

        return connectionId;
    }

    /**
	 * Obtiene una conexion con el servidor. Inicializa el output stream para
	 * poder escribir al servidor
	 * 
	 * @throws MalformedURLException
	 * @throws IOException
	 */
    private void connect() throws MalformedURLException, IOException {
        if (post == null) {
            //Obtiene una conexion
            post = connectionManager.getURLConnection();

            //Realiza la conexion
            post.connect();

            //Se obtiene stream para escribir
            postOutput = new OutputStreamWriter(post.getOutputStream());
        }
    }

    /**
	 * 
	 * Se desconecta la conexion con el servidor
	 */
    private void disconnect() {
        //Se cierra el output stream de salida abierto en el connect
        try {
            //Se cierra el stream de salida
            if (this.postOutput != null) {
                this.postOutput.close();
            }
        }
        catch (IOException e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Cerrando output del post ", e);
            }
        }
        this.postOutput = null;

        if (this.post != null) {
            //Se desconecta y se marca a null
            this.post.disconnect();
            this.post = null;
        }
    }
}