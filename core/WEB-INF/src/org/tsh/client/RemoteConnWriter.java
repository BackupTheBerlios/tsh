package org.tsh.client;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketTimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tsh.common.Constants;
import org.tsh.common.IStat;
import org.tsh.remote.RemoteHttpInputStream;

/**
 * * Lee datos de un output stream local y los escribe al input stream remoto
 * 
 * @author jmgarcia
 *  
 */
public class RemoteConnWriter implements Runnable {
   // Logger
   private static final Log logger = LogFactory.getLog(RemoteConnWriter.class);

   private RemoteConn conn = null;
   private RemoteHttpInputStream serverInput = null;
   private OutputStream output = null;
   private boolean stop = false;
   private IStat stat = null;

   /**
    * @param output Stream de escritura en el cliente
    * @param serverInput Stream de lectura del servidor
    * @param conn
    */
   public RemoteConnWriter(
      OutputStream output,
      RemoteHttpInputStream serverInput,
      RemoteConn conn,IStat stat) {
      this.serverInput = serverInput;
      this.output = output;
      this.conn = conn;
      this.stat = stat;
   }

   /**
    * Bucle principal de lectura de un stream (del servidor) y escritura en el otro (del cliente)
    * 
    * @see java.lang.Runnable#run()
    */
   public void run() {
      int readed = 0;
      byte[] buffer = new byte[Constants.MAX_READ];
      try {
         while (readed >= 0 && !stop)
            try {
               readed = serverInput.readBuffer(buffer);
               if (readed > 0) {
                  output.write(buffer, 0, readed);
                  logger.debug("Escritos al cliente " + readed + " bytes");
                  this.stat.addRecieve(readed);
                  output.flush();
               }
            } catch (SocketTimeoutException ste) {
               logger.debug("Timeout de lectura de stream de servidor");
            }
      } catch (IOException e) {
         logger.debug("Error en bucle de lectura del servidor", e);
      } finally {

         serverInput.close();

         if (stop) {
            // TODO se ha pedido que se termine
            logger.debug("Stop del escritor al cliente");
         } else {
            // TODO Terminar
            logger.debug("Terminado escritor al cliente");
            this.conn.finishWriter();
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
