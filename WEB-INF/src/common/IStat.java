package common;

/**
 * Fecha 20-ene-2004
 * 
 * @author jmgarcia
 *  
 */
public interface IStat {

    /**
     * Acumula bytes enviados
     * @param s
     */
    public void addSend(long s);
    
    /**
     * Acumula bytes recibidos
     * @param r
     */
    public void addRecieve(long r);
    
    /**
     * Devuelve bytes recibidos
	 * @return
	 */
    public long getReceive();
    /**
     * Devuelve bytes enviados
	 * @return
	 */
    public long getSend();
}