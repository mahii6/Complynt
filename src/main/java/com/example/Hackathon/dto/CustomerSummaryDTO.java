package com.example.Hackathon.dto;

import com.example.Hackathon.entity.Customer;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerSummaryDTO {
    private Long id;
    private String customerNumber;
    private String fullName;
    private String email;
    private String phone;
    private String accountNumber;

    /** Fake bank / passbook metadata for agent UI */
    private String bankName;
    private String branchName;
    private String ifscCode;
    private String accountType;
    private String availableBalanceDisplay;
    private String passbookLastPrinted;
    private String passbookDecorImageUrl;

    public static CustomerSummaryDTO fromEntity(Customer c) {
        if (c == null) {
            return null;
        }
        return CustomerSummaryDTO.builder()
                .id(c.getId())
                .customerNumber(c.getCustomerNumber())
                .fullName(c.getFullName())
                .email(c.getEmail())
                .phone(c.getPhone())
                .accountNumber(c.getAccountNumber())
                .bankName(c.getBankName())
                .branchName(c.getBranchName())
                .ifscCode(c.getIfscCode())
                .accountType(c.getAccountType())
                .availableBalanceDisplay(c.getAvailableBalanceDisplay())
                .passbookLastPrinted(c.getPassbookLastPrinted())
                .passbookDecorImageUrl(c.getPassbookDecorImageUrl())
                .build();
    }
}
