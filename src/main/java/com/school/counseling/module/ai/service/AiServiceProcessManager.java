package com.school.counseling.module.ai.service;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;

/**
 * Tự động quản lý tiến trình ai-service (Python FastAPI RAG Microservice)
 * - Khởi động tự động khi Spring Boot sẵn sàng (ApplicationReadyEvent)
 * - Tự động tắt tiến trình khi Spring Boot dừng (@PreDestroy)
 */
@Component
@Slf4j
public class AiServiceProcessManager {

    @Value("${app.ai.microservice.auto-start:true}")
    private boolean autoStart;

    @Value("${app.ai.microservice.port:8000}")
    private int port;

    @Value("${app.ai.microservice.host:127.0.0.1}")
    private String host;

    private Process pythonProcess;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!autoStart) {
            log.info("ℹ️ AI Microservice auto-start bị vô hiệu hóa bởi cấu hình.");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                if (isPortListening(host, port)) {
                    log.info("✅ AI Microservice (FastAPI RAG) đang hoạt động sẵn tại {}:{}", host, port);
                    return;
                }

                log.info("🚀 Đang tự động khởi chạy AI Microservice (Python FastAPI) tại cổng {}...", port);
                startAiService();
            } catch (Exception e) {
                log.error("⚠️ Không thể tự động khởi động AI Microservice: {}", e.getMessage());
            }
        });
    }

    private synchronized void startAiService() {
        try {
            File workspaceDir = new File(System.getProperty("user.dir"));
            File aiServiceDir = new File(workspaceDir, "ai-service");
            if (!aiServiceDir.exists() || !aiServiceDir.isDirectory()) {
                log.warn("⚠️ Không tìm thấy thư mục ai-service tại {}", aiServiceDir.getAbsolutePath());
                return;
            }

            File logDir = new File(aiServiceDir, "logs");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            File logFile = new File(logDir, "uvicorn_autostart.log");

            // [NEW_CODE]: Tự động nhận diện môi trường ảo .venv chứa đầy đủ thư viện của ai-service
            File venvPythonWin = new File(aiServiceDir, ".venv\\Scripts\\python.exe");
            File venvPythonUnix = new File(aiServiceDir, ".venv/bin/python");
            String pythonExecutable = "python";
            if (venvPythonWin.exists()) {
                pythonExecutable = venvPythonWin.getAbsolutePath();
            } else if (venvPythonUnix.exists()) {
                pythonExecutable = venvPythonUnix.getAbsolutePath();
            }

            // [LEGACY_CODE - ROLLBACK IF NEEDED]:
            // ProcessBuilder pb = new ProcessBuilder(
            //         "python", "-m", "uvicorn", "api.app:app",
            //         "--host", host,
            //         "--port", String.valueOf(port)
            // );
            ProcessBuilder pb = new ProcessBuilder(
                    pythonExecutable, "-m", "uvicorn", "api.app:app",
                    "--host", host,
                    "--port", String.valueOf(port)
            );
            pb.directory(aiServiceDir);
            pb.environment().put("PYTHONIOENCODING", "utf-8");
            pb.environment().put("PYTHONUTF8", "1");
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));

            this.pythonProcess = pb.start();
            log.info("🌟 AI Microservice đã được kích hoạt (PID: {}). Log: {}", 
                    pythonProcess.pid(), logFile.getName());

            // Đợi kiểm tra cổng mở
            for (int i = 0; i < 20; i++) {
                Thread.sleep(1000);
                if (isPortListening(host, port)) {
                    log.info("🎉 AI Microservice đã sẵn sàng phục vụ tại http://{}:{}/api/chat!", host, port);
                    return;
                }
            }
            log.warn("⚠️ AI Microservice đã kích hoạt nhưng chưa mở cổng {} sau 20s.", port);
        } catch (IOException | InterruptedException e) {
            log.error("❌ Lỗi khi khởi chạy tiến trình Python ai-service: {}", e.getMessage());
        }
    }

    private boolean isPortListening(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 800);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @PreDestroy
    public synchronized void stopAiService() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            log.info("🛑 Đang dừng tiến trình AI Microservice (PID: {})...", pythonProcess.pid());
            try {
                String os = System.getProperty("os.name", "").toLowerCase();
                if (os.contains("win")) {
                    Runtime.getRuntime().exec(new String[]{"taskkill", "/F", "/T", "/PID", String.valueOf(pythonProcess.pid())});
                } else {
                    pythonProcess.destroy();
                }
            } catch (Exception e) {
                pythonProcess.destroyForcibly();
            }
            log.info("✅ Đã dừng AI Microservice.");
        }
    }
}
