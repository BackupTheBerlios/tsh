package org.tsh.client;

import java.util.Date;

import org.tsh.common.*;

/**
 * Fecha 21-ene-2004
 * @author jmgarcia
 * 
 */
public class StatProxyHost implements IStat {

   
    private long recibidos = 0;
    private long enviados = 0;
    private Date startConnTime = null;
    private Date stopConnTime = null ;

    /**
     * @see org.tsh.client.IStat#addSend(long)
     */
    public void addSend(long s) {
        enviados += s;

    }

    /**
     * @see org.tsh.client.IStat#addRecieve(long)
     */
    public void addRecieve(long r) {
        recibidos += r;

    }

    /**
     * @see org.tsh.client.IStat#getReceive()
     */
    public long getReceive() {
        return recibidos;
    }

    /**
     * @see org.tsh.client.IStat#getSend()
     */
    public long getSend() {
        return enviados;
    }

    public String toString() {
       String result ="" ;
       if ( this.stopConnTime != null && this.startConnTime != null ) {
          result += "[t: " + (this.stopConnTime.getTime() - this.startConnTime.getTime()) + "]";
       }
       result += "[e: " + getSend() + "][r:" + getReceive() + "]" ;
       return result;
    }

   /* (non-Javadoc)
    * @see org.tsh.common.IStat#startConn()
    */
   public void startConn() {
      this.startConnTime = new Date();
      
   }

   /* (non-Javadoc)
    * @see org.tsh.common.IStat#stopConn()
    */
   public void stopConn() {
      this.stopConnTime = new Date();
   }
    
}
