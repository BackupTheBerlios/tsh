package server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Maneja las sesiones actuales del sistema Fecha 18-oct-2003
 * 
 * @author jmgarcia
 */
public class SessionManager {
    //Logger
    private static final Log logger = LogFactory.getLog(SessionManager.class);

    //Lista con las sesiones manejadas
    private Map sessions = Collections.synchronizedMap(new HashMap());

    //Lista de servicios
    private Map services = new HashMap();

    //Objeto
    private CheckSession checkSession = null;

    /**
	 * Crea un thread para "recolectar" las sesiones inactivas
	 */
    public SessionManager() {
        //Crea y arranca un thread para recolectar las sesiones inactivas
        checkSession = new CheckSession(this, sessions);
        new Thread(checkSession).start();
    }

    /**
	 * Establece la lista de servicios que se manejan. Los pasa a una tabla
	 * Hash.
	 * 
	 * @param services
	 *            Lista de servicios (Objetos ServiceServerInfo
	 */
    public void setServices(List services) {
        if (services != null) {
            Iterator it = services.iterator();

            while (it.hasNext()) {
                ServiceServerInfo ssi = (ServiceServerInfo) it.next();
                this.services.put(ssi.getName(), ssi);
            }
        }
    }

    /**
	 * A�ade una sesion
	 * 
	 * @param session
	 *            Sesion a a�adir
	 */
    public void add(Session session) {
        synchronized (sessions) {
            sessions.put(new Long(session.getId()), session);
            logger.debug("Session added: " + session);
        }
    }

    /**
	 * Crea una sesion para un determinado servicio
	 * 
	 * @param service
	 *            Servicio para el que se quiere la sesion
	 * 
	 * @return null si no se encuentra el servicio
	 */
    public Session createSession(String service) {
        ServiceServerInfo sinfo = (ServiceServerInfo) this.services.get(service);

        if (sinfo == null) {
            return null;
        }
        else {
            return new Session(sinfo);
        }
    }

    /**
	 * Busca una sesion por id
	 * 
	 * @param sessionId
	 *            id de la sesion
	 * 
	 * @return null si no la encuentra
	 */
    public Session get(long sessionId) {
        synchronized (sessions) {
            Session result = (Session) sessions.get(new Long(sessionId));
            if (result != null) {
                result.resetTime();
            }
            return result;
        }
    }

    /**
	 * Elimina una sesion del manager
	 * 
	 * @param sessionId
	 *            Identificador de la sesion
	 * 
	 * @return null si no la encuentra e.o.c Objeto sesion eliminado.
	 */
    public Session remove(long sessionId) {
        synchronized (sessions) {
            Session result = (Session) sessions.remove(new Long(sessionId));
            logger.debug("Session removed: " + result);

            return result;
        }
    }
}