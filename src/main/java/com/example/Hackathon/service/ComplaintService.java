package com.example.Hackathon.service;

import com.example.Hackathon.dto.*;
import com.example.Hackathon.entity.*;
import com.example.Hackathon.enums.*;
import com.example.Hackathon.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ComplaintService {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private SlaService slaService;

    @Autowired
    private RoutingService routingService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private AiGatewayService aiGatewayService;

    @Autowired
    private AgentNotificationService agentNotificationService;

    @Autowired
    private EmailSenderService emailSenderService;

    private static final ObjectMapper JSON = new ObjectMapper();

    @Transactional
    public ComplaintResponseDTO create(ComplaintCreateDTO dto) {
        // 1. Find or create customer (Customer 360)
        Customer customer = ingestionService.findOrCreateCustomer(dto);

        // 2. Auto-assign severity if not provided (random for now, AI later)
        Severity severity = dto.getSeverity();
        if (severity == null) {
            Severity[] values = Severity.values();
            severity = values[new Random().nextInt(values.length)];
        }

        // 3. Build complaint — channel is always DASHBOARD
        Complaint complaint = Complaint.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .productType(dto.getProductType())
                .issueType(dto.getIssueType())
                .severity(severity)
                .channel(Channel.DASHBOARD)
                .status(ComplaintStatus.OPEN)
                .customer(customer)
                .regulatoryFlag(false)
                .isDuplicate(false)
                .build();

        // 3. Generate ticket number
        complaint.setTicketNumber(ingestionService.generateTicketNumber());

        // 4. Check for duplicates
        if (ingestionService.isDuplicate(customer, dto.getProductType())) {
            complaint.setIsDuplicate(true);
        }

        // 5. Save first to get createdAt via @PrePersist
        complaint = complaintRepository.save(complaint);

        // 6. Calculate SLA deadline
        complaint.setSlaDeadline(slaService.calculateDeadline(complaint));

        // 7. Auto-assign agent
        Complaint finalComplaint = complaint;
        routingService.autoAssign(complaint).ifPresent(agent -> {
            finalComplaint.setAssignedAgent(agent);
            finalComplaint.setStatus(ComplaintStatus.IN_PROGRESS);
        });

        complaint = complaintRepository.save(complaint);

        // 8. Audit log
        auditLogService.log(complaint, "Complaint created", "SYSTEM", null, "OPEN");

        return toResponseDTO(complaint, false);
    }

    public Page<ComplaintResponseDTO> getAll(ComplaintFilterDTO filter) {
        PageRequest pageRequest = PageRequest.of(
                filter.getPage(), filter.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        LocalDateTime fromDate = null;
        LocalDateTime toDate = null;
        if (filter.getFromDate() != null && !filter.getFromDate().isBlank()) {
            fromDate = LocalDate.parse(filter.getFromDate()).atStartOfDay();
        }
        if (filter.getToDate() != null && !filter.getToDate().isBlank()) {
            toDate = LocalDate.parse(filter.getToDate()).atTime(23, 59, 59);
        }

        Page<Complaint> page = complaintRepository.findWithFilters(
                filter.getStatus(),
                filter.getProductType(),
                filter.getSeverity(),
                filter.getAgentId(),
                fromDate,
                toDate,
                pageRequest);

        return page.map(c -> toResponseDTO(c, false));
    }

    @Transactional(readOnly = true)
    public ComplaintResponseDTO getById(Long id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found: " + id));
        return toResponseDTO(complaint, true);
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponseDTO> getEscalatedComplaints() {
        return complaintRepository.findByStatusOrderByCreatedAtDesc(ComplaintStatus.ESCALATED).stream()
                .map(c -> toResponseDTO(c, false))
                .collect(Collectors.toList());
    }

    @Transactional
    public ComplaintResponseDTO notifyCustomer(Long id, AgentNotifyDTO dto) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found: " + id));
        if (complaint.getCustomer() != null) {
            complaint.getCustomer().getPhone();
        }
        agentNotificationService.notifyCustomer(complaint, dto);
        return getById(id);
    }

    public Page<ComplaintResponseDTO> getByCustomer(Long customerId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return complaintRepository.findByCustomerId(customerId, pageRequest).map(c -> toResponseDTO(c, false));
    }

    public Page<ComplaintResponseDTO> getByAgent(Long agentId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return complaintRepository.findByAssignedAgentId(agentId, pageRequest).map(c -> toResponseDTO(c, false));
    }

    @Transactional
    public ComplaintResponseDTO updateStatus(Long id, StatusUpdateDTO dto, String updatedBy) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found: " + id));

        if (dto.getAssignToAgentId() != null) {
            Agent newAgent = agentRepository.findById(dto.getAssignToAgentId())
                    .orElseThrow(() -> new RuntimeException("Agent not found: " + dto.getAssignToAgentId()));
            boolean same = complaint.getAssignedAgent() != null
                    && complaint.getAssignedAgent().getId().equals(newAgent.getId());
            if (!same) {
                String oldAgent = complaint.getAssignedAgent() != null
                        ? complaint.getAssignedAgent().getName()
                        : "unassigned";
                complaint.setAssignedAgent(newAgent);
                auditLogService.log(complaint, "Case reassigned / escalated to agent", updatedBy, oldAgent, newAgent.getName());
            }
        }

        String oldStatus = complaint.getStatus().name();
        complaint.setStatus(dto.getStatus());

        if (dto.getStatus() == ComplaintStatus.RESOLVED || dto.getStatus() == ComplaintStatus.CLOSED) {
            complaint.setResolvedAt(LocalDateTime.now());
        }

        auditLogService.log(complaint, "Workflow stage updated", updatedBy, oldStatus, dto.getStatus().name());

        if (dto.getNote() != null && !dto.getNote().isBlank()) {
            ComplaintComment comment = ComplaintComment.builder()
                    .complaint(complaint)
                    .content(dto.getNote())
                    .authorName("Agent")
                    .authorRole("AGENT")
                    .isInternal(true)
                    .build();
            if (complaint.getComments() == null) {
                complaint.setComments(new ArrayList<>());
            }
            complaint.getComments().add(comment);
            String snippet = dto.getNote().length() > 500 ? dto.getNote().substring(0, 500) + "…" : dto.getNote();
            auditLogService.log(complaint, "Internal note recorded", updatedBy, null, snippet);
        }

        complaint = complaintRepository.save(complaint);

        if (Boolean.TRUE.equals(dto.getNotifyCustomer())
                && dto.getCustomerNotifyMessage() != null
                && !dto.getCustomerNotifyMessage().isBlank()) {
            try {
                agentNotificationService.notifyCustomer(complaint, AgentNotifyDTO.builder()
                        .channel(dto.getNotifyChannel() != null ? dto.getNotifyChannel() : "AUTO")
                        .message(dto.getCustomerNotifyMessage())
                        .messageType("OTHER")
                        .build());
            } catch (Exception e) {
                auditLogService.log(complaint,
                        "Customer notification failed: " + e.getMessage(),
                        updatedBy, null, null);
            }
        }

        return getById(id);
    }

    @Transactional
    public ComplaintResponseDTO assignAgent(Long id, Long agentId, String assignedBy) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found: " + id));
        Agent agent = agentRepository.findById(agentId)
                .orElseThrow(() -> new RuntimeException("Agent not found: " + agentId));

        String oldAgent = complaint.getAssignedAgent() != null ? complaint.getAssignedAgent().getName() : "none";
        complaint.setAssignedAgent(agent);

        if (complaint.getStatus() == ComplaintStatus.OPEN) {
            complaint.setStatus(ComplaintStatus.IN_PROGRESS);
        }

        auditLogService.log(complaint, "Assigned to " + agent.getName(), assignedBy, oldAgent, agent.getName());

        complaint = complaintRepository.save(complaint);
        return getById(id);
    }

    /**
     * Escalates the case (status {@link ComplaintStatus#ESCALATED}), assigns {@code targetAgentId},
     * records optional internal handoff note, and optionally emails the new assignee.
     */
    @Transactional
    public ComplaintResponseDTO escalateComplaint(Long id, EscalateComplaintDTO dto, String updatedBy) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found: " + id));
        Agent agent = agentRepository.findById(dto.getTargetAgentId())
                .orElseThrow(() -> new RuntimeException("Agent not found: " + dto.getTargetAgentId()));

        String oldStatus = complaint.getStatus().name();
        String oldAgent = complaint.getAssignedAgent() != null
                ? complaint.getAssignedAgent().getName()
                : "unassigned";
        complaint.setAssignedAgent(agent);
        complaint.setStatus(ComplaintStatus.ESCALATED);

        auditLogService.log(complaint, "Escalation: case assigned to agent", updatedBy, oldAgent, agent.getName());
        auditLogService.log(complaint, "Workflow stage updated", updatedBy, oldStatus, ComplaintStatus.ESCALATED.name());

        if (dto.getInternalNote() != null && !dto.getInternalNote().isBlank()) {
            ComplaintComment comment = ComplaintComment.builder()
                    .complaint(complaint)
                    .content("[Escalation handoff] " + dto.getInternalNote())
                    .authorName("Agent")
                    .authorRole("AGENT")
                    .isInternal(true)
                    .build();
            if (complaint.getComments() == null) {
                complaint.setComments(new ArrayList<>());
            }
            complaint.getComments().add(comment);
            String snippet = dto.getInternalNote().length() > 500
                    ? dto.getInternalNote().substring(0, 500) + "…"
                    : dto.getInternalNote();
            auditLogService.log(complaint, "Internal note (escalation handoff)", updatedBy, null, snippet);
        }

        complaint = complaintRepository.save(complaint);

        boolean notifyAssignee = dto.getNotifyAssignedAgent() == null || dto.getNotifyAssignedAgent();
        if (notifyAssignee && agent.getEmail() != null && !agent.getEmail().isBlank()) {
            try {
                String subj = "Escalated to you — " + complaint.getTicketNumber();
                String body = "A complaint has been escalated and is now assigned to you.\n\n"
                        + "Ticket: " + complaint.getTicketNumber() + "\n"
                        + "Title: " + complaint.getTitle() + "\n"
                        + "Customer: " + computeClientDisplayName(complaint) + "\n"
                        + "Status: ESCALATED\n\n"
                        + (dto.getInternalNote() != null && !dto.getInternalNote().isBlank()
                                ? "Handoff note:\n" + dto.getInternalNote() + "\n\n"
                                : "")
                        + "— Complynt";
                emailSenderService.sendAgentMessage(agent.getEmail().trim(), subj, body);
                auditLogService.log(complaint, "Assigned agent notified by email (escalation)", "AGENT", null, agent.getEmail());
            } catch (Exception e) {
                auditLogService.log(complaint,
                        "Escalation email to agent failed: " + e.getMessage(),
                        updatedBy, null, null);
            }
        }

        return getById(id);
    }

    @Transactional
    public ComplaintCommentDTO addComment(Long id, CommentCreateDTO dto) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found: " + id));

        ComplaintComment comment = ComplaintComment.builder()
                .complaint(complaint)
                .content(dto.getContent())
                .authorName(dto.getAuthorName())
                .authorRole(dto.getAuthorRole() != null ? dto.getAuthorRole() : "AGENT")
                .isInternal(dto.getIsInternal() != null ? dto.getIsInternal() : false)
                .build();

        if (complaint.getComments() == null) {
            complaint.setComments(new ArrayList<>());
        }
        complaint.getComments().add(comment);
        complaintRepository.save(complaint);

        return toCommentDTO(comment);
    }

    @Transactional
    public ComplaintResponseDTO classify(Long id) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found: " + id));

        AiClassificationDTO classification = aiGatewayService.classify(complaint.getDescription());

        // Apply product/issue/severity from AI
        try { complaint.setProductType(ProductType.valueOf(classification.getProductType())); }
        catch (Exception e) { /* keep existing */ }

        try { complaint.setIssueType(IssueType.valueOf(classification.getIssueType())); }
        catch (Exception e) { /* keep existing */ }

        try { complaint.setSeverity(Severity.valueOf(classification.getSeverity())); }
        catch (Exception e) { /* keep existing */ }

        // Apply AI metadata
        complaint.setSentimentLabel(classification.getSentimentLabel());
        complaint.setSentimentScore(classification.getSentimentScore());
        complaint.setAiConfidence(classification.getConfidence());
        complaint.setClassifiedBy(classification.getClassifiedBy());
        complaint.setCriticalScore(classification.getCriticalScore());
        complaint.setRegulatoryFlag(classification.getRegulatoryFlag());
        complaint.setAiSuggestedResponse(classification.getSuggestedResponse());
        complaint.setExtractedEntities(classification.getExtractedEntities());

        auditLogService.log(complaint, "AI classification applied (" + classification.getClassifiedBy() + ")", "SYSTEM", null,
                "Product: " + classification.getProductType() + " | Severity: " + classification.getSeverity() +
                " | Confidence: " + classification.getConfidence());

        complaint = complaintRepository.save(complaint);
        return toResponseDTO(complaint, true);
    }

    // =================== Mapping Helpers ===================

    private String computeClientDisplayName(Complaint c) {
        if (c.getCustomer() != null && c.getCustomer().getFullName() != null
                && !c.getCustomer().getFullName().isBlank()) {
            return c.getCustomer().getFullName();
        }
        if (c.getEmail() != null && !c.getEmail().isBlank()) {
            return c.getEmail();
        }
        if (c.getCustomer() != null && c.getCustomer().getPhone() != null
                && !c.getCustomer().getPhone().isBlank()) {
            return c.getCustomer().getPhone();
        }
        return "Anonymous";
    }

    private ComplaintResponseDTO toResponseDTO(Complaint c, boolean includeAudit) {
        SlaStatusDTO slaStatus = null;
        try {
            slaStatus = slaService.getStatus(c);
        } catch (Exception e) {
            // SLA rule may not exist yet
        }

        ComplaintResponseDTO.ComplaintResponseDTOBuilder b = ComplaintResponseDTO.builder()
                .id(c.getId())
                .ticketNumber(c.getTicketNumber())
                .title(c.getTitle())
                .description(c.getDescription())
                .productType(c.getProductType())
                .issueType(c.getIssueType())
                .status(c.getStatus())
                .severity(c.getSeverity())
                .channel(c.getChannel())
                .regulatoryFlag(c.getRegulatoryFlag())
                .isDuplicate(c.getIsDuplicate())
                .duplicateOfId(c.getDuplicateOfId())
                .sentimentLabel(c.getSentimentLabel())
                .sentimentScore(c.getSentimentScore())
                .aiConfidence(c.getAiConfidence())
                .classifiedBy(c.getClassifiedBy())
                .criticalScore(c.getCriticalScore())
                .aiSuggestedResponse(c.getAiSuggestedResponse())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .slaDeadline(c.getSlaDeadline())
                .resolvedAt(c.getResolvedAt())
                .contactEmail(c.getEmail())
                .clientDisplayName(computeClientDisplayName(c))
                .customer(c.getCustomer() != null ? toCustomerDTO(c.getCustomer()) : null)
                .assignedAgent(c.getAssignedAgent() != null ? toAgentDTO(c.getAssignedAgent()) : null)
                .slaStatus(slaStatus)
                .comments(c.getComments() != null
                        ? c.getComments().stream().map(this::toCommentDTO).collect(Collectors.toList())
                        : Collections.emptyList())
                .attachmentUrls(parseAttachmentUrls(c));

        if (includeAudit && c.getId() != null) {
            List<AuditLogDTO> logs = auditLogService.getLogsForComplaint(c.getId()).stream()
                    .sorted(Comparator.comparing(AuditLog::getTimestamp, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(this::toAuditLogDTO)
                    .collect(Collectors.toList());
            b.auditLogs(logs);
        }

        return b.build();
    }

    private List<String> parseAttachmentUrls(Complaint c) {
        if (c.getAttachmentUrls() == null || c.getAttachmentUrls().isBlank()) {
            return Collections.emptyList();
        }
        try {
            return JSON.readValue(c.getAttachmentUrls(), new TypeReference<List<String>>() { });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private AuditLogDTO toAuditLogDTO(AuditLog a) {
        return AuditLogDTO.builder()
                .id(a.getId())
                .action(a.getAction())
                .performedBy(a.getPerformedBy())
                .oldValue(a.getOldValue())
                .newValue(a.getNewValue())
                .timestamp(a.getTimestamp())
                .build();
    }

    private CustomerSummaryDTO toCustomerDTO(Customer c) {
        return CustomerSummaryDTO.fromEntity(c);
    }

    private AgentSummaryDTO toAgentDTO(Agent a) {
        return AgentSummaryDTO.builder()
                .id(a.getId())
                .name(a.getName())
                .email(a.getEmail())
                .role(a.getRole())
                .build();
    }

    private ComplaintCommentDTO toCommentDTO(ComplaintComment cc) {
        return ComplaintCommentDTO.builder()
                .id(cc.getId())
                .content(cc.getContent())
                .authorName(cc.getAuthorName())
                .authorRole(cc.getAuthorRole())
                .isInternal(cc.getIsInternal())
                .createdAt(cc.getCreatedAt())
                .build();
    }
}
