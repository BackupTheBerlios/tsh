package org.tsh.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tsh.remote.MsgHead;

import java.util.Date;

/**
 * Clase de Utilidades Fecha 22-abr-2004
 * 
 * @author jmgarcia
 *  
 */
public class Util {
   /** objeto de log */
   private static Log logger = LogFactory.getLog(Util.class);

   /**
    * Lee un buffer de un tamaño determinado de un inputStream
    * 
    * @param input Stream de entrada
    * @param buffer Resultado
    * @param tam Numero de bytes a leer
    * @return 0 si todo correcto -1 si EOF
    */
   public static int readBuffer(InputStream input, byte[] buffer, int tam,long timeout)
      throws IOException, SocketTimeoutException {
      int readed;
      long initTime = new Date().getTime();
      for (int i = 0; i < tam;) {
         try {
            readed = input.read(buffer, i, tam - i);
            initTime = new Date().getTime();
            i += readed;
            if (readed == -1) {
               tam = -1;
            }
         } catch (SocketTimeoutException ste) {
            if ( ( new Date().getTime() - initTime) >= timeout ) {
               	throw ste; 
            }            
         }
      }
      return tam;
   }

   /**
    * Lee de un inputStream la cabecera de un mensaje
    * 
    * @param input Entrada de donde leer
    * @param head Array donde almacenar la cabecera leida
    * @return -1 EOF, 0 ok
    */
   public static int readMsgHead(InputStream input, MsgHead head)
      throws SocketTimeoutException, IOException {
      byte[] buffer = new byte[Constants.MSG_HEAD_TAM];
      int result = Util.readBuffer(input, buffer, Constants.MSG_HEAD_TAM,Constants.READ_TIMEOUT);
      if (result != -1) {
         head.setMsg(buffer[0]);
         head.setData(((int) buffer[1] & 0xFF) + ((int) buffer[2] & 0xFF) * 256);
         logger.debug("Leida cabecera: " + head.getMsg() + "," + head.getData() + "->" +result);
      }
      return result;

   }

   /**
    * Escribe en un outputStream la cabecera de un mensaje. Se realiza un flush del OutputStream
    * 
    * @param stream Donde se escribe la cabecera
    * @param msg Tipo del mensaje
    * @param data Primer dato de la cabecera
    */
   public static void writeMsgHead(OutputStream output, MsgHead head) throws IOException {
      logger.debug ("Escribiendo cabecera: " + head.getMsg() + "," + head.getData());
      output.write(head.getMsg());
      output.write(head.getData() % 256);
      output.write(head.getData() / 256);
      output.flush();

   }

}
