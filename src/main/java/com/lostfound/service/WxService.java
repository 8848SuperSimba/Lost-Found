package com.lostfound.service;

public interface WxService {

    String verify(String signature, String timestamp, String nonce, String echostr);

    String handleMessage(String xmlBody);
}
