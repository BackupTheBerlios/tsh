package org.tsh.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
public class RemoteHttpInputStream {
    /** objeto de log */
    private Log logger = LogFactory.getLog(RemoteHttpInputStream.class);
       
    private String service = null;
    
    private long id = Constants.NO_SESSION;
        
    private HttpConnection conn = null;
    private BufferedReader reader = null;
    
    /**
     * 
     */
    public RemoteHttpInputStream(String service,HttpConnection connection) {
        super();
        conn = connection; 
        this.service = service;
    }

    /**
     * @return
     */
    public InputStream getInputStream() throws IOException {
    	if ( conn != null ) {  
    		//Se obtiene stream para escribir
    		return conn.getResponseInputStream();
    	}
    	return null;
    }

    /**
     * 
     */
    public void open() throws IOException,Exception {
        //Realiza la conexion
        if ( conn != null ) {
        	//Abrir Conexion
        	conn.open();
        	
        	//Escribir Request Header basicas
        	ConnectionManager.writeRequestHeader(conn);
        	
        	//Content-length enorme para que no se corte
        	conn.printLine("Content-length: 500000000");
        	conn.printLine();// close head
        	        	
            //Se añaden parametros del protocolo tsh a la peticion        	
            conn.printLine(Constants.PARAM_ACTION + "=" + Constants.COMMAND_OPENR);
            conn.printLine(Constants.PARAM_SERVICE + "=" + service );
            conn.printLine(Constants.PARAM_ID + "=" + getId() );
            conn.flushRequestOutputStream();

            ConnectionManager.readResponseHeaders(conn);
            
            // TODO comprobar chuncked
            
            //Esperando OK e id de la peticion
            ResponseInputStream input = new ResponseInputStream(getInputStream(),false,5000000);
            reader = new BufferedReader(new InputStreamReader(input));            
            String result = reader.readLine();
            if (result != null && result.startsWith(Constants.COMMAND_OK)) {
                setId( Long.parseLong(reader.readLine()) );
                logger.debug("sendOpen OK");
            }
            else {
                throw new Exception("Unable to connect to remote host: " + result);
            }
                      
        }
    }

    

	/**
     * 
     */
    public void close()  {
        if ( conn != null ) { 
        	logger.debug("Cerrando HttpInputStream");
        	/*
        	try {
        	//	conn.shutdownOutput();
				conn.getRequestOutputStream().close();
				conn.getResponseInputStream().close();
			} catch (IllegalStateException e) {
				logger.debug ("Cerrando HttpInputStream",e);
			} catch (IOException e) {
				logger.debug ("Cerrando HttpInputStream",e);
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
    /**
     * @param id Nuevo id a establecer.
     */
    private void setId(long id) {
        this.id = id;
    }
}
