package com.examly.springapp.service;
import com.examly.springapp.entity.ComplianceDeadline;
import com.examly.springapp.repository.ComplianceDeadlineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
@Service public class ComplianceService {
    private final ComplianceDeadlineRepository repo;
    private final AuditService audit;
    public ComplianceService(ComplianceDeadlineRepository repo,AuditService audit) {
        this.repo=repo;
        this.audit=audit;
    }
    @Transactional public ComplianceDeadline create(ComplianceDeadline d) {
        ComplianceDeadline saved=repo.save(d);
        audit.log("CREATE","COMPLIANCE",String.valueOf(saved.getId()),"SUCCESS",null,saved.getTitle(),"compliance");
        return saved;
    }
    @Transactional public ComplianceDeadline complete(Long id) {
        ComplianceDeadline d=repo.findById(id).orElseThrow(()->new IllegalArgumentException("Compliance deadline not found"));
        d.setCompleted(true);
        return repo.save(d);
    }
    public List<ComplianceDeadline> upcoming(int days) {
        return repo.findByDueDateBetween(LocalDate.now(),LocalDate.now().plusDays(Math.max(1,days)));
    }
}
