package com.practice.apiservice.metrics;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

@Component
public class AppInfoContributor implements InfoContributor {
    @Override public void contribute(Info.Builder builder) {
        builder.withDetail("app",
                java.util.Map.of("name", "dataflowhub-api", "version", "F2"));
    }
}
