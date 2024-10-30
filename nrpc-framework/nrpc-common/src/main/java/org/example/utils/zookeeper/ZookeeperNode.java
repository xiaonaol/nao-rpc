package org.example.utils.zookeeper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author xiaonaol
 * @date 2024/10/28
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ZookeeperNode {

    private String nodePath;
    private byte[] data;
}
