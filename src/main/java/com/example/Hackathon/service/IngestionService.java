package com.example.Hackathon.service;

import com.example.Hackathon.dto.ComplaintCreateDTO;
import com.example.Hackathon.entity.Customer;
import com.example.Hackathon.enums.ProductType;
import com.example.Hackathon.repository.ComplaintRepository;
import com.example.Hackathon.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class IngestionService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    public Customer findOrCreateCustomer(ComplaintCreateDTO dto) {
        // 1. Try by email
        var byEmail = customerRepository.findByEmail(dto.getCustomerEmail());
        if (byEmail.isPresent()) return byEmail.get();

        // 2. Try by phone
        var byPhone = customerRepository.findByPhone(dto.getCustomerPhone());
        if (byPhone.isPresent()) return byPhone.get();

        // 3. Try by account number
        if (dto.getAccountNumber() != null && !dto.getAccountNumber().isBlank()) {
            var byAccount = customerRepository.findByAccountNumber(dto.getAccountNumber());
            if (byAccount.isPresent()) return byAccount.get();
        }

        // 4. Create new customer
        Customer customer = Customer.builder()
                .fullName(dto.getCustomerName())
                .email(dto.getCustomerEmail())
                .phone(dto.getCustomerPhone())
                .accountNumber(dto.getAccountNumber())
                .build();
        customer = customerRepository.save(customer);

        // Generate customer number
        customer.setCustomerNumber("CUST-" + String.format("%04d", customer.getId()));
        return customerRepository.save(customer);
    }

    public boolean isDuplicate(Customer customer, ProductType product) {
        var recent = complaintRepository.findByCustomerIdAndProductTypeAndCreatedAtAfter(
                customer.getId(), product, LocalDateTime.now().minusHours(48));
        return !recent.isEmpty();
    }

    public String generateTicketNumber() {
        long count = complaintRepository.count();
        int year = LocalDateTime.now().getYear();
        return "CMP-" + year + "-" + String.format("%06d", count + 1);
    }
}
