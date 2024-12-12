package com.CodeEvalCrew.AutoScore.controllers;

import java.time.LocalDateTime;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.CodeEvalCrew.AutoScore.models.Entity.Enum.GradingStatusEnum;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@RestController
@CrossOrigin(origins = "http://localhost:5173/")
public class SSEController {
// Tạo Sink để lưu trữ các sự kiện
    private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();

    // Endpoint để client lắng nghe
    @GetMapping(value = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamEvents() {
        return sink.asFlux(); // Trả về các sự kiện từ Sink
    }

    // Phương thức gửi JSON dạng message
    public void pushGradingProcess(Long processId, GradingStatusEnum status, LocalDateTime time, Long examPaperId) {
        // Tạo đối tượng JSON dưới dạng chuỗi
        String jsonMessage = """
        {
            "processId": %d,
            "status": "%s",
            "startDate": "%s",
            "updateDate": "%s",
            "examPaperId": "%d"
        }
        """.formatted(
            processId,
            status,
            time.toString(),
            LocalDateTime.now().toString(),
            examPaperId
        );

        sink.tryEmitNext(jsonMessage); // Gửi sự kiện
    }
}
