package com.lostfound.controller;

import com.lostfound.service.WxService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wx")
public class WxController {

    private final WxService wxService;

    public WxController(WxService wxService) {
        this.wxService = wxService;
    }

    @ResponseBody
    @GetMapping(value = "/callback", produces = "text/plain;charset=UTF-8")
    public String verify(
            @RequestParam String signature,
            @RequestParam String timestamp,
            @RequestParam String nonce,
            @RequestParam String echostr) {
        return wxService.verify(signature, timestamp, nonce, echostr);
    }

    @ResponseBody
    @PostMapping(value = "/callback", produces = "application/xml;charset=UTF-8")
    public String handleMessage(@RequestBody String xmlBody) {
        return wxService.handleMessage(xmlBody);
    }
}
