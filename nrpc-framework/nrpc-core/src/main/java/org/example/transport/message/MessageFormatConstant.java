package org.example.transport.message;

/**
 * @author xiaonaol
 * @date 2024/11/16
 **/
public class MessageFormatConstant {
    public final static byte[] MAGIC = "nrpc".getBytes();
    public final static int VERSION = 1;
    public final static short HEADER_LENGTH = (short) (MAGIC.length + 1 + 2 + 4 + 1 + 1 + 1 + 8);
    public final static short FULL_LENGTH = 4;


}
