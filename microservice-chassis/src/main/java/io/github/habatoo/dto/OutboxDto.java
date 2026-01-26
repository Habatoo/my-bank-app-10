package io.github.habatoo.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class OutboxDto {
    private UUID id;
    private String eventType;
    private JsonNode payload;
    private String status;
    private LocalDateTime createdAt;
}
