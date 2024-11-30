package org.example;

import lombok.Data;

import lombok.extern.slf4j.Slf4j;
import org.example.discovery.RegistryConfig;
import org.example.loadbalancer.LoadBalancer;
import org.example.loadbalancer.impl.RoundRobinLoadBalancer;
import org.example.serialize.Serializer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

/**
 * 全局的配置类，代码配置-->xml配置-->默认项
 * @author xiaonaol
 * @date 2024/11/30
 **/
@Data
@Slf4j
public class Configuration {
    // 配置信息-->端口号
    private int port = 8088;

    // 配置信息-->应用程序名
    private String appName = "default";

    // 配置信息-->注册中心
    private RegistryConfig registryConfig;

    // 配置信息-->序列化协议
    private ProtocolConfig protocolConfig;

    // 配置信息-->ID生成器
    private IdGenerator idGenerator = new IdGenerator(1, 2);

    // 配置信息-->序列化方式
    private String serializeType = "hessian";

    // 配置信息-->压缩方式
    private String compressType = "gzip";

    // 配置信息-->负载均衡策略
    private LoadBalancer loadBalancer = new RoundRobinLoadBalancer();

    // 读xml
    public Configuration() {
        // 读取xml获得上面的信息
        loadFromXml(this);
    }

    /**
     * 从xml配置文件中读取配置
     * @param configuration 配置实例
     * @author xiaonaol
     */
    private void loadFromXml(Configuration configuration) {
        try {
            // 1、创建一个Document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("nrpc.xml");
            Document doc = builder.parse(inputStream);

            // 2、获取一个xpath的解析器
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();

            // 3、解析一个表达式
            String expression = "/configuration/port";
            String port = parseString(xPath, doc, expression);

            expression = "/configuration/serializer";
            Serializer serializer = parseObject(xPath, doc, expression, null);

        } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
            log.info("未读取到配置文件", e);
        }
        //
    }

    /**
     * 获得一个节点属性的值 <port> 8088 </port>
     * @param doc           文档对象
     * @param xPath         xpath解析器
     * @param expression    表达式
     * @return              配置的实例
     * @author xiaonaol
     */
    private String parseString(XPath xPath, Document doc, String expression)
            throws XPathExpressionException {

        XPathExpression expr = xPath.compile(expression);
        // 表达式可以帮我们获取节点
        Node targetNode = (Node) expr.evaluate(doc, XPathConstants.NODE);
        return targetNode.getTextContent();
    }

    /**
     * 解析一个节点，返回一个实例
     * @param doc        文档对象
     * @param xpath      xpath解析器
     * @param expression xpath表达式
     * @param paramType  参数列表
     * @param param      参数
     * @param <T>        泛型
     * @return 配置的实例
     */
    private <T> T parseObject(XPath xpath, Document doc, String expression, Class<?>[] paramType, Object... param) {
        try {
            XPathExpression expr = xpath.compile(expression);
            // 我们的表达式可以帮我们获取节点
            Node targetNode = (Node) expr.evaluate(doc, XPathConstants.NODE);
            String className = targetNode.getAttributes().getNamedItem("class").getNodeValue();
            Class<?> aClass = Class.forName(className);
            Object instant = null;
            if (paramType == null) {
                instant = aClass.getConstructor().newInstance();
            } else {
                instant = aClass.getConstructor(paramType).newInstance(param);
            }
            return (T) instant;
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException | XPathExpressionException e) {
            log.error("An exception occurred while parsing the expression.", e);
        }
        return null;
    }


    // 进行配置



}
