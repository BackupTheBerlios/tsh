package org.tsh.remote;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;

import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.ResponseInputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tsh.client.ConnectionManager;
import org.tsh.common.Constants;

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

   /**
    *  
    */
   public RemoteHttpInputStream(String service, HttpConnection connection) {
      super();
      conn = connection;
      this.service = service;
   }

   /**
    * Obtiene un input stream
    * 
    * @return
    */
   private InputStream getInputStream() throws IOException {
      if (conn != null) {
         //Se obtiene stream para escribir
         return conn.getResponseInputStream();
      }
      return null;
   }

   /**
    *  
    */
   public void open() throws IOException, Exception {
      //Realiza la conexion
      if (conn != null) {
         //Abrir Conexion
         conn.open();

         //Escribir Request Header basicas
         ConnectionManager.writeRequestHeader(conn);

         //Content-length enorme para que no se corte
         conn.printLine("Content-length: 500000000");
         conn.printLine(); // close head

         //Se añaden parametros del protocolo tsh a la peticion
         conn.printLine(Constants.PARAM_ACTION + "=" + Constants.COMMAND_OPENR);
         conn.printLine(Constants.PARAM_SERVICE + "=" + service);
         conn.printLine(Constants.PARAM_ID + "=" + getId());
         conn.flushRequestOutputStream();

         ConnectionManager.readResponseHeaders(conn);

         // TODO comprobar chuncked

         //Esperando OK e id de la peticion
         ResponseInputStream input = new ResponseInputStream(getInputStream(), false, 5000000);
         reader = new BufferedReader(new InputStreamReader(input));
         String result = reader.readLine();
         if (result != null && result.startsWith(Constants.COMMAND_OK)) {
            setId(Long.parseLong(reader.readLine()));
            logger.debug("sendOpen OK");
         } else {
            throw new Exception("Unable to connect to remote host: " + result);
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
    * @param buffer
    * @return
    */
   public int readBuffer(byte[] buffer) throws IOException {
      int readed = -1, i = 0;
      int tam = -1, tam2 = -1, msg = -1;
      boolean fin = false;

      //Lee la cabecera     
      while (!fin) {
         try {
            msg = this.getInputStream().read();
            fin = true;
         } catch (SocketTimeoutException ste) {
            logger.debug("Time out de lectura cabecera del servidor");
         }
      }
      //En funcion de la cabecera realiza lo que corresponda
      switch (msg) {
         case -1 :
            //Se ha cerrado la conexion
            break;
         case Constants.MSG_CLOSE :
            //Cerra esta conexion (indicado por el cliente)
            break;
         case Constants.MSG_DATA :
            //Recibidos datos
            tam = this.getInputStream().read();
            tam2 = this.getInputStream().read();
            if (tam == -1 || tam2 == -1)
               break;
            tam += tam2 * 256;
            logger.debug("Recibiendo buffer de " + tam + "," + msg);
            for (i = 0; i < tam;) {
               try {
                  readed = this.getInputStream().read(buffer, i, tam - i);
                  i += readed;
                  if (readed == -1) {
                     tam = -1;
                  }
               } catch (SocketTimeoutException ste) {
                  logger.debug("Time out de lectura del servidor");
               }
            }
            logger.debug("Recibido buffer de " + i + " bytes");
            break;
         default :
            logger.warn("Error en protocolo leyendo datos del cliente " + msg);
            break;
      }
      return tam;

   }
}
