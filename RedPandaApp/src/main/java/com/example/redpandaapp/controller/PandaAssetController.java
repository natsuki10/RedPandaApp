package com.example.redpandaapp.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.redpandaapp.config.StorageConfig;

@Controller
public class PandaAssetController {

    private final StorageConfig storageConfig;

    public PandaAssetController(StorageConfig storageConfig) {
        this.storageConfig = storageConfig;
    }

    // /pandas/以下は全部 GCS へリダイレクト（サブフォルダもOK）
    @GetMapping("/pandas/{path:**}")
    public ResponseEntity<Void> redirectToGcs(@PathVariable("path") String path) {
        String bucket = storageConfig.getAssetBucket();
        // 例: https://storage.googleapis.com/<bucket>/<path>
        String gcsBase = "https://storage.googleapis.com/" + bucket;

        // pathSegment に渡すと適切にエンコードされる（日本語/スペース対応）
        UriComponentsBuilder b = UriComponentsBuilder.fromUriString(gcsBase);
        for (String seg : path.split("/")) {
            if (!seg.isEmpty()) b.pathSegment(seg);
        }
        return ResponseEntity.status(302).location(b.build().toUri()).build();
    }
}
