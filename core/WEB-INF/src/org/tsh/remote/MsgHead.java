package org.tsh.remote;

/**
 * Fecha 23-abr-2004
 * 
 * @author jmgarcia
 *  
 */
public class MsgHead {

   private byte msg;
   private int data;

   /**
    *  
    */
   public MsgHead(byte msg, int data) {
      this.msg = msg;
      this.data = data;
   }

   /**
    *  
    */
   public MsgHead() {

      // TODO Auto-generated constructor stub
   }

   public byte getMsg() {
      return this.msg;
   }
   public int getData() {
      return this.data;
   }
   public void setMsg(byte msg) {
      this.msg = msg;
   }
   public void setData(int data) {
      this.data = data;
   }

}
