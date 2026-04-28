package com.example.Hackathon.dto;

import com.example.Hackathon.enums.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintFilterDTO {
    private ComplaintStatus status;
    private ProductType productType;
    private Severity severity;
    private Long agentId;
    private String fromDate;
    private String toDate;
    private int page = 0;
    private int size = 20;
}
