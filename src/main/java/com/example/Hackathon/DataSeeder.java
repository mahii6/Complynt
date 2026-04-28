package com.example.Hackathon;

import com.example.Hackathon.entity.*;
import com.example.Hackathon.enums.*;
import com.example.Hackathon.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private AgentRepository agentRepository;

    @Autowired
    private SlaRuleRepository slaRuleRepository;

    @Autowired
    private EscalationRuleRepository escalationRuleRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (teamRepository.count() == 0) {
            seedTeamsAgentsAndRules();
        }
        if (complaintRepository.count() == 0) {
            seedDummyComplaints();
        }
    }

    private void seedTeamsAgentsAndRules() {
        Team ccTeam = teamRepository.save(Team.builder().name("Credit Card Team").category("CREDIT_CARD").build());
        Team loanTeam = teamRepository.save(Team.builder().name("Loan Team").category("HOME_LOAN").build());
        Team savingsTeam = teamRepository.save(Team.builder().name("Savings Team").category("SAVINGS_ACCOUNT").build());
        Team genTeam = teamRepository.save(Team.builder().name("General Team").category("OTHER").build());

        agentRepository.save(Agent.builder().name("Priya Sharma").email("priya@bank.com").role("AGENT").active(true).team(ccTeam).build());
        agentRepository.save(Agent.builder().name("Rahul Mehta").email("rahul@bank.com").role("AGENT").active(true).team(ccTeam).build());
        agentRepository.save(Agent.builder().name("Sneha Patel").email("sneha@bank.com").role("AGENT").active(true).team(loanTeam).build());
        agentRepository.save(Agent.builder().name("Vikram Nair").email("vikram@bank.com").role("SUPERVISOR").active(true).team(genTeam).build());
        agentRepository.save(Agent.builder().name("Anita Roy").email("anita@bank.com").role("AGENT").active(true).team(savingsTeam).build());

        slaRuleRepository.save(SlaRule.builder().severity("P1").slaHours(24).warnAtPercent(80).build());
        slaRuleRepository.save(SlaRule.builder().severity("P2").slaHours(72).warnAtPercent(80).build());
        slaRuleRepository.save(SlaRule.builder().severity("P3").slaHours(168).warnAtPercent(80).build());
        slaRuleRepository.save(SlaRule.builder().severity("P4").slaHours(720).warnAtPercent(80).build());

        escalationRuleRepository.save(EscalationRule.builder().severity("P1").triggerAtPercent(80).escalateTo("SUPERVISOR").notifyMethod("DASHBOARD_ALERT").build());
        escalationRuleRepository.save(EscalationRule.builder().severity("P1").triggerAtPercent(100).escalateTo("BRANCH_HEAD").notifyMethod("EMAIL").build());
        escalationRuleRepository.save(EscalationRule.builder().severity("P2").triggerAtPercent(80).escalateTo("SUPERVISOR").notifyMethod("DASHBOARD_ALERT").build());
        escalationRuleRepository.save(EscalationRule.builder().severity("P2").triggerAtPercent(100).escalateTo("BRANCH_HEAD").notifyMethod("DASHBOARD_ALERT").build());

        System.out.println("========================================");
        System.out.println("  Data Seeder: teams, agents, SLA & escalation rules");
        System.out.println("========================================");
    }

    /**
     * Demo complaints: SLA breached (past deadline), SLA on-track, and escalated cases for the SLA monitor UI.
     */
    private void seedDummyComplaints() {
        List<Agent> agents = agentRepository.findAll();
        if (agents.isEmpty()) {
            return;
        }

        Agent a0 = agents.get(0);
        Agent a1 = agents.size() > 1 ? agents.get(1) : a0;
        Agent a2 = agents.size() > 2 ? agents.get(2) : a1;
        Agent a3 = agents.size() > 3 ? agents.get(3) : a2;
        Agent a4 = agents.size() > 4 ? agents.get(4) : a3;

        Customer c1 = customerRepository.findByEmail("arjun.malhotra.work@gmail.com")
                .orElseGet(() -> customerRepository.save(Customer.builder()
                        .customerNumber("CUST-DEMO-0001")
                        .fullName("Arjun Malhotra")
                        .email("arjun.malhotra.work@gmail.com")
                        .phone("+919876543210")
                        .accountNumber("501002334455")
                        .bankName("Somaiya Demo Bank Ltd.")
                        .branchName("Fort — Main Banking, Mumbai")
                        .ifscCode("SDBK0002147")
                        .accountType("Savings Bank — Individual")
                        .availableBalanceDisplay("₹ 1,84,920.35")
                        .passbookLastPrinted("12-Mar-2025")
                        .passbookDecorImageUrl("https://images.unsplash.com/photo-1554224155-6726b3ff858f?w=400&q=70")
                        .build()));

        Customer c2 = customerRepository.findByEmail("priya.nair.personal@gmail.com")
                .orElseGet(() -> customerRepository.save(Customer.builder()
                        .customerNumber("CUST-DEMO-0002")
                        .fullName("Priya Nair")
                        .email("priya.nair.personal@gmail.com")
                        .phone("+919812345678")
                        .accountNumber("501002889901")
                        .bankName("Somaiya Demo Bank Ltd.")
                        .branchName("Andheri — Extension Counter")
                        .ifscCode("SDBK0009981")
                        .accountType("Savings Bank — Individual")
                        .availableBalanceDisplay("₹ 42,105.00")
                        .passbookLastPrinted("03-Mar-2025")
                        .passbookDecorImageUrl("https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?w=400&q=70")
                        .build()));

        int y = LocalDateTime.now().getYear();

        String imgsTxn = "[\"https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?w=800&q=80\",\"https://images.unsplash.com/photo-1563013544-824ae1b704d3?w=800&q=80\"]";
        String imgCode = "[\"https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=800&q=80\"]";

        // Escalated + past SLA (breached monitor)
        persistComplaint(Complaint.builder()
                .ticketNumber("CMP-" + y + "-910001")
                .title("Unauthorized debit on credit card")
                .description("Customer reports two unauthorized transactions totaling ₹12,400. Card blocked pending investigation.")
                .email(c1.getEmail())
                .providedCustomerId("UNKNOWN")
                .priority("P1")
                .status(ComplaintStatus.ESCALATED)
                .severity(Severity.P1)
                .productType(ProductType.CREDIT_CARD)
                .issueType(IssueType.TRANSACTION_DISPUTE)
                .channel(Channel.EMAIL)
                .customer(c1)
                .assignedAgent(a0)
                .attachmentUrls(imgsTxn)
                .regulatoryFlag(false)
                .isDuplicate(false)
                .classifiedBy("SEED")
                .aiConfidence(0.92)
                .criticalScore(9)
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now().minusDays(1))
                .slaDeadline(LocalDateTime.now().minusDays(4))
                .build());

        persistComplaint(Complaint.builder()
                .ticketNumber("CMP-" + y + "-910002")
                .title("Home loan EMI not applied — account frozen")
                .description("EMI deducted but not reflected; account shows overdue. Customer contacted branch twice.")
                .email(c2.getEmail())
                .providedCustomerId("UNKNOWN")
                .priority("P2")
                .status(ComplaintStatus.ESCALATED)
                .severity(Severity.P2)
                .productType(ProductType.HOME_LOAN)
                .issueType(IssueType.FEE_COMPLAINT)
                .channel(Channel.WHATSAPP)
                .customer(c2)
                .assignedAgent(a2)
                .attachmentUrls("[\"https://images.unsplash.com/photo-1450101499163-c8848c66ca85?w=800&q=80\"]")
                .regulatoryFlag(false)
                .isDuplicate(false)
                .classifiedBy("SEED")
                .aiConfidence(0.88)
                .criticalScore(7)
                .createdAt(LocalDateTime.now().minusDays(12))
                .updatedAt(LocalDateTime.now().minusDays(2))
                .slaDeadline(LocalDateTime.now().minusDays(9))
                .build());

        // Breached SLA (not yet escalated status — scheduler can still move these)
        persistComplaint(Complaint.builder()
                .ticketNumber("CMP-" + y + "-910003")
                .title("Net banking login failure for 72 hours")
                .description("Repeated errors on mobile app; unable to pay utility bills.")
                .email(c1.getEmail())
                .providedCustomerId("UNKNOWN")
                .priority("P2")
                .status(ComplaintStatus.IN_PROGRESS)
                .severity(Severity.P2)
                .productType(ProductType.NET_BANKING)
                .issueType(IssueType.SERVICE_OUTAGE)
                .channel(Channel.EMAIL)
                .customer(c1)
                .assignedAgent(a1)
                .attachmentUrls(imgCode)
                .regulatoryFlag(false)
                .isDuplicate(false)
                .classifiedBy("SEED")
                .aiConfidence(0.85)
                .criticalScore(6)
                .createdAt(LocalDateTime.now().minusDays(6))
                .updatedAt(LocalDateTime.now().minusHours(2))
                .slaDeadline(LocalDateTime.now().minusDays(3))
                .build());

        persistComplaint(Complaint.builder()
                .ticketNumber("CMP-" + y + "-910004")
                .title("ATM cash not dispensed — amount debited")
                .description("₹10,000 debited, no cash. No receipt generated at ATM.")
                .email(c2.getEmail())
                .providedCustomerId("UNKNOWN")
                .priority("P1")
                .status(ComplaintStatus.OPEN)
                .severity(Severity.P1)
                .productType(ProductType.DEBIT_CARD)
                .issueType(IssueType.TRANSACTION_DISPUTE)
                .channel(Channel.APP)
                .customer(c2)
                .assignedAgent(a3)
                .regulatoryFlag(false)
                .isDuplicate(false)
                .classifiedBy("SEED")
                .aiConfidence(0.9)
                .criticalScore(8)
                .createdAt(LocalDateTime.now().minusDays(2))
                .updatedAt(LocalDateTime.now().minusHours(5))
                .slaDeadline(LocalDateTime.now().minusDays(1))
                .build());

        // Within SLA
        persistComplaint(Complaint.builder()
                .ticketNumber("CMP-" + y + "-910005")
                .title("Passbook update delay")
                .description("Passbook not updated for last 3 visits to branch.")
                .email(c1.getEmail())
                .providedCustomerId("UNKNOWN")
                .priority("P3")
                .status(ComplaintStatus.IN_PROGRESS)
                .severity(Severity.P3)
                .productType(ProductType.SAVINGS_ACCOUNT)
                .issueType(IssueType.GENERAL_INQUIRY)
                .channel(Channel.DASHBOARD)
                .customer(c1)
                .assignedAgent(a4)
                .regulatoryFlag(false)
                .isDuplicate(false)
                .classifiedBy("SEED")
                .aiConfidence(0.75)
                .criticalScore(4)
                .createdAt(LocalDateTime.now().minusHours(6))
                .updatedAt(LocalDateTime.now().minusHours(1))
                .slaDeadline(LocalDateTime.now().plusDays(6))
                .build());

        persistComplaint(Complaint.builder()
                .ticketNumber("CMP-" + y + "-910006")
                .title("KYC document resubmission")
                .description("Aadhaar masked copy rejected; need clarity on acceptable format.")
                .email(c2.getEmail())
                .providedCustomerId("UNKNOWN")
                .priority("P4")
                .status(ComplaintStatus.IN_PROGRESS)
                .severity(Severity.P4)
                .productType(ProductType.SAVINGS_ACCOUNT)
                .issueType(IssueType.KYC_ISSUE)
                .channel(Channel.EMAIL)
                .customer(c2)
                .assignedAgent(a2)
                .attachmentUrls("[\"https://images.unsplash.com/photo-1586281380349-632531db7ed4?w=800&q=80\"]")
                .regulatoryFlag(false)
                .isDuplicate(false)
                .classifiedBy("SEED")
                .aiConfidence(0.7)
                .criticalScore(3)
                .createdAt(LocalDateTime.now().minusHours(12))
                .updatedAt(LocalDateTime.now().minusHours(2))
                .slaDeadline(LocalDateTime.now().plusDays(25))
                .build());

        // Resolved (not in breach list once resolved)
        persistComplaint(Complaint.builder()
                .ticketNumber("CMP-" + y + "-910007")
                .title("Duplicate charge reversed")
                .description("Bank confirmed reversal within SLA.")
                .email(c1.getEmail())
                .providedCustomerId("UNKNOWN")
                .priority("P3")
                .status(ComplaintStatus.RESOLVED)
                .severity(Severity.P3)
                .productType(ProductType.CREDIT_CARD)
                .issueType(IssueType.TRANSACTION_DISPUTE)
                .channel(Channel.EMAIL)
                .customer(c1)
                .assignedAgent(a3)
                .regulatoryFlag(false)
                .isDuplicate(false)
                .classifiedBy("SEED")
                .aiConfidence(0.8)
                .criticalScore(4)
                .createdAt(LocalDateTime.now().minusDays(20))
                .updatedAt(LocalDateTime.now().minusDays(5))
                .slaDeadline(LocalDateTime.now().minusDays(10))
                .resolvedAt(LocalDateTime.now().minusDays(5))
                .build());

        System.out.println("========================================");
        System.out.println("  Data Seeder: dummy complaints (breached SLA + escalated + on-track)");
        System.out.println("========================================");
    }

    private void persistComplaint(Complaint c) {
        complaintRepository.save(c);
    }
}
