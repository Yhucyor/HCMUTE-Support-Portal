package com.school.counseling.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudinaryConfig {

    @Value("${app.cloudinary.cloud-name:db9j7uhdf}")
    private String cloudName;

    @Value("${app.cloudinary.api-key:818745529863945}")
    private String apiKey;

    @Value("${app.cloudinary.api-secret:nw79tJ47LQNbD6L0ZR38wJU6awo}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }
}
