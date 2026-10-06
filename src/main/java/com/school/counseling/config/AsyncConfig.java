package com.school.counseling.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Cấu hình ThreadPool bất đồng bộ tách biệt hoàn toàn cho từng nhóm tác vụ:
 * - mailTaskExecutor:      Gửi Email OTP và thông báo Ticket
 * - aiTaskExecutor:        Xử lý câu hỏi real-time với Gemini AI (Non-Blocking)
 * - aiIngestionExecutor:   Parse PDF / băm chunk / sinh Vector embedding (Background)
 *
 * Nguyên tắc cốt lõi: Ngăn chặn Tomcat Thread Starvation khi AI bị lag 1-3s/request.
 * Khi 100 SV hỏi AI đồng thời mà không tách pool → 200 Tomcat workers đều bị block →
 * toàn bộ API khác (Ticket, Feed, Auth) không còn thread để phục vụ → web sập.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Pool dành riêng gửi Email OTP & thông báo Ticket.
     * CallerRunsPolicy: khi queue đầy → thread gọi tự xử lý, tránh RejectedExecutionException làm
     * rollback transaction tạo Ticket của sinh viên.
     */
    @Bean(name = "mailTaskExecutor")
    public Executor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(15);
        executor.setQueueCapacity(2000);   // Nâng từ 100 → 2000 để chịu spike 1.000 users tạo ticket
        executor.setThreadNamePrefix("mail-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("[AsyncConfig] mailTaskExecutor khởi tạo: core=5, max=15, queue=2000");
        return executor;
    }

    /**
     * Pool dành riêng xử lý câu hỏi Chatbot AI real-time (Gemini API 1-3s/request).
     * Non-blocking: RagChatRestController trả về CompletableFuture, thread Tomcat được giải phóng
     * ngay lập tức sau khi giao tác vụ vào pool này.
     * Hard Timeout 8s được cấu hình tại tầng Client HTTP (GeminiApiClient).
     */
    @Bean(name = "aiTaskExecutor")
    public Executor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(30);
        executor.setQueueCapacity(200);    // Bộ đệm 200 request đang chờ xử lý
        executor.setThreadNamePrefix("ai-chat-");
        // CallerRunsPolicy: khi pool đầy → fallback sang thread Tomcat nhưng chỉ xảy ra cực hiếm
        // vì queue=200 đã đủ buffer cho spike load thông thường
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(false); // Không chờ AI khi shutdown
        executor.initialize();
        log.info("[AsyncConfig] aiTaskExecutor khởi tạo: core=10, max=30, queue=200");
        return executor;
    }

    /**
     * Pool dành riêng tác vụ nền: parse PDF, băm chunk, sinh Vector embedding hàng loạt.
     * Tốc độ thấp không quan trọng, ưu tiên không chiếm tài nguyên của người dùng real-time.
     */
    @Bean(name = "aiIngestionExecutor")
    public Executor aiIngestionExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("ai-ingest-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60); // Chờ hoàn thành ingestion khi shutdown
        executor.initialize();
        log.info("[AsyncConfig] aiIngestionExecutor khởi tạo: core=2, max=5, queue=50");
        return executor;
    }
}
