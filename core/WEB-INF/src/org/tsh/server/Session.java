package org.tsh.server;

import org.tsh.common.Constants;
import org.tsh.common.Util;
import org.tsh.remote.MsgHead;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import java.util.Date;
import java.util.Random;

/**
 * Clase que mantiene la informacion necesaria de una sesion de conexion con un host remoto Fecha
 * 18-oct-2003
 * 
 * @author jmgarcia
 */
public class Session {
   //Logger
   private static Log logger = LogFactory.getLog(Session.class);

   //Contador para generar los identificadores de sesion
   private static long contador = 0;

   //Generador de numeros de sesion
   private static final Random generador = new Random();

   //Identificador de sesion
   private int idSesion = -1;

   //Servicio
   private String service = null;

   //Socket de conexion con el servidor remoto
   private Socket socket = null;

   //InputStream del socket
   private InputStream input = null;

   //OutputStream del socket
   private OutputStream output = null;

   //Maquina
   private String host = null;

   //puerto
   private int port = -1;

   // Objeto utilizado para sincronizacion de acceso al socket
   private final Object lock = new Object();

   //Time que indica el ultimo acceso que se hizo a esta sesion
   private long lastAccessTime;

   //Timeout de la sesion
   private long timeout;

   //Indica si hay que reconectar
   private boolean reopen = false;

   /**
    * Constructor de sesion
    * 
    * @param sinfo Contiene la informacion de servicio que se proporciona por esta session
    */
   public Session(ServiceServerInfo sinfo) {
      contador++; //Se incrementa el numero de sesiones activas
      this.idSesion = generador.nextInt(16384);

      this.service = sinfo.getName();
      this.host = sinfo.getHost();
      this.port = sinfo.getPort();
      this.timeout = sinfo.getSessionTimeout();
      this.resetTime();
   }

   /**
    * Devuelve el id de la sesion
    * 
    * @return Id de la sesion
    */
   public int getId() {
      return this.idSesion;
   }

   /**
    * Getter
    * 
    * @return Devuelve el inputStream para leer datos del servidor remoto de esta sesion
    */
   public InputStream getInputStream() {
      return input;
   }

   /**
    * Getter
    * 
    * @return Devuelve el outputStream para escribir datos en el servidor remoto de esta sesion
    */
   public OutputStream getOutputStream() {
      return output;
   }

   /**
    * Cierra una sesion. Libera los recursos del socket
    */
   public void close() {
      if (this.socket != null) {
         try {
            synchronized (this.lock) {
               logger.info("Cerrando sesion " + this);
               /*
			    * if (this.input != null) { try { input.close(); } catch (IOException e) {
			    * logger.debug("Error cerrando input " + this, e); } }
			    */
               this.input = null;
               /*
			    * if (this.output != null) { try { output.close(); } catch (IOException e) {
			    * logger.debug("Error cerrando output " + this, e); } }
			    */
               this.output = null;
               if (this.socket != null && !this.socket.isClosed()) {
                  this.socket.close();
               }
               this.socket = null;

            }
         } catch (IOException e) {
            if (logger.isDebugEnabled()) {
               logger.debug("Error cerrando sesion " + this, e);
            }
         }
      }
   }

   /**
    * Devuelve true si la sesion ha estado inactiva por mas tiempo del configurado
    * 
    * @return true si la sesion esta inactiva
    */
   public boolean inactive() {
      return (new Date().getTime() - this.lastAccessTime) >= this.timeout;
   }

   /**
    * Inicia una sesion, abre una conexion con el servidor remoto
    * 
    * @throws UnknownHostException
    * @throws IOException
    */
   public void open() throws UnknownHostException, IOException {
      this.socket = new Socket(InetAddress.getByName(this.host), this.port);
      this.socket.setSoTimeout(Constants.READ_TIMEOUT);
      this.input = this.socket.getInputStream();
      this.output = this.socket.getOutputStream();
      logger.info("Abriendo conexion con " + this.host + ":" + this.port);
   }

   /**
    * Resetea el tiempo de acceso
    */
   public void resetTime() {
      this.lastAccessTime = new Date().getTime();
   }

   /**
    * Metodo toString
    * 
    * @return DOCUMENT ME!
    */
   public String toString() {
      return "Sesion[" + this.idSesion + "," + this.service + "]";
   }

   /**
    * @param out
    */
   public void setResponseWriter(OutputStream out) {

      logger.debug("Entro en setResponseWriter");

      try {
         int read = 0;
         int readed = 0;
         byte[] buffer = new byte[Constants.MAX_READ];
         //Leer datos del servidor

         while (!this.reopen && read >= 0) {
            try {
               logger.debug("Esperando a leer del servidor para enviar el buffer al cliente");
               read = input.read(buffer);
               if (read >= 0) {
                  resetTime();

                  logger.debug("Enviando al cliente bytes " + read);
                  readed += read;

                  Util.writeMsgHead(out, new MsgHead(Constants.MSG_DATA, read));
                  out.write(buffer, 0, read);
                  out.flush();
               }
            } catch (SocketTimeoutException ste) {
            }            
         }
      } catch (IOException e) {
         if (this.input != null) {
            logger.warn("Bucle de lectura", e);
         }
      }

      try {
         if (this.reopen) {
            resetTime();
            Util.writeMsgHead(out, new MsgHead(Constants.MSG_REOPEN, 0));
         } else {
            Util.writeMsgHead(out, new MsgHead(Constants.MSG_CLOSE, 0));
         }
         out.flush();
      } catch (IOException e1) {         
         logger.warn("Enviando comando al cliente ", e1);
      }

      logger.debug("Termina el metodo setResponseWriter");
      if (!this.reopen) {
         this.close();
      } else {
         logger.debug("Enviado MSG_REOPEN");
         this.reopen = false;
      }
   }

   /**
    * @param out
    * @param reader
    */
   public void setRequestWriter(InputStream input) {
      //Leer datos y enviarlos al servidor
      int data = -1, tam = -1;
      boolean fin = false;
      MsgHead head = new MsgHead();
      logger.debug("Entro en setRequestWriter");
      try {
         while (!fin) {
            //Leer cabecera del mensaje
            logger.debug("Leyendo cabecera de buffer del cliente");
            if (Util.readMsgHead(input, head) == -1)
               break;

            switch (head.getMsg()) {
               case -1 :
                  //Se ha cerrado la conexion
                  fin = true;
                  break;
               case Constants.MSG_CLOSE :
                  //Cerra esta conexion (indicado por el cliente)
                  fin = true;
                  break;
               case Constants.MSG_REOPEN :
                  //Cerrar esta conexion para reabrirla
                  this.reopen = true;
                  fin = true;
                  break;
               case Constants.MSG_DATA :
                  //Recibido mensaje de datos
                  tam = head.getData();
                  logger.debug("Recibiendo buffer de " + tam);

                  for (int i = 0; i < tam && !fin;) {
                     try {
                        data = input.read();
                        i++;
                        resetTime();
                        if (data == -1) {
                           fin = true;
                        } else {
                           output.write(data);
                           output.flush();
                        }
                     } catch (SocketTimeoutException ste) {
                        logger.debug("Time out de lectura del cliente");
                     }
                  }
                  break;
               default :
                  logger.warn("Error en protocolo leyendo datos del cliente");
                  break;
            }

         }
      } catch (IOException e) {
         if (this.output != null) {
            logger.warn("Bucle de escritura", e);
         }
      }
      logger.debug("Termina el metodo setRequestWriter");
      if (Constants.MSG_REOPEN != head.getMsg()) {
         this.close();
      }

   }
}