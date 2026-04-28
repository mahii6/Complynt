package com.example.Hackathon.controller;

import com.example.Hackathon.dto.ComplaintResponseDTO;
import com.example.Hackathon.dto.CustomerSummaryDTO;
import com.example.Hackathon.entity.Customer;
import com.example.Hackathon.repository.CustomerRepository;
import com.example.Hackathon.service.ComplaintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin("*")
public class CustomerController {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ComplaintService complaintService;

    @GetMapping
    public ResponseEntity<List<CustomerSummaryDTO>> getAll() {
        List<CustomerSummaryDTO> customers = customerRepository.findAll().stream()
                .map(CustomerSummaryDTO::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerSummaryDTO> getById(@PathVariable Long id) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found: " + id));
        return ResponseEntity.ok(CustomerSummaryDTO.fromEntity(c));
    }

    @GetMapping("/{id}/complaints")
    public ResponseEntity<Page<ComplaintResponseDTO>> getComplaints(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(complaintService.getByCustomer(id, page, size));
    }
}
