/*
 * Created on 01-nov-2003
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.tsh.server;

/**
 * @author juanma
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ServiceServerInfo {
	
	//Nombre del servicio
	private String name = null;
	
	//Host al que hay que conectarse
	private String host = null;
	
	//Puerto del servicio remoto
	private int port = -1;
	
	//Usuario para autentificarse
	private String user = null;
	
	//Password
	private String password = null;
    
    //Timeout de la sesion
    private long sessionTimeout = 1800000;
	
	

	/**
	 * @return Returns the host.
	 */
	public String getHost() {
		return host;
	}

	/**
	 * @param host The host to set.
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return Returns the password.
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password The password to set.
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return Returns the port.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * @param port The port to set.
	 */
	public void setPort(String port) {
		this.port = Integer.parseInt(port);
	}

	/**
	 * @return Returns the user.
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @param user The user to set.
	 */
	public void setUser(String user) {
		this.user = user;
	}

    /**
     * @return
     */
    public long getSessionTimeout() {
        return sessionTimeout;
    }

    /**
     * @param l
     */
    public void setSessionTimeout(String l) {
        sessionTimeout = Long.parseLong(l);
    }

}
