package org.tsh.server;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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
import org.xml.sax.SAXException;

/**
 * Servlet para manejar las peticiones de conexion del cliente con un host
 * remoto Controla todo el flujo del protocolo de comunicacion Fecha
 * 18-oct-2003
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
	 * Init
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
        }
        catch (MalformedURLException e) {
            logger.error(e);
        }
        catch (IOException e) {
            logger.error(e);
        }
        catch (SAXException e) {
            logger.error(e);
        }
    }

        
    protected void service(HttpServletRequest arg0, HttpServletResponse arg1)
            throws ServletException, IOException {        
        doPost(arg0,arg1);        
    }
    
            
    /**
	 * Manejador de peticiones Post realizadas por el cliente.
	 * 
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

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
            logger.info("Request from [" + remoteHost + "]: service=" + service + ",action="
                    + action + ",id=" + id);
        }
        if ((action != null) && action.equals(Constants.COMMAND_OPENR)) {
            doOpenRead(service,output);
        }
        //Abrir la conexion para escribir
        else if ((action != null) && action.equals(Constants.COMMAND_OPENW)) {
            doOpenWrite(id, service, input, output);
        }        
        //Comando no soportado
        else {
            logger.warn("Command not defined: " + action + ",service=" + service + ",id=" + id);
        }
        try {
        	input.close();
        	output.close();
        }catch (IOException e){
        	logger.debug ("Cerrando streams en post",e);
        }
    }

    /**
     * Lee un parametro de la cabecera del protocolo tsh
	 * @param stream
	 * @param string
	 * @return
	 */
	private String getParam(ServletInputStream stream, String param) {
		String value = "";

		try {
			byte[] bline = new byte[1024];
			stream.readLine(bline,0,1023);
			String line= new String(bline);
			if (line.startsWith(param + "=")) {
				int fin = line.indexOf("\r\n");
				int ini = line.indexOf("=");
				return line.substring(ini + 1,fin);
			}
		}
		catch (IOException e) {
			if (logger.isWarnEnabled()) {
				logger.warn("Error leyendo parametro del post", e);
			}
		}

		return value;
	}


	/**
	 * Abre una conexion para escribir en el servidor
	 * @param service
	 * @param response
	 */
    private void doOpenWrite(long sessionId, String service, ServletInputStream input,
					 ServletOutputStream out) {
        // TODO Auto-generated method stub
        Session session = sessionManager.get(sessionId);
        
        try {

            //No existe la session
            if (session == null) {
                logger.warn("Recibido una peticion de open write para una conexion cerrada "
                        + sessionId);                
                out.println(Constants.COMMAND_KO);
                out.flush();
                return;
            }
            //Envio COMMAND_OK al cliente            
            out.println(Constants.COMMAND_OK);            
            out.flush();
            
            //Leer datos y enviarlos al servidor
            session.setRequestWriter(input);                      
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Abre una conexion para Leer del servidor
	 * @param service
	 * @param response
	 */
    private void doOpenRead(String service,ServletOutputStream out)
            throws IOException {    	        
        try {
            Session session = sessionManager.createSession(service);

            if (session == null) {
                logger.info("Servicio no encontrado " + service);
                out.print(Constants.COMMAND_KO);
            }
            else {            	
                session.open();
                sessionManager.add(session);
                //Enviar id de sesion al cliente                
                out.println(Constants.COMMAND_OK);
                out.println(session.getId());
                out.flush();
                //Tratar el response
                session.setResponseWriter(out);
                out.close();                
            }
        }
        catch (Throwable e) {

            logger.warn("Error creando sesion ", e);
            out.print(Constants.COMMAND_KO);
        }
    }

    /**
	 * Cierra una sesion
	 * 
	 * @param sessionId
	 */
    private void doClose(long sessionId) {
        Session session = sessionManager.remove(sessionId);

        if (session == null) {
            logger.warn("Recibido una peticion de close para una conexion cerrada " + sessionId);
        }
        else {
            if (logger.isDebugEnabled()) {
                logger.debug("Cerrando sesion " + session);
            }
            session.close();
            if (logger.isInfoEnabled()) {
                logger.info("Cerranda sesion " + session);
            }
        }
    }

    /**
	 * Abre una nueva conexion con un determinado servicio
	 * 
	 * @param request
	 *            Objeto request de la peticion http. Se utiliza para obtener
	 *            el servicio
	 * @param response
	 *            Objeto response de la peticion http. Se devuelve el id de la
	 *            conexion
	 * 
	 * @throws IOException
	 *             DOCUMENT ME!
	 */
    /*
    private void doOpen(String service, HttpServletResponse response) throws IOException {
        PrintWriter out = new PrintWriter(response.getWriter());

        try {
            Session session = sessionManager.createSession(service);

            if (session == null) {
                logger.info("Servicio no encontrado " + service);
                out.print(Constants.COMMAND_KO);
            }
            else {
                session.open();
                sessionManager.add(session);
                out.println(Constants.COMMAND_OK);
                out.println(session.getId());
            }
        }
        catch (Throwable e) {
            logger.warn("Error creando sesion ", e);
            out.print(Constants.COMMAND_KO);
        }
    }
*/
    /**
	 * Maneja una peticion de leer datos del servidor
	 * 
	 * @param sessionId
	 *            Identificador de la sesion
	 * @param response
	 * 
	 * @throws IOException
	 *             DOCUMENT ME!
	 */
    /*
    private void doRead(long sessionId, HttpServletResponse response) throws IOException {
        Session session = sessionManager.get(sessionId);

        //No existe la session
        if (session == null) {
            logger.warn("Recibido una peticion de read para una conexion cerrada " + sessionId);

            PrintWriter out = new PrintWriter(response.getWriter());
            out.print(Constants.COMMAND_KO);
        }
        else {
            writeData(session, response);
        }
    }
*/
    /**
	 * Ejecuta un comando write. Escribe datos en el servidor y envia la
	 * respuesta al cliente
	 * 
	 * @param sessionId
	 *            identificador de la sesion
	 * @param request
	 * @param response
	 *            DOCUMENT ME!
	 * 
	 * @throws IOException
	 *             DOCUMENT ME!
	 */
    /*
    private void doWrite(long sessionId, InputStream input, HttpServletResponse response)
            throws IOException {
        Session session = sessionManager.get(sessionId);

        //No existe la session
        if (session == null) {
            logger.warn("Recibido una peticion de write para una conexion cerrada " + sessionId);
            PrintWriter out = new PrintWriter(response.getWriter());
            out.print(Constants.COMMAND_KO);
            return;
        }

        //Leer datos y enviarlos al servidor
        int data = -1;
        OutputStream output = session.getOutputStream();
        while ((data = input.read()) >= 0) {
            //Se indica que se ha accedido a la session
            session.resetTime();
            output.write(data);
        }
        output.flush();

        //Escribir respuesta del servidor al response
        writeData(session, response);

    }
*/
    /**
	 * Lee datos del servidor remoto y los escribe en el response
	 * 
	 * @param session
	 *            Session
	 * @param response
	 *            response
	 * 
	 * @throws IOException
	 *             DOCUMENT ME!
	 */
    /*
    private void writeData(Session session, HttpServletResponse response) throws IOException {
        InputStream input = session.getInputStream();
        OutputStream output = response.getOutputStream();
        int read = 0;
        int available = -1;
        int readed = 0;

        //Leer datos del servidor
        while (((available = input.available()) > 0) && (read = input.read()) >= 0) {
            readed++;
            output.write(read);
        }
        if (read < 0 || available < 0) {
            session.close();
        }

        //Log
        if (readed > 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("Leidos " + readed + " bytes por sesion " + session);
            }
        }
        else if (available == 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("Leidos 0 bytes por sesion " + session);
            }
        }
    }
    */
}