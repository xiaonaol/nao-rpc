package org.example.config;

import lombok.extern.slf4j.Slf4j;
import org.example.IdGenerator;
import org.example.ProtocolConfig;
import org.example.compress.Compressor;
import org.example.discovery.RegistryConfig;
import org.example.loadbalancer.LoadBalancer;
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
 * @author xiaonaol
 * @date 2024/12/1
 **/
@Slf4j
public class XmlResolver {


    /**
     * 从xml配置文件中读取配置
     * @param configuration 配置实例
     * @author xiaonaol
     */
    public void loadFromXml(Configuration configuration) {
        try {
            // 1、创建一个Document
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 禁用DTD校验：可以通过调用setValidating(false)方法来禁用DTD校验
            factory.setValidating(false);
            // 禁用外部实体解析
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            InputStream inputStream = ClassLoader.getSystemClassLoader().getResourceAsStream("nrpc.xml");
            Document doc = builder.parse(inputStream);

            // 2、获取一个xpath的解析器
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();

            // 3、解析所有的标签
            configuration.setPort(resolvePort(doc, xPath));
            configuration.setAppName(resolveAppName(doc, xPath));

            configuration.setIdGenerator(resolveIdGenerator(doc, xPath));

            configuration.setRegistryConfig(resolveRegistryConfig(doc, xPath));

            configuration.setCompressType(resolveCompressType(doc, xPath));

            configuration.setSerializeType(resolveSerializeType(doc, xPath));
            configuration.setLoadBalancer(resolveLoadBalancer(doc, xPath));
            configuration.setProtocolConfig(new ProtocolConfig(configuration.getSerializeType()));

            configuration.setCompressor(resolveCompressor(doc, xPath));

            configuration.setSerializer(resolveSerializer(doc, xPath));

            // 如果有新增的标签从这里添加

        } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException |
                 ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            log.info("未读取到配置文件", e);
        }
        //
    }

    private Serializer resolveSerializer(Document doc, XPath xPath) {
        String expression = "/configuration/serializer";
        return parseObject(xPath, doc, expression, null);
    }

    private Compressor resolveCompressor(Document doc, XPath xPath) {
        String expression = "/configuration/compressor";
        return parseObject(xPath, doc, expression, null);
    }

    private RegistryConfig resolveRegistryConfig(Document doc, XPath xPath) throws XPathExpressionException {
        String expression = "/configuration/registry";
        String url = parseString(xPath, doc, expression);
        return new RegistryConfig(url);
    }

    private int resolvePort(Document doc, XPath xPath) throws XPathExpressionException {
        String expression = "/configuration/port";
        String port = parseString(xPath, doc, expression);
        return Integer.parseInt(port);
    }

    private String resolveAppName(Document doc, XPath xPath) throws XPathExpressionException {
        String expression = "/configuration/appName";
        return parseString(xPath, doc, expression);
    }

    private LoadBalancer resolveLoadBalancer(Document doc, XPath xPath) throws XPathExpressionException {
        String expression = "/configuration/loadBalancer";
        return parseObject(xPath, doc, expression, null);
    }

    private String resolveCompressType(Document doc, XPath xPath) throws XPathExpressionException {
        String expression = "/configuration/compressType";
        return parseString(xPath, doc, expression, "type");
    }

    private String resolveSerializeType(Document doc, XPath xPath) throws XPathExpressionException {
        String expression = "/configuration/serializeType";
        return parseString(xPath, doc, expression, "type");
    }

    private IdGenerator resolveIdGenerator(Document doc, XPath xPath) throws XPathExpressionException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        String expression = "/configuration/idGenerator";
        String aClass = parseString(xPath, doc, expression, "class");
        String dataCenterId = parseString(xPath, doc, expression, "dataCenterId");
        String machineId = parseString(xPath, doc, expression, "MachineId");

        Class<?> clazz = Class.forName(aClass);
        Object instance = clazz.getConstructor(new Class[]{long.class, long.class})
                .newInstance(Long.parseLong(dataCenterId), Long.parseLong(machineId));

        return (IdGenerator) instance;
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
     * 获得一个节点属性的值   <port num="7777"></>
     * @param doc           文档对象
     * @param xpath         xpath解析器
     * @param expression    xpath表达式
     * @param AttributeName 节点名称
     * @return 节点的值
     */
    private String parseString(XPath xpath, Document doc, String expression, String AttributeName) {
        try {
            XPathExpression expr = xpath.compile(expression);
            // 我们的表达式可以帮我们获取节点
            Node targetNode = (Node) expr.evaluate(doc, XPathConstants.NODE);
            return targetNode.getAttributes().getNamedItem(AttributeName).getNodeValue();
        } catch (XPathExpressionException e) {
            log.error("An exception occurred while parsing the expression.", e);
        }
        return null;
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
}
