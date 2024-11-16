package org.example.channelHandler.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.example.transport.message.MessageFormatConstant;
import org.example.transport.message.NrpcRequest;
import org.example.transport.message.RequestPayload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * magic       4B   ----> nrpc.getBytes()
 * version     1B    ---->  1
 * head length 2B
 * full length 4B
 * serialize   1B
 * compress    1B
 * requestType 1B
 * requestId   8B
 *
 * body
 *
 * 出站时，第一个经过的处理器
 * @author xiaonaol
 * @date 2024/11/16
 **/
@Slf4j
public class NrpcMessageEncoder extends MessageToByteEncoder<NrpcRequest> {

    // 将nrpcRequest的内容写到byteBuf里
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, NrpcRequest nrpcRequest, ByteBuf byteBuf) throws Exception {
        // 4个字节的魔术值
        byteBuf.writeBytes(MessageFormatConstant.MAGIC);
        // 1个字节的版本号
        byteBuf.writeByte(MessageFormatConstant.VERSION);
        // 2个字节的头部长度
        byteBuf.writeShort(MessageFormatConstant.HEADER_LENGTH);
        //
        byteBuf.writerIndex(byteBuf.writerIndex() + 4);
        // 3个类型
        byteBuf.writeByte(nrpcRequest.getRequestType());
        byteBuf.writeByte(nrpcRequest.getSerializeType());
        byteBuf.writeByte(nrpcRequest.getCompressType());
        // 8字节的请求id
        byteBuf.writeLong(nrpcRequest.getRequestId());
        // 写入请求体requestPayload
        byte[] body = getBodyBytes(nrpcRequest.getRequestPayload());
        byteBuf.writeBytes(body);

        // 重新处理报文的总长度
        // 保存当前写指针的位置
        int writerIndex = byteBuf.writerIndex();
        // 将写指针移动到总长度的位置上
        byteBuf.writerIndex(7);
        byteBuf.writeInt(MessageFormatConstant.HEADER_LENGTH + body.length);

        // 将写指针归位
        byteBuf.writerIndex(writerIndex);
    }

    private byte[] getBodyBytes(RequestPayload requestPayload) {
        // todo 针对不同的消息类型做不通的处理
        // 序列化和压缩
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream);
            outputStream.writeObject(requestPayload);

            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            log.error("序列化出现异常");
            throw new RuntimeException(e);
        }
    }
}
