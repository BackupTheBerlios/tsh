package org.tsh.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpConnection;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpRecoverableException;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.ResponseInputStream;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tsh.common.Constants;

/**
 * Conexiones con el org.tsh.server
 * 
 * @author jmgarcia
 */
public class ConnectionManager {
	
	/** objeto de log */
	private static Log logger = LogFactory.getLog(ProxyHost.class);
	
    private String serverURL = null;
    private boolean useHTTPProxy = false;
    private String proxyHost = null;
    private int proxyPort = -1;
    private boolean useHTTPS = false;
    private String serverUser;
    private String serverPassword;
    private MultiThreadedHttpConnectionManager connManager;
    

    /**
	 * Crea un manejador de conexiones con el servidor
	 * 
	 * @param service
	 *            Informacion del servicio
	 */
    public ConnectionManager(ServiceInfo service) {
 	
        this.serverURL = service.getServerURL();
        this.useHTTPProxy = service.isUseProxy();
        this.proxyHost = service.getProxyHost();
        this.proxyPort = service.getProxyPort();
        this.useHTTPS = service.isUseHttps();
        this.serverUser = service.getUser();
        this.serverPassword = service.getPassword();
        if (useHTTPProxy && !useHTTPS) {
            System.getProperties().put("proxySet", "true");
            System.getProperties().put("http.proxyHost", proxyHost);
            System.getProperties().put("http.proxyPort", String.valueOf(proxyPort));
        }

        if (useHTTPProxy && useHTTPS) {
            System.getProperties().put("proxySet", "true");
            System.getProperties().put("https.proxyHost", proxyHost);
            System.getProperties().put("https.proxyPort", String.valueOf(proxyPort));

            //Esto es para admitir cualquier certificado de https
            // TODO
            HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                public boolean verify(String ip, String urlCert) {
                    return true;
                }

                public boolean verify(String arg0, SSLSession arg1) {

                    return true;
                }
            });

        }
        
        /////
        this.connManager=new MultiThreadedHttpConnectionManager();
        //this.connManager.setMaxTotalConnections(10);

    }

    /**
	 * Devuelve la url del servidor en funcion de si hay que usar http o https
	 * 
	 * @return URL de conexion al servidor
	 */
    public String getServerURL() {
        if (this.useHTTPS) {
            return "https://" + serverURL;
        }
        else {
            return "http://" + serverURL;
        }
    }

    /**
	 * Devuelve informacion del objeto
	 * 
	 * @return String
	 */
    public String toString() {
        return " ConnectionManager[" + this.serverURL + "," + this.useHTTPProxy + ","
                + this.proxyHost + ":" + this.proxyPort + "]";
    }
    
    public HttpConnection getHttpConnection() throws HttpRecoverableException, HttpException, IOException {
    			
		HostConfiguration hconf = new HostConfiguration();
		if ( useHTTPProxy ) {
		   //Hay que usar proxy
		   hconf.setProxy(this.proxyHost,this.proxyPort);
		}
		URI uri = new URI(this.getServerURL());
		hconf.setHost(uri);
        HttpConnection conn = connManager.getConnection(hconf);
        logger.debug("Conexiones en uso " +  connManager.getConnectionsInUse(hconf) + "/" + connManager.getConnectionsInUse());
		//HttpConnection conn = new HttpConnection(hconf);
        conn.setSoTimeout(Constants.READ_TIMEOUT);       
                
        
        return conn;
    }       
    
    public static boolean readResponseHeaders(HttpConnection conn) throws IOException, IllegalStateException {
    	//Leer response headers
    	String header = null;
    	boolean chunked = false;
    	while (true) {
    		try {
	    		header = conn.readLine();
	    		if ( header.trim().compareTo("") == 0 ) break;
	    		logger.debug("Header=>" + header);
	    		chunked = (header.trim().compareToIgnoreCase("Transfer-Encoding: chunked") == 0);
    		}catch (SocketTimeoutException ste) {    		
    		}
    	}
    	logger.debug ("chunked: " + chunked );
    	return chunked;
    }

   /**
    * @param conn
    * @param serverURL2
    */
   public static void writeRequestHeader(HttpConnection conn, String serverURL) throws HttpRecoverableException, IllegalStateException, URIException, IOException  {
      URI uri = new URI(serverURL);
      // Se escribe la cabecera HTTP
      conn.printLine("POST " + uri.getPath() +  " HTTP/1.1");
      conn.printLine("Host: " + uri.getHost());
      conn.printLine("Connection: close, TE");
      //conn.printLine("TE: trailers, deflate, gzip, compress");
      conn.printLine("TE: ");      
      conn.printLine("User-Agent: Mozilla/4.0 (compatible)");    	
      conn.printLine("Accept-Language: es");
      conn.printLine("Content-type: application/octet-stream"); 
      conn.flushRequestOutputStream();
      
   }

   /**
    * Lee la cabecera de la respuesta HTTP y obtiene el InputStream de lectura
    * @param conn Conexion
    * @return InputStream de escritura
    */
   public static InputStream getInputStream(HttpConnection conn) throws IllegalStateException, IOException {
      //comprobar chuncked
      boolean chuncked = ConnectionManager.readResponseHeaders(conn);
      
      //Esperando OK e id de la peticion
      return new ResponseInputStream(conn.getResponseInputStream(), chuncked, Constants.CONTENT_LENGTH);
            
   }
    

}