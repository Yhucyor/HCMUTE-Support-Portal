package com.school.counseling.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;

/**
 * Cấu hình Caffeine L1 In-Memory Cache hiệu năng cao cho Spring Boot
 * Giúp giảm thiểu tối đa các truy vấn lặp đi lặp lại tới Cloud Database (TiDB Tokyo)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_DEPARTMENTS_ACTIVE = "departments_active";
    public static final String CACHE_FAQS_ALL_ACTIVE = "faqs_all_active";
    public static final String CACHE_USER_TICKET_COUNTS = "user_ticket_counts";
    public static final String CACHE_SYSTEM_STATS = "system_stats";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofMinutes(30))
                .recordStats());
        cacheManager.setCacheNames(Arrays.asList(
                CACHE_DEPARTMENTS_ACTIVE,
                CACHE_FAQS_ALL_ACTIVE,
                CACHE_USER_TICKET_COUNTS,
                CACHE_SYSTEM_STATS
        ));
        return cacheManager;
    }
}
