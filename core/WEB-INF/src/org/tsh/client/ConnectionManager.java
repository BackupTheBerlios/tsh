package org.tsh.client;

import java.io.IOException;
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
import org.apache.commons.httpclient.URI;
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
        this.connManager= new MultiThreadedHttpConnectionManager();

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

    /**
	 * Abre una conexion con el servidor http
	 * 
	 * @return Conexion
	 * @throws MalformedURLException
	 * @throws IOException
	 */
    /*
    public HttpURLConnection getURLConnection() throws MalformedURLException, IOException {
        URL url = new URL(this.getServerURL());
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("POST");        
        urlConnection.setDoOutput(true);
        urlConnection.setUseCaches(false);
        urlConnection.setRequestProperty("Content-Type", "application/octet-stream");
        urlConnection.setRequestProperty("Connection", "keep-alive");
        return urlConnection;
    }
    */

    public HttpURLConnection getURLConnection() throws MalformedURLException, IOException {
    	URL url = new URL(this.getServerURL());
    	System.getProperties().put("HTTPClient.dontChunkRequests", "true"); 
    	System.getProperties().put("HTTPClient.disable_pipelining", "true"); 
    	System.getProperties().put("HTTPClient.disableKeepAlives", "true");
    	System.getProperties().put("HTTPClient.deferStreamed", "true");
    	//System.getProperties().put("HTTPClient.forceHTTP_1.0", "true");
    	
    	HTTPClient.HttpURLConnection urlConnection = new HTTPClient.HttpURLConnection (url);
    	urlConnection.setRequestMethod("POST");        
    	urlConnection.setDoOutput(true);
    	urlConnection.setUseCaches(false);
    	urlConnection.setRequestProperty("Content-Type", "application/octet-stream");
    	//urlConnection.setRequestProperty("Connection", "keep-alive");
    	//urlConnection.setRequestProperty("Content-Length","500000000");
    	return urlConnection;
    }

    /*
    public HttpURLConnection getURLConnection() throws MalformedURLException, IOException {
        URL url = new URL(this.getServerURL());
        
        //HttpClient client = new HttpClient(connectionManager);
        HttpClient client = new HttpClient(new MultiThreadedHttpConnectionManager());
        
        PostMethod post = new PostMethod(this.getServerURL());        
        post.addRequestHeader("Content-Type", "application/octet-stream");
        //post.addRequestHeader("Connection", "keep-alive");
        
        //urlConnection.setRequestMethod("POST");        
        //urlConnection.setDoOutput(true);
        //urlConnection.setUseCaches(false);
        //urlConnection.setRequestProperty("Content-Type", "application/octet-stream");
        //urlConnection.setRequestProperty("Connection", "keep-alive");
        client.executeMethod(post);
        org.apache.commons.httpclient.util.HttpURLConnection urlConnection = 
        		new org.apache.commons.httpclient.util.HttpURLConnection(post,url);
        
       
        return urlConnection;
    }
    */
    
    public HttpConnection getHttpConnection() throws HttpRecoverableException, HttpException, IOException {
    			
		HostConfiguration hconf = new HostConfiguration();    	
		hconf.setHost(new URI(this.getServerURL()));
        //HttpConnection conn = connManager.getConnection(hconf);
		HttpConnection conn = new HttpConnection(hconf);
        conn.setSoTimeout(Constants.READ_TIMEOUT);
        return conn;
    }
        
    public static void writeRequestHeader(HttpConnection conn) throws IOException{
    	conn.printLine("POST /tsh/service HTTP/1.0");
    	conn.printLine("Host: 127.0.0.1:8080");
    	conn.printLine("Connection: close, TE");
    	conn.printLine("TE: trailers, deflate, gzip, compress");
    	conn.printLine("User-Agent: RPT-HTTPClient/0.3-3");
    	conn.printLine("Content-type: application/octet-stream");    	    	    	 
    }
    
    public static void readResponseHeaders(HttpConnection conn) throws IOException, IllegalStateException {
    	//Leer response headers
    	String header = null;
    	while (true) {
    		try {
	    		header = conn.readLine();
	    		if ( header.trim().compareTo("") == 0 ) break;
	    		logger.debug("Header=>" + header);
    		}catch (SocketTimeoutException ste) {    		
    		}
    	}
    }
    

}