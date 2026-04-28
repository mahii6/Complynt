package com.example.Hackathon.repository;

import com.example.Hackathon.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByPhone(String phone);
    Optional<Customer> findByAccountNumber(String accountNumber);
    Optional<Customer> findByCustomerNumber(String customerNumber);
    Optional<Customer> findByFullNameIgnoreCase(String fullName);
}
