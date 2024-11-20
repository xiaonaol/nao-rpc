package org.example.enumeration;

import lombok.Getter;

/**
 * @author xiaonaol
 * @date 2024/11/19
 **/
@Getter
public enum RespCode {
    SUCCESS((byte) 1, "成功"), FAIL((byte) 2, "失败");

    private byte code;
    private String desc;

    RespCode(byte code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
