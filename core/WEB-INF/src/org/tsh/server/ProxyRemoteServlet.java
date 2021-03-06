package org.tsh.server;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.digester.Digester;
import org.apache.commons.digester.xmlrules.DigesterLoader;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tsh.common.Constants;
import org.tsh.common.Util;
import org.tsh.remote.MsgHead;
import org.xml.sax.SAXException;

/**
 * Servlet para manejar las peticiones de conexion del cliente con un host remoto Controla todo el
 * flujo del protocolo de comunicacion Fecha 18-oct-2003
 * 
 * @author jmgarcia
 */
public class ProxyRemoteServlet extends HttpServlet {
   //Nombre del parametro de configuracion con el fichero de configuracion
   private static final String PARAM_CONFIGFILE = "configFile";

   //Nombre del parametro de configuracion con el fichero de reglas del
   // fichero de configuracion
   private static final String PARAM_RULESCONFIGFILE = "rulesConfigFile";

   // Controlador de las sesiones
   private static final SessionManager sessionManager = new SessionManager();

   // Logger
   private static final Log logger = LogFactory.getLog(ProxyRemoteServlet.class);

   /**
    * Init del server. Se leen los ficheros de configuración del web.xml y se obtiene la
    * configuración del fichero
    * 
    * @see javax.servlet.GenericServlet#init()
    */
   public void init() throws ServletException {
      super.init();

      logger.info("Iniciando servlet de servicio");

      String configFile = this.getInitParameter(PARAM_CONFIGFILE);
      String rulesConfigFile = this.getInitParameter(PARAM_RULESCONFIGFILE);

      //Leer configuracion del servidor
      File inputXMLFile = new File(this.getServletContext().getRealPath(configFile));
      File rules = new File(this.getServletContext().getRealPath(rulesConfigFile));

      try {
         Digester digester = DigesterLoader.createDigester(rules.toURL());
         List services = (List) digester.parse(inputXMLFile);
         sessionManager.setServices(services);

         logger.info("Iniciado servlet de servicio");
      } catch (MalformedURLException e) {
         logger.error(e);
      } catch (IOException e) {
         logger.error(e);
      } catch (SAXException e) {
         logger.error(e);
      }
   }

   protected void service(HttpServletRequest arg0, HttpServletResponse arg1)
      throws ServletException, IOException {
      doPost(arg0, arg1);
   }

   /**
    * Manejador de peticiones Post realizadas por el cliente.
    * 
    * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
    *      javax.servlet.http.HttpServletResponse)
    */
   protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
      //Obtener streams de lectura y escritura
      ServletInputStream input = request.getInputStream();
      ServletOutputStream output = response.getOutputStream();
      //Leer Cabecera de la peticion
      String action = getParam(input, Constants.PARAM_ACTION);
      String service = getParam(input, Constants.PARAM_SERVICE);
      String strId = getParam(input, Constants.PARAM_ID);
      long id = Long.parseLong(strId);

      //Procesar peticion
      if (logger.isInfoEnabled()) {
         String remoteHost = request.getRemoteHost();
         logger.info(
            "Request from ["
               + remoteHost
               + "]: service="
               + service
               + ",action="
               + action
               + ",id="
               + id);
      }
      try {
         //Abrir la conexion para leer
         if ((action != null) && action.equals(Constants.COMMAND_OPENR)) {
            doOpenRead(id, service, output);
         }
         //Abrir la conexion para escribir
         else if ((action != null) && action.equals(Constants.COMMAND_OPENW)) {
            doOpenWrite(id, service, input, output);
         }
         //Comando no soportado
         else {
            logger.warn("Command not defined: " + action + ",service=" + service + ",id=" + id);
         }
      } catch (IOException e1) {
         logger.warn("Procesando peticion", e1);
      } finally {
         try {
            input.close();
            output.close();
         } catch (IOException e) {
            logger.debug("Cerrando streams en post", e);
         }
      }
   }

   /**
    * Lee un parametro de la cabecera del protocolo tsh
    * 
    * @param stream Stream de lectura
    * @param param Parametro a leer
    * @return valor del parametro
    */
   private String getParam(ServletInputStream stream, String param) {
      String value = "";

      try {
         byte[] bline = new byte[1024];
         stream.readLine(bline, 0, 1023);
         String line = new String(bline);
         if (line.startsWith(param + "=")) {
            int fin = line.indexOf("\r\n");
            int ini = line.indexOf("=");
            return line.substring(ini + 1, fin);
         }
      } catch (IOException e) {
         if (logger.isWarnEnabled()) {
            logger.warn("Error leyendo parametro del post", e);
         }
      }

      return value;
   }

   /**
    * Abre una conexion para escribir en el servidor
    * 
    * @param service
    * @param response
    */
   private void doOpenWrite(
      long sessionId,
      String service,
      ServletInputStream input,
      ServletOutputStream out)
      throws IOException {
      logger.debug("Entro en doOpenWrite");
      Session session = sessionManager.get(sessionId);

      try {
         //No existe la session
         if (session == null) {
            logger.warn(
               "Recibido una peticion de open write para una conexion cerrada " + sessionId);
            Util.writeMsgHead(out, new MsgHead(Constants.MSG_KO, 0));
            return;
         }
         //Envio COMMAND_OK al cliente
         Util.writeMsgHead(out, new MsgHead(Constants.MSG_OK, session.getId()));
         logger.debug("Enviado comando OK por OpenWrite");
         //Leer datos y enviarlos al servidor
         session.setRequestWriter(input);
      } catch (Throwable e) {
         logger.warn("Procesando petición de abrir conexion de escritura", e);
         Util.writeMsgHead(out, new MsgHead(Constants.MSG_KO, 0));
      }
   }

   /**
    * Abre una conexion para Leer del servidor
    * 
    * @param service
    * @param response
    */
   private void doOpenRead(long id, String service, ServletOutputStream out) throws IOException {
      logger.debug("Entro en doOpenRead");
      try {
         Session session = null;
         if (id == Constants.NO_SESSION) {
            session = sessionManager.createSession(service);
         } else {
            session = sessionManager.get(id);
         }

         if (session == null) {
            logger.info("Servicio no encontrado " + service);
            Util.writeMsgHead(out, new MsgHead(Constants.MSG_KO, 0));
         } else {
            if (id == Constants.NO_SESSION) {
               session.open();
               sessionManager.add(session);
            }
            //Enviar id de sesion al cliente
            Util.writeMsgHead(out, new MsgHead(Constants.MSG_OK, session.getId()));
            logger.debug("Enviado comando OK por OpenRead");
            //Tratar el response
            session.setResponseWriter(out);
         }
      } catch (Throwable e) {
         logger.warn("Procesando petición de abrir conexion de lectura ", e);
         Util.writeMsgHead(out, new MsgHead(Constants.MSG_KO, 0));
      }
   }
}