package org.example.channelHandler.handler.providerHandler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import lombok.extern.slf4j.Slf4j;
import org.example.compress.Compressor;
import org.example.compress.CompressorFactory;
import org.example.enumeration.RequestType;
import org.example.serialize.Serializer;
import org.example.serialize.SerializerFactory;
import org.example.transport.message.MessageFormatConstant;
import org.example.transport.message.NrpcRequest;
import org.example.transport.message.RequestPayload;

/**
 * @author xiaonaol
 * @date 2024/11/16
 **/
@Slf4j
public class NrpcRequestDecoder extends LengthFieldBasedFrameDecoder {

    public NrpcRequestDecoder() {
        super(
                // 找到当前报文的总长度，截取报文
                // 最大帧长度，超过这个maxFrameLength会直接丢弃
                MessageFormatConstant.MAX_FRAME_LENGTH,
                // 长度字段的偏移量
                MessageFormatConstant.MAGIC.length + MessageFormatConstant.VERSION_LENGTH
                        + MessageFormatConstant.HEADER_FIELD_LENGTH,
                // 长度字段的长度
                MessageFormatConstant.FULL_FIELD_LENGTH,
                // todo 负载的适配长度
                -(MessageFormatConstant.MAGIC.length + MessageFormatConstant.VERSION_LENGTH
                        + MessageFormatConstant.HEADER_FIELD_LENGTH + MessageFormatConstant.FULL_FIELD_LENGTH),
                0);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        log.info("开始解码报文……");
        Object decode = super.decode(ctx, in);
        if(decode instanceof ByteBuf byteBuf){
            return decodeFrame(byteBuf);
        }
        return null;
    }

    private Object decodeFrame(ByteBuf byteBuf) {
        // 1、解析魔数
        byte[] magic = new byte[MessageFormatConstant.MAGIC.length];
        byteBuf.readBytes(magic);
        // 检测魔数是否匹配
        for (int i = 0; i < magic.length; i++) {
            if(magic[i] != MessageFormatConstant.MAGIC[i]) {
                throw new RuntimeException("请求不合法");
            }
        }

        // 2、解析版本号
        byte version = byteBuf.readByte();
        if(version > MessageFormatConstant.VERSION) {
            throw new RuntimeException("请求版本不被支持");
        }

        // 3、解析头部的长度
        short headLength = byteBuf.readShort();

        // 4、解析总长度
        int fullLength = byteBuf.readInt();

        // 5、请求类型 todo 判断是不是心跳检测
        byte requestType = byteBuf.readByte();

        // 6、序列化类型
        byte serializeType = byteBuf.readByte();

        // 7、压缩类型
        byte compressType = byteBuf.readByte();

        // 8、请求id
        long requestId = byteBuf.readLong();

        // 9、时间戳
        long timeStamp = byteBuf.readLong();

        // 我们需要封装
        NrpcRequest nrpcRequest = new NrpcRequest();
        nrpcRequest.setRequestId(requestId);
        nrpcRequest.setSerializeType(serializeType);
        nrpcRequest.setCompressType(compressType);
        nrpcRequest.setRequestType(requestType);
        nrpcRequest.setTimeStamp(timeStamp);


        // 心跳请求没有负载，直接返回
        if(requestType == RequestType.HEART_BEAT.getId()) {
            return nrpcRequest;
        }

        int payloadLength = fullLength - headLength;
        byte[] payload = new byte[payloadLength];
        byteBuf.readBytes(payload);

        // 有了payload字节数组后，就可以解压缩反序列化
        // 1. 解压缩
        if(payload.length > 0) {
            Compressor compressor = CompressorFactory.getCompressor(nrpcRequest.getCompressType()).getImpl();
            payload = compressor.decompress(payload);

            // 2. 反序列化
            Serializer serializer = SerializerFactory.getSerializer(serializeType).getImpl();
            RequestPayload requestPayload = serializer.deserialize(payload, RequestPayload.class);
            nrpcRequest.setRequestPayload(requestPayload);
        }

        if(log.isDebugEnabled()) {
            log.debug("请求【{}】已经在服务端完成解码", nrpcRequest.getRequestId());
        }

        return nrpcRequest;
    }
}
