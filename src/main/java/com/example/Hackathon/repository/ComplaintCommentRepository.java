package com.example.Hackathon.repository;

import com.example.Hackathon.entity.ComplaintComment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplaintCommentRepository extends JpaRepository<ComplaintComment, Long> {
    List<ComplaintComment> findByComplaintIdOrderByCreatedAtDesc(Long complaintId);
}
