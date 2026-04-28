package com.example.Hackathon.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintCommentDTO {
    private Long id;
    private String content;
    private String authorName;
    private String authorRole;
    private Boolean isInternal;
    private LocalDateTime createdAt;
}
