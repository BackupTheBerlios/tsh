package org.tsh.client;

import org.tsh.common.Constants;

/**
 * Información de configuración de un servicio.
 * Fecha 29-oct-2003
 * @author jmgarcia
 * 
 */
public class ServiceInfo {

    private String name = null;
    private int localPort = -1 ;
    private boolean useProxy = false; 
    private String proxyHost = null;
    private int proxyPort = -1;
    private String proxyUser = null;
    private String proxyPassword = null;
    private boolean useHttps = false;
    private String serverURL = null;
    private String user = null;
    private String password = null;     
    private int maxConnections = Constants.MAXCONNECTIONS_CLIENT;

    /**
     * @return
     */
    public String getProxyUser() {
        return proxyUser;
    }

    /**
     * @return
     */
    public String getServerURL() {
        return serverURL;
    }

    /**
     * @return
     */
    public boolean isUseHttps() {
        return useHttps;
    }

    /**
     * @return
     */
    public boolean isUseProxy() {
        return useProxy;
    }

    /**
     * @return
     */
    public String getUser() {
        return user;
    }

    /**
     * @param string
     */
    public void setProxyUser(String string) {
        proxyUser = string;
    }

    /**
     * @param string
     */
    public void setServerURL(String string) {
        serverURL = string;
    }

    /**
     * @param b
     */
    public void setUseHttps(String b) {
        useHttps = Boolean.valueOf(b).booleanValue();
    }

    /**
     * @param b
     */
    public void setUseProxy(String b) {
        useProxy = Boolean.valueOf(b).booleanValue();
    }

    /**
     * @param string
     */
    public void setUser(String string) {
        user = string;
    }

    /**
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * @return
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return
     */
    public String getProxyHost() {
        return proxyHost;
    }

    /**
     * @return
     */
    public String getProxyPassword() {
        return proxyPassword;
    }

    /**
     * @return
     */
    public int getProxyPort() {
        return proxyPort;
    }

    /**
     * @param string
     */
    public void setName(String string) {
        name = string;
    }

    /**
     * @param string
     */
    public void setPassword(String string) {
        password = string;
    }

    /**
     * @param string
     */
    public void setProxyHost(String string) {
        proxyHost = string;
    }

    /**
     * @param string
     */
    public void setProxyPassword(String string) {
        proxyPassword = string;
    }

    /**
     * @param i
     */
    public void setProxyPort(String i) {
        proxyPort = Integer.parseInt(i);
    }

    /**
     * @return
     */
    public int getLocalPort() {
        return localPort;
    }

    /**
     * @param i
     */
    public void setLocalPort(String i) {
        localPort = Integer.parseInt(i);
    }

	/**
	 * @return Returns the maxConnections.
	 */
	public int getMaxConnections() {
		return maxConnections;
	}

	/**
	 * Establece el numero maximo de conexiones simultaneas que se atienden para este servicio
	 * @param maxConnections The maxConnections to set.
	 */
	public void setMaxConnections(String maxConnections) {
		this.maxConnections = Integer.parseInt(maxConnections);
	}

}
