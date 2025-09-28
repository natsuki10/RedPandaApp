package com.example.redpandaapp.controller;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.util.UriUtils;

@Controller
public class PandasRedirectController {

    @Value("${app.asset-base}")
    private String assetBase;  // 例: https://storage.googleapis.com/redpandaapp-202509-assets

    @GetMapping("/pandas/{filename:.+}")
    public ResponseEntity<Void> redirect(@PathVariable String filename) {
        // ファイル名を URL エンコードして GCS にリダイレクト
        String enc = UriUtils.encodePathSegment(filename, StandardCharsets.UTF_8);
        URI to = URI.create(assetBase + "/pandas/" + enc);
        return ResponseEntity.status(HttpStatus.FOUND).location(to).build(); // 302
    }
}
