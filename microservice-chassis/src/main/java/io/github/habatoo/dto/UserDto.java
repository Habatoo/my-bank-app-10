package io.github.habatoo.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class UserDto {
    private UUID id;
    private String login;
    private String name;
    private LocalDate birthDate;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
