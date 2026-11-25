package com.examly.springapp.service;
import com.examly.springapp.entity.*;
import com.examly.springapp.enums.WorkflowStatus;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.exception.UnauthorisedAccessException;
import com.examly.springapp.repository.*;
import com.examly.springapp.util.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
@Service
public class WorkflowService {
    private final WorkflowRepository workflows;
    private final WorkflowStageRepository stages;
    private final AuditService audit;
    private final UserRepository users;
    private final ClaimRepository claims;
    private final PolicyRepository policies;
    private final LossNotificationRepository losses;
    private final SurveyAssignmentRepository surveys;
    public WorkflowService(WorkflowRepository workflows,WorkflowStageRepository stages,AuditService audit,UserRepository users,ClaimRepository claims,PolicyRepository policies,LossNotificationRepository losses,SurveyAssignmentRepository surveys) {
        this.workflows=workflows;
        this.stages=stages;
        this.audit=audit;
        this.users=users;
        this.claims=claims;
        this.policies=policies;
        this.losses=losses;
        this.surveys=surveys;
    }
    @Transactional public Workflow start(String type,String entityType,Long entityId,List<StageSpec> specs) {
        if(specs==null||specs.isEmpty())throw new IllegalArgumentException("At least one workflow stage is required");
        Workflow w=new Workflow();
        w.setWorkflowType(type);
        w.setEntityType(entityType);
        w.setEntityId(entityId);
        w.setStatus(WorkflowStatus.IN_PROGRESS);
        w.setCurrentStage(1);
        Workflow saved=workflows.save(w);
        for(int i=0;i<specs.size();i++) {
            StageSpec spec=specs.get(i);
            WorkflowStage stage=new WorkflowStage();
            stage.setWorkflow(saved);
            stage.setStageOrder(i+1);
            stage.setStageName(spec.name());
            stage.setRequiredRole(spec.role());
            stage.setSlaHours(spec.slaHours());
            stage.setCompleted(false);
            stages.save(stage);
        }audit.log("WORKFLOW_START","WORKFLOW",String.valueOf(saved.getId()),"SUCCESS",null,type,"workflow");
        return saved;
    }
    public List<WorkflowStage> stages(Long workflowId) {
        return stages.findAll().stream().filter(s->s.getWorkflow().getId().equals(workflowId)).sorted(Comparator.comparingInt(WorkflowStage::getStageOrder)).toList();
    }

    // Advances the workflow past its current stage. Only the exact role a stage is assigned to may complete
    // it — Admin gets no blanket override here, since a stage like claim approval carries the same
    // Insurance-Officer-only rule enforced on the Claims page itself; Admin can only complete stages that are
    // themselves assigned to ADMIN (e.g. the default template's "Completion" stage).
    @Transactional public Workflow advance(Long workflowId,String note) {
        Workflow w=workflows.findById(workflowId).orElseThrow(()->new ResourceNotFoundException("Workflow not found"));
        if(w.getStatus()!=WorkflowStatus.IN_PROGRESS&&w.getStatus()!=WorkflowStatus.PENDING)throw new IllegalArgumentException("Workflow is not active");
        List<WorkflowStage> ordered=stages(workflowId);
        WorkflowStage current=ordered.stream().filter(s->s.getStageOrder()==w.getCurrentStage()).findFirst().orElseThrow(()->new ResourceNotFoundException("Current stage not found"));
        if(current.isCompleted())throw new IllegalArgumentException("Current stage is already completed");
        if(!SecurityUtil.has(current.getRequiredRole()))throw new UnauthorisedAccessException("Only "+current.getRequiredRole().replace('_',' ')+" can complete this stage");
        current.setCompleted(true);
        current.setCompletedBy(currentUserId());
        stages.save(current);
        boolean last=current.getStageOrder()==ordered.size();
        w.setStatus(last?WorkflowStatus.COMPLETED:WorkflowStatus.IN_PROGRESS);
        if(!last)w.setCurrentStage(w.getCurrentStage()+1);
        Workflow saved=workflows.save(w);
        audit.log("WORKFLOW_STAGE_COMPLETE","WORKFLOW",String.valueOf(workflowId),"SUCCESS",null,current.getStageName(),note);
        return saved;
    }

    @Transactional(readOnly=true) public List<Map<String,Object>> list() {
        return workflows.findAll().stream().sorted(Comparator.comparing(Workflow::getCreatedAt).reversed()).map(this::view).toList();
    }
    @Transactional(readOnly=true) public Map<String,Object> get(Long id) {
        Workflow w=workflows.findById(id).orElseThrow(()->new ResourceNotFoundException("Workflow not found"));
        return view(w);
    }

    private Map<String,Object> view(Workflow w) {
        List<WorkflowStage> ordered=stages(w.getId());
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("id",w.getId());
        result.put("workflowType",w.getWorkflowType());
        result.put("entityType",w.getEntityType());
        result.put("entityId",w.getEntityId());
        result.put("record",recordLabel(w.getEntityType(),w.getEntityId()));
        result.put("status",w.getStatus());
        result.put("currentStage",w.getCurrentStage());
        result.put("totalStages",ordered.size());
        result.put("startedAt",w.getCreatedAt());

        Instant cursor=w.getCreatedAt();
        WorkflowStage active=null;
        Instant activeDue=null;
        List<Map<String,Object>> stageViews=new ArrayList<>();
        for(WorkflowStage s:ordered) {
            Instant due=cursor.plusSeconds(s.getSlaHours()*3600L);
            cursor=due;
            Map<String,Object> sv=new LinkedHashMap<>();
            sv.put("order",s.getStageOrder());
            sv.put("name",s.getStageName());
            sv.put("responsibleRole",s.getRequiredRole());
            sv.put("slaHours",s.getSlaHours());
            sv.put("dueAt",due);
            sv.put("completed",s.isCompleted());
            stageViews.add(sv);
            if(s.getStageOrder()==w.getCurrentStage()) {
                active=s;
                activeDue=due;
            }
        }
        result.put("stages",stageViews);
        if(active!=null&&w.getStatus()==WorkflowStatus.IN_PROGRESS) {
            result.put("currentStageName",active.getStageName());
            result.put("responsibleRole",active.getRequiredRole());
            result.put("slaHours",active.getSlaHours());
            result.put("dueAt",activeDue);
            boolean canAdvance=SecurityUtil.has(active.getRequiredRole());
            result.put("canAdvance",canAdvance);
            int nextOrder=active.getStageOrder()+1;
            result.put("nextStep",ordered.stream().filter(s->s.getStageOrder()==nextOrder).findFirst().map(WorkflowStage::getStageName).orElse("Workflow completion"));
        } else {
            result.put("currentStageName",w.getStatus()==WorkflowStatus.COMPLETED?"Completed":"—");
            result.put("responsibleRole","—");
            result.put("canAdvance",false);
            result.put("nextStep",w.getStatus()==WorkflowStatus.COMPLETED?"None — workflow completed":"—");
        }
        return result;
    }

    private String recordLabel(String entityType,Long entityId) {
        if(entityType==null||entityId==null)return "#"+entityId;
        try {
            switch(entityType.toUpperCase()) {
                case "CLAIM"-> {
                    return claims.findById(entityId).map(c->"Claim #"+c.getId()+" - "+c.getPolicy().getFarmer().getName()+" - "+c.getPolicy().getCropName()).orElse("Claim #"+entityId+" (not found)");
                }case "POLICY"-> {
                    return policies.findById(entityId).map(p->"Policy "+p.getPolicyNumber()+" - "+p.getFarmer().getName()+" - "+p.getCropName()).orElse("Policy #"+entityId+" (not found)");
                }case "LOSS","LOSS_NOTIFICATION"-> {
                    return losses.findById(entityId).map(l->"Loss #"+l.getId()+" - "+l.getPolicy().getFarmer().getName()+" - "+l.getLossType()).orElse("Loss #"+entityId+" (not found)");
                }case "SURVEY","SURVEY_ASSIGNMENT"-> {
                    return surveys.findById(entityId).map(s->"Survey #"+s.getId()+" - "+s.getNotification().getPolicy().getFarmer().getName()).orElse("Survey #"+entityId+" (not found)");
                }default-> {
                    return entityType+" #"+entityId;
                }
            }
        }catch(Exception e) {
            return entityType+" #"+entityId;
        }
    }

    private Long currentUserId() {
        try {
            return users.findByUserId(SecurityUtil.username()).map(User::getId).orElse(null);
        }catch(Exception e) {
            return null;
        }
    }

    public record StageSpec(String name,String role,int slaHours) {
    }
}
