package org.example.enumeration;

import lombok.Getter;

/**
 * @author xiaonaol
 * @date 2024/11/18
 **/
public enum RequestType {
    REQUEST((byte) 1, "普通请求"), HEART_BEAT((byte) 2, "心跳检测");

    @Getter
    private byte id;
    private String type;

    RequestType(byte id, String type) {
        this.id = id;
        this.type = type;
    }


}
