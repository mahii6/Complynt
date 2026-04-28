package com.example.Hackathon.service;

import com.example.Hackathon.entity.Complaint;
import com.example.Hackathon.repository.ComplaintRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class ReportingService {

    @Autowired
    private ComplaintRepository complaintRepository;

    public byte[] exportComplaintsCsv(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.atTime(LocalTime.MAX);

        List<Complaint> complaints = complaintRepository.findByDateRange(fromDt, toDt);

        try {
            StringWriter writer = new StringWriter();
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader("TicketNumber", "CustomerName", "Phone", "Email",
                            "AccountNumber", "Product", "IssueType", "Severity",
                            "Status", "Channel", "RegulatoryFlag", "Sentiment",
                            "CreatedAt", "SLADeadline", "ResolvedAt", "AssignedAgent")
                    .build();

            CSVPrinter printer = new CSVPrinter(writer, format);

            for (Complaint c : complaints) {
                printer.printRecord(
                        c.getTicketNumber(),
                        c.getCustomer() != null ? c.getCustomer().getFullName() : "",
                        c.getCustomer() != null ? c.getCustomer().getPhone() : "",
                        c.getCustomer() != null ? c.getCustomer().getEmail() : "",
                        c.getCustomer() != null ? c.getCustomer().getAccountNumber() : "",
                        c.getProductType(),
                        c.getIssueType(),
                        c.getSeverity(),
                        c.getStatus(),
                        c.getChannel(),
                        c.getRegulatoryFlag(),
                        c.getSentimentLabel(),
                        c.getCreatedAt(),
                        c.getSlaDeadline(),
                        c.getResolvedAt(),
                        c.getAssignedAgent() != null ? c.getAssignedAgent().getName() : ""
                );
            }

            printer.flush();
            return writer.toString().getBytes();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CSV report", e);
        }
    }
}
