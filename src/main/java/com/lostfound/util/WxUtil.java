package com.lostfound.util;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public final class WxUtil {

    private WxUtil() {}

    public static boolean verifySha1(String token, String timestamp, String nonce, String signature) {
        if (token == null || timestamp == null || nonce == null || signature == null) {
            return false;
        }
        try {
            String[] values = new String[] {token, timestamp, nonce};
            Arrays.sort(values);
            String payload = String.join("", values);
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().equals(signature);
        } catch (Exception ex) {
            return false;
        }
    }

    public static Map<String, String> parseXml(String xmlBody) {
        if (xmlBody == null || xmlBody.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(xmlBody)));
            Element root = document.getDocumentElement();
            NodeList childNodes = root.getChildNodes();
            Map<String, String> result = new HashMap<>();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    result.put(node.getNodeName(), node.getTextContent());
                }
            }
            return result;
        } catch (Exception ex) {
            return Collections.emptyMap();
        }
    }
}
