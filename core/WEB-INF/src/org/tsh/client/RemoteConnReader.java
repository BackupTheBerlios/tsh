package org.tsh.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tsh.common.Constants;
import org.tsh.common.IStat;
import org.tsh.remote.RemoteHttpOutputStream;

/**
 * Lee datos de un output stream remoto y los escribe al input stream local Fecha 15-mar-2004
 * 
 * @author jmgarcia
 *  
 */
public class RemoteConnReader implements Runnable {
   // Logger
   private static final Log logger = LogFactory.getLog(RemoteConnReader.class);

   private RemoteConn conn = null;
   private InputStream input = null;
   private RemoteHttpOutputStream output = null;
   private boolean stop = false;
   private IStat stat = null;

   /**
    * @param input Stream de lectura del cliente
    * @param output Stream de escritura en el servidor
    * @param conn Conexion asociada al reader
    */
   public RemoteConnReader(InputStream input, RemoteHttpOutputStream output, RemoteConn conn,IStat stat) {
      this.conn = conn;
      this.input = input;
      this.output = output;
      this.stat = stat;
   }

   /**
    * Bucle principal de lectura de un stream (del cliente) y escritura en el otro (del servidor)
    * 
    * @see java.lang.Runnable#run()
    */
   public void run() {
      int readed = 0;
      byte[] buffer = new byte[Constants.MAX_READ];
      try {
         while (readed >= 0 && !stop) {
            try {
               readed = input.read(buffer);
               if (readed > 0) {
                  logger.debug("Leidos del cliente " + readed + " bytes");
                  output.writeBuffer(readed, buffer);
                  logger.debug ("Escritos al servidor " + readed + " bytes" );
                  this.stat.addSend(readed);
               }
            } catch (SocketTimeoutException ste) {
               logger.debug("Timeout en lectura de socket de cliente");
            }
         }

      } catch (IOException e) {
         // TODO que hay que hacer
         logger.warn("Bucle de lectura del cliente", e);
      } finally {

         // TODO El cliente cierra la conexion (se termina la comunicación
         // desde el lado cliente)
         output.close();

         if (stop) {
            // TODO Termina limpiamente
            logger.debug("Stop lector del cliente");
         } else {
            // TODO cerrar
            logger.debug("Terminando lector del cliente");
            this.conn.finishReader();
         }

      }
   }

   /**
    * Se solicita que se termine el servicio
    *  
    */
   public void stop() {
      stop = true;
   }
}
