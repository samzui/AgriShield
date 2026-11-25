package com.examly.springapp.service;
import com.examly.springapp.dto.request.PolicyRequests.EnrollRequest;
import com.examly.springapp.dto.response.PageResponse;
import com.examly.springapp.entity.Policy;
import com.examly.springapp.entity.User;
import com.examly.springapp.enums.PolicyStatus;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.repository.PolicyRepository;
import com.examly.springapp.repository.CropRepository;
import com.examly.springapp.repository.UserRepository;
import com.examly.springapp.util.SecurityUtil;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.LocalDate;
import java.util.List;
@Service
public class PolicyService {
    private final PolicyRepository repo;
    private final UserRepository users;
    private final CropRepository crops;
    private final AuditService audit;
    private final OperationalHoursService hours;
    public PolicyService(PolicyRepository repo,UserRepository users,CropRepository crops,AuditService audit,OperationalHoursService hours) {
        this.repo=repo;
        this.users=users;
        this.crops=crops;
        this.audit=audit;
        this.hours=hours;
    }
    @Transactional public Policy enroll(EnrollRequest r) {
        hours.assertOpen();
        User farmer=users.findById(r.farmerId()).orElseThrow(()->new ResourceNotFoundException("Farmer not found"));
        if(SecurityUtil.has("FARMER") && !farmer.getId().equals(currentUserId())) throw new com.examly.springapp.exception.UnauthorisedAccessException("Farmers can enroll only their own policy");
        crops.findByCropNameIgnoreCaseAndSeason(r.cropName(),r.season()).filter(c->c.isActive()).orElseThrow(()->new IllegalArgumentException("Crop is not configured for the selected season"));
        if(r.sownAreaHa().signum()<=0)throw new IllegalArgumentException("Insured area must be positive");
        if(r.sumInsured().signum()<=0)throw new IllegalArgumentException("Premium must be positive");
        BigDecimal base=r.sownAreaHa().multiply(r.sumInsured()).divide(BigDecimal.valueOf(100),2,RoundingMode.HALF_UP);
        Policy p=new Policy();
        p.setPolicyNumber("PMFBY-"+System.currentTimeMillis());
        p.setFarmer(farmer);
        p.setCropName(r.cropName().trim());
        p.setSeason(r.season());
        p.setSownAreaHa(r.sownAreaHa());
        p.setBankAccountNo(r.bankAccountNo().trim());
        if(r.bankHolderName()!=null)p.setBankHolderName(r.bankHolderName().trim());
        if(r.bankName()!=null)p.setBankName(r.bankName().trim());
        if(r.bankIfsc()!=null){String ifsc=r.bankIfsc().trim().toUpperCase();if(!ifsc.isEmpty()&&!ifsc.matches("[A-Z]{4}0[A-Z0-9]{6}"))throw new IllegalArgumentException("Invalid IFSC code (expected format: AAAA0XXXXXX)");p.setBankIfsc(ifsc);}
        if(r.bankBranch()!=null)p.setBankBranch(r.bankBranch().trim());
        p.setSumInsured(r.sumInsured());
        p.setPremiumFarmer(base.multiply(BigDecimal.valueOf(.02)).setScale(2,RoundingMode.HALF_UP));
        p.setPremiumState(base.multiply(BigDecimal.valueOf(.04)).setScale(2,RoundingMode.HALF_UP));
        p.setPremiumCentre(base.multiply(BigDecimal.valueOf(.04)).setScale(2,RoundingMode.HALF_UP));
        p.setStatus(PolicyStatus.ENROLLED);
        p.setEnrollmentDate(LocalDate.now());
        Policy saved=repo.save(p);
        audit.log("CREATE","POLICY",String.valueOf(saved.getId()),"SUCCESS",null,saved.getPolicyNumber(),"policy enrollment");
        return saved;
    }
    @Transactional(readOnly=true) public Policy get(Long id) {
        Policy p=repo.findById(id).orElseThrow(()->new ResourceNotFoundException("Policy not found"));
        assertAccess(p);
        audit.log("READ","POLICY",String.valueOf(id),"SUCCESS",null,null,"policy access");
        return p;
    }
    @Transactional(readOnly=true) public List<Policy> farmer(Long id) {
        if(!canSeeFarmer(id))throw new com.examly.springapp.exception.UnauthorisedAccessException("You can access only your own policies");
        return repo.findByFarmerId(id);
    }
    public PageResponse<Policy> list(int page,int size) {
        Page<Policy> p=repo.findAll(PageRequest.of(Math.max(0,page),Math.min(Math.max(1,size),100),Sort.by("id").descending()));
        return new PageResponse<>(p.getContent(),p.getNumber(),p.getSize(),p.getTotalElements(),p.getTotalPages(),p.isLast());
    }
    @Transactional public Policy updateStatus(Long id,PolicyStatus status) {
        Policy p=get(id);
        String old=p.getStatus().name();
        p.setStatus(status);
        Policy saved=repo.save(p);
        audit.log("STATUS_CHANGE","POLICY",String.valueOf(id),"SUCCESS",old,status.name(),"policy status");
        return saved;
    }
    @Transactional public void delete(Long id) {
        Policy p=get(id);
        p.setStatus(PolicyStatus.LAPSED);
        repo.save(p);
        audit.log("LAPSE","POLICY",String.valueOf(id),"SUCCESS",null,"LAPSED","policy lifecycle");
    }
    private void assertAccess(Policy p) {
        if(SecurityUtil.has("ADMIN")||SecurityUtil.has("INSURANCE_OFFICER")||SecurityUtil.has("STATE_OFFICER")||SecurityUtil.has("BANK_OFFICER"))return;
        Long current=currentUserId();
        if(current==null||!p.getFarmer().getId().equals(current))throw new com.examly.springapp.exception.UnauthorisedAccessException("You can access only your own policies");
    }
    private boolean canSeeFarmer(Long id) {
        if(SecurityUtil.has("ADMIN")||SecurityUtil.has("INSURANCE_OFFICER")||SecurityUtil.has("STATE_OFFICER")||SecurityUtil.has("BANK_OFFICER"))return true;
        Long current=currentUserId();
        return current!=null&&current.equals(id);
    }
    private Long currentUserId() {
        try {
            return users.findByUserId(SecurityUtil.username()).map(User::getId).orElse(null);
        }catch(Exception e) {
            return null;
        }
    }
}
