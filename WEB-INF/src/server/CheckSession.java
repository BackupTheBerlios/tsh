package server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Recorre una lista de sesiones, cerrando las que no se hayan utilizado
 * ultimamente Fecha 09-nov-2003
 * 
 * @author jmgarcia
 */
public class CheckSession implements Runnable {
    // Periodo de activación del chequeo
    private static final long THREAD_SLEEP = 60000;

    // Logger
    private static final Log logger = LogFactory.getLog(CheckSession.class);

    // Sesiones
    private Map sessions = null;

    //Manejador de las sesiones
    private SessionManager manager = null;

    /**
	 * Constructor
	 * 
	 * @param manager
	 *            DOCUMENT ME!
	 * @param sessions
	 *            Map con las sesiones a controlar
	 */
    public CheckSession(SessionManager manager, Map sessions) {
        this.sessions = sessions;
        this.manager = manager;
    }

    /**
	 * Bucle principal
	 */
    public void run() {
        while (true) {
            try {
                Thread.sleep(THREAD_SLEEP);
            }
            catch (InterruptedException e) {
            }
            logger.debug("Inicio limpiado de sesiones inactivas");

            Long sessionId = null;
            List removed = new ArrayList();
            Iterator it = null;
            synchronized (sessions) {

                //Recorre las sesiones cerrando las inactivas
                it = sessions.keySet().iterator();

                while (it.hasNext()) {
                    sessionId = (Long) it.next();
                    Session session = (Session) sessions.get(sessionId);

                    if (session != null && session.inactive()) {
                        //Cerrar sesion
                        logger.info("Session inactiva. Cerrando: " + session);
                        session.close();
                        //Se añade la sesion a la lista de sesiones a borrar
                        removed.add(sessionId);
                    }
                }
            }
            //Recorrer la lista de sesiones a borrar
            it = removed.iterator();
            while (it.hasNext()) {
                sessionId = (Long) it.next();
                manager.remove(sessionId.longValue());
            }

        }
    }
}