package org.tsh.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tsh.common.Constants;
import org.tsh.common.IStat;
import org.tsh.remote.RemoteHttpOutputStream;
import org.tsh.remote.RemoteHttpInputStream;


/**
 * Fecha 15-mar-2004
 * @author jmgarcia
 * 
 */
public class RemoteConn {
    /** objeto de log */
    private Log logger = LogFactory.getLog(RemoteConn.class);
    
    // Stream de lectura cliente
    private InputStream input;
    // Stream de escritura cliente
    private OutputStream output;
    
    // Stream de escritura servidor
    private RemoteHttpOutputStream outputServidor;    
    // Stream de lectura servidor
    private RemoteHttpInputStream inputServidor;
   
    //Servicio al que esta conectado esta conexion
    private String service = null;       

    //Manejador de conexiones
    private ConnectionManager conn ;

    private RemoteConnReader rcr ;
    private RemoteConnWriter rcw ;

	private IStat stat;
	private ProxyHost proxy;
    
    /**
     * @param stream
     * @param stream2
     */
    public RemoteConn(InputStream input, OutputStream output,ConnectionManager conn,String service, IStat stat, ProxyHost proxy) {        
        this.input = input;
        this.output = output;        
        this.conn  = conn;
        this.service = service;
        this.stat = stat;
        this.proxy = proxy; 
    }

    /**
     * 
     */
    public void run() {             
                        
        try {
        	//Abrir RemoteHttpInputStream
            this.inputServidor = new RemoteHttpInputStream(service,conn.getHttpConnection());
            this.inputServidor.open();        
        
            //Obtener id de la sesion
            long id = inputServidor.getId();
            
            if ( id == Constants.NO_SESSION ) {
                //No se ha podido establece la conexion    
                logger.warn("No se ha podido establecer conexion con servicio: " + service);    
                this.close();                
                return;
            }
            
            this.proxy.setId(id);
            
            //Abrir RemoteHttpOutputStream
            outputServidor = new RemoteHttpOutputStream(id,service,conn.getHttpConnection());
            outputServidor.open();
        
        }
        catch (IOException e1) {
            logger.warn("Error estableciendo conexion con el servidor",e1);
            this.close();
            return;
        }
        catch (Exception e1) {
            logger.warn("Error estableciendo conexion con el servidor",e1);
            this.close();
            return;
        }
        
        
        try {
	        //Crear lector y escritor
	        rcr = new RemoteConnReader(input,outputServidor,this,this.stat);
	        rcw = new RemoteConnWriter(output,inputServidor,this,this.stat);
	        
	        //Arrancar thread lector
	        Thread t1 = new Thread (rcr);
	        t1.start();
	        
	        //El escritor se queda en este thread
	        rcw.run();	        
	             
            //Espero hasta que se cierren las conexiones (terminen los threads)
            t1.join();
            
        }
        catch (InterruptedException e) {
            logger.warn ("Error en thread de conexion",e);
        }finally {
            this.close();
        }
 
    }

    /**
     * Se cierran las conexiones remotas para lectura y escritura
     */
    private void close() {        
        // Cerrar conexion remota de lectura
        if ( inputServidor != null ) {
            inputServidor.close();
        }
        // Cerrar conexion remota de escritura
        if ( outputServidor != null) {
            outputServidor.close();
        }
    }
    
    void finishReader() {
    	//TODO implementar
    	this.rcw.stop();
    }
    
    void finishWriter() {
    	// TODO implementar
    	this.rcr.stop();
    }
    
    void readerTimeout(RemoteConnReader rcr) {
    	if ( rcr == this.rcr) {
    	}    
    }
    
    void writerTimeout(RemoteConnWriter rcw) {
    	if ( rcw == this.rcw){
    		
    	}
    }
    
    
}
