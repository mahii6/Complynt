package com.example.Hackathon.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentCreateDTO {
    private String content;
    private String authorName;
    private String authorRole;
    private Boolean isInternal;
}
