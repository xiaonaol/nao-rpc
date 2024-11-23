package org.example.channelHandler.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import org.example.compress.Compressor;
import org.example.compress.CompressorFactory;
import org.example.enumeration.RequestType;
import org.example.serialize.Serializer;
import org.example.serialize.SerializerFactory;
import org.example.transport.message.MessageFormatConstant;
import org.example.transport.message.NrpcResponse;
import org.example.transport.message.RequestPayload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * @author xiaonaol
 * @date 2024/11/19
 **/
@Slf4j
public class NrpcResponseEncoder extends MessageToByteEncoder<NrpcResponse> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, NrpcResponse nrpcResponse, ByteBuf byteBuf) throws Exception {
        // 4个字节的魔术值
        byteBuf.writeBytes(MessageFormatConstant.MAGIC);
        // 1个字节的版本号
        byteBuf.writeByte(MessageFormatConstant.VERSION);
        // 2个字节的头部长度
        byteBuf.writeShort(MessageFormatConstant.HEADER_LENGTH);
        //
        byteBuf.writerIndex(byteBuf.writerIndex() + MessageFormatConstant.FULL_FIELD_LENGTH);
        // 3个类型
        byteBuf.writeByte(nrpcResponse.getCode());
        byteBuf.writeByte(nrpcResponse.getSerializeType());
        byteBuf.writeByte(nrpcResponse.getCompressType());
        // 8字节的请求id
        byteBuf.writeLong(nrpcResponse.getRequestId());

        // 如果是心跳请求就不处理请求体 "ping" "pong"

        // 1. 对响应做序列化
        Serializer serializer = SerializerFactory.getSerializer(nrpcResponse
                .getSerializeType()).getSerializer();
        byte[] body = serializer.serialize(nrpcResponse.getBody());

        // 2. 压缩
        Compressor compressor = CompressorFactory.getCompressor(nrpcResponse.getCompressType()).getCompressor();
        body = compressor.compress(body);

        if(body != null) {
            byteBuf.writeBytes(body);
        }

        int bodyLength = body == null ? 0 : body.length;
        // 重新处理报文的总长度
        // 保存当前写指针的位置
        int writerIndex = byteBuf.writerIndex();
        // 将写指针移动到总长度的位置上
        byteBuf.writerIndex(MessageFormatConstant.MAGIC_LENGTH + MessageFormatConstant.VERSION_LENGTH
                + MessageFormatConstant.HEADER_FIELD_LENGTH);
        byteBuf.writeInt(MessageFormatConstant.HEADER_LENGTH + bodyLength);

        // 将写指针归位
        byteBuf.writerIndex(writerIndex);

        if(log.isDebugEnabled()){
            log.debug("响应【{}】已在服务端完成解码", nrpcResponse.getRequestId());
        }
    }
}
