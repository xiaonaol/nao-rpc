package org.example.transport.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务提供方回复的响应
 * @author xiaonaol
 * @date 2024/11/18
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NrpcResponse {
    // 请求id
    private long requestId;

    // 1 成功，2 异常
    private byte code;

    //请求的类型，压缩的类型，序列化的方式
    private byte requestType;
    private byte compressType;
    private byte serializeType;

    // 具体的消息体
    private Object body;

}
