package org.tsh.common;

/**
 * Fecha 09-oct-2003
 * 
 * @author jmgarcia
 */
public final class Constants {

    //Id para decir que no hay sesion
    public static final int NO_SESSION = -1;

    //Numero de conexiones maximas por servicio(valor por defecto)
    public static final int MAXCONNECTIONS_CLIENT = 10;

    //Numero de timeouts antes de enviar una peticion de read.
    public static final int MAXTIMEOUTS = 5;

    /** DOCUMENT ME! */
    public static final int BUFFER = 200000;

    /** DOCUMENT ME! */
    public static final int READ_TIMEOUT = 400;

    /** Maximo numero de bytes leido del servidor remoto para enviar al cliente */
    public static final int SERVER_MAX_READED = 1000000;

    /** Maximo numero de bytes leidos del cliente remoto para enviar al servidor */
    public static final int CLIENT_MAX_READED = 1000000;

    /** DOCUMENT ME! */
    public static final String CHARSET = "UTF-8";

    /** Parametro action de una conexion */
    public static final String PARAM_ACTION = "action";

    /** Parametro id de la conexion */
    public static final String PARAM_ID = "id";

    /** Parametro servicio de la peticion */
    public static final String PARAM_SERVICE = "service";
    

    /** Parametro para el envio de los datos */
    public static final String PARAM_DATA = "d";

    /** Parametro que indica el tama�o de los datos enviados */
    public static final String PARAM_DATALENGTH = "dl";

    /** Parametro con el siguiente numero de secuencia en el protocolo */
    public static final String PARAM_NEXTSEQ = "ns";

    //Comandos del protocolo
    /** Comando para abrir una conexion con el servidor */
    public static final String COMMAND_OPEN = "o";
    
    /** Comando para abrir una conexion de escritura con el servidor */
    public static final String  COMMAND_OPENW = "ow";
    
    /** Comando para abrir una conexion de lectura con el servidor */
    public static final String  COMMAND_OPENR = "or";

    /** Comando para cerrar una conexion */
    public static final String COMMAND_CLOSE = "c";

    /** Comando de ok */
    public static final String COMMAND_OK = "ok";

    /** Comando de error */
    public static final String COMMAND_KO = "ko";

    /** Comando para indicar escritura en el servidor */
    public static final String COMMAND_WRITE = "w";

    /** Comando para pedir si existen mas datos que leer del servidor */
    public static final String COMMAND_READ = "r";

    //Constantes de configuracion
    /** URL del servidor */
    public static final String CONFIG_SERVER = "org.tsh.client.org.tsh.server";

    /** Usar proxy */
    public static final String CONFIG_USEPROXY = "org.tsh.client.useProxy";

    /** Host del proxy */
    public static final String CONFIG_PROXYHOST = "org.tsh.client.proxyHost";

    /** Puerto del proxy */
    public static final String CONFIG_PROXYPORT = "cliente.proxyPort";

    /** Usar https */
    public static final String CONFIG_USEHTTPS = "org.tsh.client.userHttps";

}