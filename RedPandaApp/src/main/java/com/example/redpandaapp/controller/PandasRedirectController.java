// src/main/java/com/example/redpandaapp/controller/PandasRedirectController.java
package com.example.redpandaapp.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PandasRedirectController {

    @Value("${app.asset-base}")
    private String assetBase;

    @GetMapping("/pandas/{filename:.+}")
    public String redirect(@PathVariable String filename) {
        // パスセグメント用にエンコード（スペース→%20）
        String enc = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        String target = assetBase + "/pandas/" + enc;
        return "redirect:" + target;
    }
}
