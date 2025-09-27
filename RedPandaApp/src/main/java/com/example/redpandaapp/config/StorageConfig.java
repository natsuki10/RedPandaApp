package com.example.redpandaapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StorageConfig {

    @Value("${app.asset-bucket}")
    private String assetBucket;

    public String getAssetBucket() {
        return assetBucket;
    }
}
