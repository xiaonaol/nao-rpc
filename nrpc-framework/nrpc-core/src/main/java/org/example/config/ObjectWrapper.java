package org.example.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xiaonaol
 * @date 2024/12/3
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ObjectWrapper<T> {
    private Byte code;
    private String name;
    private T impl;
}
