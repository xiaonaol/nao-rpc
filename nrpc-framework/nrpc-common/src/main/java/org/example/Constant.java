package org.example;

/**
 * @author xiaonaol
 * @date 2024/10/28
 **/
public class Constant {

    // Zookeeper默认链接地址
    public static final String DEFAULT_ZK_CONNECT = "127.0.0.1:2181";

    // Zookeeper默认超时时间
    public static final int TIME_OUT = 10000;

    // 服务提供方和调用方在注册中心的路径
    public static final String BASE_PROVIDERS_PATH = "/xiaonaol-metadata/providers";
    public static final String BASE_CONSUMERS_PATH = "/xiaonaol-metadata/consumers";
}
