package org.example.transport.message;

/**
 * @author xiaonaol
 * @date 2024/11/16
 **/
public class MessageFormatConstant {
    public final static byte[] MAGIC = "nrpc".getBytes();
    public final static int VERSION = 1;
    // 头部信息的长度
    public final static short HEADER_LENGTH = (short) (MAGIC.length + 1 + 2 + 4 + 1 + 1 + 1 + 8);

    public final static int MAX_FRAME_LENGTH = 1024 * 1024;
    public static final int VERSION_LENGTH = 1;
    // 头部信息长度占用字节数
    public static final int HEADER_FIELD_LENGTH = 2;
    // 总长度占用的字节数
    public static final int FULL_FIELD_LENGTH = 4;
}
