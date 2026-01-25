package com.seating.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for report header settings
 */
@Configuration
@ConfigurationProperties(prefix = "report.header")
@Data
public class ReportConfig {

    private String line1 = "";
    private String line2 = "";
}
