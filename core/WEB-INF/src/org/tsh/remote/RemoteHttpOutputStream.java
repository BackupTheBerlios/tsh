package org.tsh.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.Date;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tsh.client.ConnectionManager;
import org.tsh.common.Constants;
import org.tsh.common.Util;

/**
 * Gestiona una conexión para escritura del servidor remoto Fecha 15-mar-2004
 * 
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
   private String serverURL = null;

   private long lastAccess;

   private long maxConnTime;

   /**
    *  
    */
   public RemoteHttpOutputStream(long id, String service, HttpConnection connection, String serverURL,long maxConnTime) {
      super();
      //Obtiene una conexion
      conn = connection;
      this.id = id;
      this.service = service;
      this.serverURL = serverURL;  
      this.maxConnTime = maxConnTime;
   }

   /**
    * @return
    */
   private OutputStream getOutputStream() throws IOException {
      if (conn != null) {
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
      if (conn != null) {
         lastAccess = new Date().getTime();
         conn.open();
         ConnectionManager.writeRequestHeader(conn,serverURL);
         conn.printLine("Content-length: " + Constants.CONTENT_LENGTH);
         conn.printLine();
         conn.flushRequestOutputStream();
         
         //Se añaden datos del protocol tsh a la peticion
         conn.printLine(Constants.PARAM_ACTION + "=" + Constants.COMMAND_OPENW);
         conn.printLine(Constants.PARAM_SERVICE + "=" + service);
         conn.printLine(Constants.PARAM_ID + "=" + getId());
         conn.flushRequestOutputStream();
         
         //Obtener inputstream de lectura del response de la conexion
         InputStream input = ConnectionManager.getInputStream(conn);
         
         //Leer cabecera del mensaje de respuesta
         MsgHead head = new MsgHead();
         int readed = Util.readMsgHead(input,head);
                  
         //Comprobar codigo de retorno                
         if ( readed != -1 && head.getMsg() == Constants.MSG_OK) {
            int id = head.getData();
            logger.debug("OpenW OK, id" + id);
         } else {
            throw new Exception("Unable to connect to remote host: " + head.getMsg());
         }         
      }
   }

   /**
    * Reabre una conexion de Lectura desde el servidor remoto
    */
   public void reOpen() throws Exception {
      //Realiza la conexion
      if (conn != null) {
         if  (( new Date().getTime() - lastAccess) >= maxConnTime ) {
            logger.debug("Realizando refresco de conexion de escritura en servidor");
	         try {
	            //Se envia mensaje de cerrar la conexion
	            Util.writeMsgHead(this.getOutputStream(),new MsgHead(Constants.MSG_REOPEN,0));            
	            this.getOutputStream().flush();
	         } catch (IOException e) {
	            logger.warn("Enviando mensaje de cerrar conexion de escritura en servidor para reabrirlo", e);
	         }         
	         conn.close();         
	         open();
         }
      }
   }
   
   /**
    * Cierra la conexion, libera recursos
    */
   public void close() {
      if (conn != null) {
         try {
            //Se envia mensaje de cerrar la conexion
            Util.writeMsgHead(this.getOutputStream(),new MsgHead(Constants.MSG_CLOSE,0));            
            this.getOutputStream().flush();
         } catch (IOException e) {
            logger.warn("Enviando mensaje de cerrar conexion de escritura en servidor", e);
         }
         logger.debug("Cerrando HttpOutputStream");
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
    * Escribe 
    * @param tam
    * @param buffer
    */
   public void writeBuffer(int tam, byte[] buffer) throws IOException {       
      //Escribir cabecera del mensaje
      Util.writeMsgHead(this.getOutputStream(),new MsgHead(Constants.MSG_DATA,tam));            
      //Escribir buffer
      this.getOutputStream().write(buffer, 0, tam);
      this.getOutputStream().flush();
     
   }

}
