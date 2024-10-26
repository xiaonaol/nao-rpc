package com.example;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import org.example.netty.AppClient;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class NettyTest {
    @Test
    public void testCompositeByteBuf() {
        ByteBuf header = Unpooled.buffer();
        ByteBuf body = Unpooled.buffer();

        // 通过逻辑组装而不是物理拷贝，实现在JVM内零拷贝
        CompositeByteBuf byteBuf = Unpooled.compositeBuffer();
        byteBuf.addComponents(header, body);
    }

    @Test
    public void testWrapper() {
        byte[] buf = new byte[1024];
        byte[] buf2 = new byte[1024];

        // 共享byte数组的内容的内容而不是拷贝
        ByteBuf byteBuf = Unpooled.wrappedBuffer(buf, buf2);
    }

    public void testMessage() throws IOException {
        ByteBuf message = Unpooled.buffer();
        // magic number
        message.writeBytes("xiaonaol".getBytes(StandardCharsets.UTF_8));
        // version
        message.writeByte(1);
        // head length
        message.writeShort(125);
        // full length
        message.writeInt(256);
        // message type
        message.writeByte(1);
        // Serialization type
        message.writeByte(0);
        // Compress type
        message.writeByte(2);
        // id
        message.writeLong(2514245L);
        // 用对象流转化为字节数组
        AppClient client = new AppClient();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(outputStream);
        oos.writeObject(client);
        byte[] bytes = outputStream.toByteArray();
        message.writeBytes(bytes);

        System.out.println(message.readableBytes());
    }

    @Test
    public void testCompress() throws IOException {
        byte[] buf = new byte[] {12,12,12,12,12,12,12,12,12,12,12,12,12};

        // 将buf作为输入，将结果输出到另一个字节数组当中
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);

        gzipOutputStream.write(buf);
        gzipOutputStream.finish();

        byte[] bytes = outputStream.toByteArray();
        System.out.println(Arrays.toString(bytes));
    }

    @Test
    public void testDeCompress() throws IOException {
        byte[] buf = new byte[] {12,12,12,12,12,12,12,12,12,12,12,12,12};

        // 将buf作为输入，将结果输出到另一个字节数组当中
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        GZIPInputStream gzipInputStream = new GZIPInputStream(bais);

        byte[] bytes = gzipInputStream.readAllBytes();
        System.out.println(Arrays.toString(bytes));
    }
}
