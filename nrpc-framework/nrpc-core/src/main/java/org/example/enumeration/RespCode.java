package org.example.enumeration;

import lombok.Getter;

/**
 * 响应码
 * 成功 20（方法） 21（心跳）
 * 服务端错误 50
 * 客户端错误 44
 * 负载 31（被限流）
 * @author xiaonaol
 * @date 2024/11/19
 **/
@Getter
public enum RespCode {
    SUCCESS((byte) 20, "成功"),
    SUCCESS_HEARTBEAT((byte) 21, "心跳检测成功"),
    RATE_LIMIT((byte) 31, "服务被限流"),
    RESOURCE_NOT_FOUND((byte) 44, "请求的资源不存在"),
    FAIL((byte) 50, "调用失败"),
    CLOSING((byte) 51, "关闭中");


    private byte code;
    private String desc;

    RespCode(byte code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
