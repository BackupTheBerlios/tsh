package org.tsh.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tsh.client.ConnectionManager;
import org.tsh.common.Constants;
import org.tsh.common.Util;

/**
 * Gestiona una conexión Http para lectura del servidor remoto Fecha 15-mar-2004
 * 
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
   private InputStream input = null;
   private String serverURL = null;
   
   /**
    *  
    */
   public RemoteHttpInputStream(String service, HttpConnection connection, String serverURL) {
      super();
      this.conn = connection;
      this.service = service;
      this.serverURL = serverURL;
   }


   /**
    *  
    */
   public void open() throws IOException, Exception {
      //Realiza la conexion
      if (conn != null) {
         //Abrir Conexion
         conn.open();
         //Escribir cabecera HTTP
         ConnectionManager.writeRequestHeader(conn,serverURL);      
         
         //Content-length enorme para que no se corte
         conn.printLine("Content-length: " + Constants.CONTENT_LENGTH);
         conn.printLine(); // close head
         conn.flushRequestOutputStream();

         //Se añaden parametros del protocolo tsh a la peticion
         conn.printLine(Constants.PARAM_ACTION + "=" + Constants.COMMAND_OPENR);
         conn.printLine(Constants.PARAM_SERVICE + "=" + service);
         conn.printLine(Constants.PARAM_ID + "=" + getId());
         conn.flushRequestOutputStream();
         
         //Obtengo el inputStream de lectura de la conexion
         input = ConnectionManager.getInputStream(conn);
         
         //Leer cabecera del mensaje de respuesta
         MsgHead head = new MsgHead();
         int readed = Util.readMsgHead(input,head);
         
         //Comprobar codigo de respuesta y leer Id
         if (readed != -1 && head.getMsg() == Constants.MSG_OK) {
            setId (head.getData());
         	logger.debug("OpenR OK");
         } else {
        	   throw new Exception("Unable to connect to remote host: " + head.getMsg());
         }	
         
      }
   }

   /**
    * Cierra la conexión
    */
   public void close() {
      if (conn != null) {
         logger.debug("Cerrando HttpInputStream " + this);
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

   public String toString() {
      return "[" + this.getId() + "]";
   }

   /**
    * Lee un mensaje de datos del servidor
    * @param buffer Se almacenan los datos del mensaje
    * @return -1 EOF, ok e.o.c
    */
   public int readBuffer(byte[] buffer) throws IOException {
      int tam = -1,readed = -1;
      MsgHead head = new MsgHead();            
      
      //Lee la cabecera     
      readed = Util.readMsgHead(this.input,head);
      if ( readed == -1 ) return -1; //Leido EOF antes de leer la cabecera
      
      //En funcion de la cabecera realiza lo que corresponda
      switch (head.getMsg()) {
         case -1 :
            //Se ha cerrado la conexion
            logger.debug("Se ha cerrado la conexion del servidor");
            break;
         case Constants.MSG_CLOSE :
            //Cerra esta conexion (indicado por el cliente)
            logger.debug ("Recibido MSG_CLOSE del servidor");
            break;                 
         case Constants.MSG_DATA :         
            //Recibido mensaje de datos, calcular el tamaño de los datos del mensaje            
            tam = head.getData();
            logger.debug("Recibido MSG_DATA del servidor, recibiendo buffer de " + tam );
            try {
               readed = Util.readBuffer(this.input,buffer,tam);            
            } catch (SocketTimeoutException ste) {
               logger.debug("Time out de lectura del servidor");
            }            
            logger.debug("Recibido buffer de " + tam + " bytes");
            break;
         default :
            logger.warn("Error en protocolo leyendo datos del cliente " + buffer[0]);
            break;
      }
      
      return tam;

   }
}
