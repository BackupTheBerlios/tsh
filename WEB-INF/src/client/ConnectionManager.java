package client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

/**
 * Conexiones con el server
 * 
 * @author jmgarcia
 */
public class ConnectionManager {
    private String serverURL = null;
    private boolean useHTTPProxy = false;
    private String proxyHost = null;
    private int proxyPort = -1;
    private boolean useHTTPS = false;
    private String serverUser;
    private String serverPassword;

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
    public HttpURLConnection getURLConnection() throws MalformedURLException, IOException {
        URL url = new URL(this.getServerURL());
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("POST");        
        urlConnection.setDoOutput(true);
        urlConnection.setUseCaches(false);
        urlConnection.setRequestProperty("Content-Type", "application/octet-stream");
        return urlConnection;
    }

}