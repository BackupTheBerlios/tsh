package org.tsh.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.tsh.common.Constants;
import org.tsh.remote.RemoteHttpOutputStream;

/**
 * Lee datos de un output stream remoto y los escribe al input stream local
 * Fecha 15-mar-2004
 * 
 * @author jmgarcia
 *  
 */
public class RemoteConnReader implements Runnable {
	// Logger
	private static final Log logger = LogFactory.getLog(RemoteConnReader.class);

	private RemoteConn conn = null;
	private InputStream input = null;
	private RemoteHttpOutputStream output = null;
	private boolean stop = false;

	/**
	 * @param input
	 *            Stream de lectura
	 * @param output
	 *            Stream de escritura
	 * @param conn
	 *            Conexion asociada al reader
	 */
	public RemoteConnReader(
		InputStream input,
		RemoteHttpOutputStream output,
		RemoteConn conn) {
		this.conn = conn;
		this.input = input;
		this.output = output;
	}

	/**
	 * Bucle principal de lectura de un stream y escritura en el otro
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		int readed = 0;
		byte[] buffer = new byte[Constants.BUFFER];		
		try {
			while (readed >= 0 && !stop ) {
				try {
					readed = input.read(buffer);
					if (readed > 0 ) {
						output.getOutputStream().write(buffer, 0, readed);
						logger.debug("Leidos del cliente " + readed + " bytes");
						output.getOutputStream().flush();						
					}
				} catch (SocketTimeoutException ste) {					
				}
			}

		} catch (IOException e) {
			// TODO que hay que hacer
			logger.debug("Error en bucle de lectura del cliente", e);
		} finally {
			try {
				// TODO El cliente cierra la conexion (se termina la comunicación
				// desde el lado cliente)
				output.getOutputStream().close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			if (stop) {
				// TODO Termina limpiamente
				logger.debug("Stop lector del cliente");
			} else {
				// TODO cerrar
				logger.debug("Terminando lector del cliente");
				this.conn.finishReader();
			}

		}
	}

	/**
	 * Se solicita que se termine el servicio
	 *  
	 */
	public void stop() {
		stop = true;
	}
}
