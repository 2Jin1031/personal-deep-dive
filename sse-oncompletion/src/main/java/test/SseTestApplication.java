package test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
@RestController
public class SseTestApplication {

    private final Map<String, SseEmitter> connections = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private int messageCounter = 0;

    public static void main(String[] args) {
        SpringApplication.run(SseTestApplication.class, args);
    }

    @GetMapping("/sse")
    public SseEmitter subscribe() {
        String clientId = "client-" + System.currentTimeMillis();
        SseEmitter emitter = new SseEmitter(300_000L); // 5분 타임아웃

        connections.put(clientId, emitter);
        log("✅ 새 연결 생성: " + clientId + " (총 연결 수: " + connections.size() + ")");

        emitter.onTimeout(() -> {
            log("⏰ TIMEOUT: " + clientId);
            connections.remove(clientId);
            log("   → 제거 후 총 연결 수: " + connections.size());
        });

        emitter.onError(e -> {
            log("❌ ERROR: " + clientId + " - " + e.getMessage());
            connections.remove(clientId);
            log("   → 제거 후 총 연결 수: " + connections.size());
        });

        emitter.onCompletion(() -> {
            log("🔚 COMPLETION: " + clientId);
            connections.remove(clientId);
            log("   → 제거 후 총 연결 수: " + connections.size());
        });

        try {
            emitter.send(SseEmitter.event()
                .name("connected")
                .data("Connected as " + clientId));
        } catch (IOException e) {
            log("❌ 초기 메시지 전송 실패: " + clientId);
        }

        return emitter;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
            "activeConnections", connections.size(),
            "clientIds", connections.keySet()
        );
    }

    public SseTestApplication() {
        // 2초마다 모든 연결에 메시지 전송
        executor.scheduleAtFixedRate(() -> {
            messageCounter++;
            String message = "Message #" + messageCounter + " at " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

            log("📤 메시지 전송 중... (수신자: " + connections.size() + "명)");

            connections.forEach((clientId, emitter) -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("message")
                        .data(message));
                } catch (IOException e) {
                    log("❌ 메시지 전송 실패: " + clientId);
                }
            });
        }, 2, 2, TimeUnit.SECONDS);
    }

    private void log(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.println("[" + timestamp + "] " + message);
    }
}