package client;

import common.*;

/**
 * Fecha 21-ene-2004
 * @author jmgarcia
 * 
 */
public class StatProxyHost implements IStat {

   
    private long recibidos = 0;
    private long enviados = 0;

    /**
     * @see client.IStat#addSend(long)
     */
    public void addSend(long s) {
        enviados += s;

    }

    /**
     * @see client.IStat#addRecieve(long)
     */
    public void addRecieve(long r) {
        recibidos += r;

    }

    /**
     * @see client.IStat#getReceive()
     */
    public long getReceive() {
        return recibidos;
    }

    /**
     * @see client.IStat#getSend()
     */
    public long getSend() {
        return enviados;
    }

}
