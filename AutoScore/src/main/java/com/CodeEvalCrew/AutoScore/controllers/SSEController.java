package com.CodeEvalCrew.AutoScore.controllers;

import java.time.LocalDateTime;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@RestController
@CrossOrigin(origins = "https://autoscore.io.vn")
public class SSEController {
// Tạo Sink để lưu trữ các sự kiện
    private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

    // Endpoint để client lắng nghe
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamEvents() {
        return sink.asFlux(); // Trả về các sự kiện từ Sink
    }

    // Phương thức gửi JSON dạng message
    public void pushEvent(Long processId, String status, int successProcess, int totalProcess, LocalDateTime start) {
        // Tạo đối tượng JSON dưới dạng chuỗi
        String jsonMessage = """
        {
            "processId": %d,
            "status": "%s",
            "successProcess": %d,
            "totalProcess": %d,
            "startDate": "%s",
            "updateDate": "%s"
        }
        """.formatted(
            processId,
            status,
            successProcess,
            totalProcess,
            start.toString(),
            LocalDateTime.now().toString()
        );

        sink.tryEmitNext(jsonMessage); // Gửi sự kiện
    }
}
