package org.example.loadbalancer.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.NrpcBootstrap;
import org.example.exceptions.LoadBalancerException;
import org.example.loadbalancer.AbstractLoadBalancer;
import org.example.loadbalancer.Selector;
import org.example.transport.message.NrpcRequest;

import java.net.InetSocketAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询的负载均衡策略
 * @author xiaonaol
 * @date 2024/11/24
 **/
@Slf4j
public class ConsistentHashLoadBalancer extends AbstractLoadBalancer{

    @Override
    protected Selector getSelector(List<InetSocketAddress> serviceList) {
        return new ConsistentHashSelector(serviceList, 128);
    }

    /**
     * 一致性hash的具体算法实现
     */
    private static class ConsistentHashSelector implements Selector {

        // hash环用来存储服务器节点
        private SortedMap<Integer, InetSocketAddress> circle = new TreeMap<>();
        // 虚拟节点的个数
        private int virtualNodes;

        public ConsistentHashSelector(List<InetSocketAddress> serviceList, int virtualNodes) {
            // 将节点转化为虚拟节点，进行挂载
            this.virtualNodes = virtualNodes;
            for(InetSocketAddress inetSocketAddress: serviceList) {
                // 需要把每一个节点加入到hash环中
                addNodeToCircle(inetSocketAddress);
            }
        }

        /**
         * 具体的hash算法 todo 需要改进
         * @param s
         * @return hash值
         */
        private int hash(String s) {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
            byte[] digest = md.digest(s.getBytes());
            // md5得到的结果是一个字节数组，但是我们想要int 4个字节

            int res = 0;
            for(int i = 0; i < 4; i++) {
                res = res << 8;
                if(digest[i] < 0) {
                    res = res | (digest[i] & 255);
                }
                res = res | digest[i];
            }

            return res;
        }

        @Override
        public InetSocketAddress getNext() {
            // 1、hash环已经建立，接下来要对请求的要素做处理
            // 需要获取到具体的请求 --> ThreadLocal
            NrpcRequest nrpcRequest = NrpcBootstrap.REQUEST_THREAD_LOCAL.get();

            // 我们想根据请求的一些特征来选择服务器 id
            String requestId = Long.toString(nrpcRequest.getRequestId());

            // 对请求的id做hash，字符串默认的hash不好
            int hash = hash(requestId);

            // 判断该hash值是否能够直接落在一个服务器上
            if(!circle.containsKey(hash)) {
                // 寻找最近的一个节点
                SortedMap<Integer, InetSocketAddress> tailMap = circle.tailMap(hash);
                hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
            }


            return circle.get(hash);
        }

        /**
         * 将每个接待你挂载到hash环上
         * @param inetSocketAddress 节点地址
         */
        private void addNodeToCircle(InetSocketAddress inetSocketAddress) {
            // 为每一个节点生成匹配的虚拟节点进行挂载
            for(int i = 0; i < virtualNodes; i++) {
                int hash = hash(inetSocketAddress + "-" + i);
                // 挂载到hash环上
                circle.put(hash, inetSocketAddress);
                if(log.isDebugEnabled()) {
                    log.debug("hash【{}】的节点已经挂载到了哈希环上", hash);
                }
            }
        }

        /**
         * 将节点从hash环上删除
         * @param inetSocketAddress 节点地址
         */
        private void removeNodeFromCircle(InetSocketAddress inetSocketAddress) {
            // 为每一个节点生成匹配的虚拟节点进行挂载
            for(int i = 0; i < virtualNodes; i++) {
                int hash = hash(inetSocketAddress + "-" + i);
                // 挂载到hash环上
                circle.remove(hash, inetSocketAddress);
            }
        }

        private String toBinary(int i) {
            String s = Integer.toBinaryString(i);
            int index = 32 - s.length();
            StringBuilder sb = new StringBuilder(s);
            for(int j = 0; j < index; j ++ ) {
                sb.append(0);
            }
            sb.append(s);
            return sb.toString();
        }
    }
}
