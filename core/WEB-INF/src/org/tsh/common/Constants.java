package org.tsh.common;

/**
 * Fecha 09-oct-2003
 *
 * @author jmgarcia
 */
public final class Constants {

    /** Id para decir que no hay sesion*/
    public static final int NO_SESSION = -1;

    /** Numero de conexiones maximas por servicio (valor por defecto)*/
    public static final int MAXCONNECTIONS_CLIENT = 10;

    /** DOCUMENT ME! */
    public static final int MAX_READ = 64000;

    /** DOCUMENT ME! */
    public static final int READ_TIMEOUT = 400;
    

    /** Parametro action de una conexion */
    public static final String PARAM_ACTION = "action";

    /** Parametro id de la conexion */
    public static final String PARAM_ID = "id";

    /** Parametro servicio de la peticion */
    public static final String PARAM_SERVICE = "service";
   
    //Comandos del protocolo que se envian al servidor en la peticion http
    /** Comando para abrir una conexion de escritura con el servidor */
    public static final String COMMAND_OPENW = "ow";    
    
    /** Comando para abrir una conexion de lectura con el servidor */
    public static final String COMMAND_OPENR = "or";
    

   
    //Comandos de los mensajes enviados
    /** Es un mensaje de datos, el byte siguiente indicará el tamaño del mensaje */
    public static final byte MSG_DATA = 55;
    public static final byte MSG_CLOSE = 56;
    public static final byte MSG_OK = 57;
    public static final byte MSG_KO = 58;
    public static final byte MSG_REOPEN = 59;    
    
    /** Tamaño (num. bytes) de la cabecera de los MSG*/
    public static final int MSG_HEAD_TAM = 3;

    /** Tamaño máximo del content_length */
    public static final int CONTENT_LENGTH = 50000000;
    
    
    
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
