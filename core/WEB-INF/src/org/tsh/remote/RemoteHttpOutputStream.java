package org.tsh.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.ResponseInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tsh.client.ConnectionManager;
import org.tsh.common.Constants;


/**
 * Fecha 15-mar-2004
 * @author jmgarcia
 * 
 */
public class RemoteHttpOutputStream {
    /** objeto de log */
    private Log logger = LogFactory.getLog(RemoteHttpOutputStream.class);
    
    /** Objeto con la conexion para realizar el post */
    private HttpURLConnection post = null;
    
    private long id = Constants.NO_SESSION;
    
    private String service = null;
    
    private HttpConnection conn = null;
    
    private BufferedReader reader = null;
    
    /**
     * 
     */
    public RemoteHttpOutputStream(long id,String service,HttpConnection connection) {
        super();           
        //Obtiene una conexion
        conn = connection;
        this.id = id;
        this.service = service;
    }

    /**
     * @return
     */
    public OutputStream getOutputStream() throws IOException {
        if ( conn != null ) {  
            //Se obtiene stream para escribir
            return conn.getRequestOutputStream();
        }
        return null;
        
    }

    /**
     * Abre una conexion de Lectura desde el servidor remoto
     */
    public void open() throws Exception {
        //Realiza la conexion
        if ( conn != null ) {
        	conn.open();
        	ConnectionManager.writeRequestHeader(conn);
        	conn.printLine("Content-length: 500000000");     
        	conn.printLine();
            
        	//Se añaden datos del protocol tsh a la peticion                                    
            conn.printLine(Constants.PARAM_ACTION + "=" + Constants.COMMAND_OPENW); 
            conn.printLine(Constants.PARAM_SERVICE + "=" + service );
            conn.printLine(Constants.PARAM_ID + "=" + getId() );
            conn.flushRequestOutputStream();                       
                     
            ConnectionManager.readResponseHeaders(conn);
            
            // TODO comprobar chunked 
            
            //Comprobar que se ha conectado correctamente
            ResponseInputStream input = new ResponseInputStream(conn.getResponseInputStream(),false,5000000);
            reader = new BufferedReader(new InputStreamReader(input));            
            String result = reader.readLine();
            if (result != null && result.startsWith(Constants.COMMAND_OK)) {                
                logger.debug("OpenW OK");
            }
            else {
                throw new Exception("Unable to connect to remote host: " + result);
            }
            
        }                       
    }

    /**
     * Cierra la conexion, libera recursos
     */
    public void close() {
        if ( conn != null ) { 
        	logger.debug("Cerrando HttpOutputStream");
        	/*
        	try {
				conn.getRequestOutputStream().close();
				conn.getResponseInputStream().close();
			} catch (IllegalStateException e) {
				logger.debug("Cerrando HttpOutputStream",e);
			} catch (IOException e) {
				logger.debug("Cerrando HttpOutputStream",e);
			}
        	*/
            conn.close();                            
            conn.releaseConnection();
            conn = null;
        }
    }
    
    /**
     * @return Devuelve el id de la conexion.
     */
    public long getId() {
        return this.id;
    }


}
