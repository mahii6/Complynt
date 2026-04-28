package com.example.Hackathon.dto;

import com.example.Hackathon.enums.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintCreateDTO {

    @NotBlank
    private String title;

    @NotBlank
    @Size(min = 10, message = "Description must be at least 10 characters")
    private String description;

    @NotNull
    private ProductType productType;

    @NotNull
    private IssueType issueType;

    private Severity severity; // Optional — auto-assigned if null

    // Channel removed — always DASHBOARD (set by backend)

    // Customer info
    @NotBlank
    private String customerName;

    @NotBlank
    @Email
    private String customerEmail;

    @NotBlank
    private String customerPhone;

    private String accountNumber;
}
