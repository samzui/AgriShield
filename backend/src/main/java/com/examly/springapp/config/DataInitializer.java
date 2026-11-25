package com.examly.springapp.config;

import com.examly.springapp.entity.*;
import com.examly.springapp.enums.*;
import com.examly.springapp.repository.*;
import com.examly.springapp.service.PermissionService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataInitializer {
    private static final String DEMO_PASSWORD = "Admin@1234";

    @Bean
    @Profile({"default", "dev", "test"})
    CommandLineRunner seed(
            UserRepository users,
            PasswordEncoder encoder,
            CropRepository crops,
            PolicyRepository policies,
            LossNotificationRepository losses,
            SurveyAssignmentRepository surveys,
            ClaimRepository claims,
            NotificationTemplateRepository templates,
            AuditLogRepository audits,
            ExternalIntegrationRepository integrations,
            ComplianceDeadlineRepository deadlines,
            PermissionService permissionService,
            PermissionRepository permissionRepo,
            RolePermissionRepository rolePermissionRepo) {
        return args -> {
            seedPermissions(permissionService, permissionRepo, rolePermissionRepo);

            User farmer = ensureUser(users, encoder, "Ravi Kumar", "farmer", "farmer@example.com", "9876543210", Role.FARMER);
            User secondFarmer = ensureUser(users, encoder, "Meena Devi", "farmer_meena", "meena@example.com", "9876543220", Role.FARMER);
            User thirdFarmer = ensureUser(users, encoder, "Arjun Singh", "farmer_arjun", "arjun@example.com", "9876543230", Role.FARMER);
            User bank = ensureUser(users, encoder, "Bank Officer", "bank_officer", "bank_officer@example.com", "9876543211", Role.BANK_OFFICER);
            User surveyor = ensureUser(users, encoder, "Field Surveyor", "surveyor", "surveyor@example.com", "9876543212", Role.SURVEYOR);
            User officer = ensureUser(users, encoder, "Insurance Officer", "insurance_officer", "insurance_officer@example.com", "9876543213", Role.INSURANCE_OFFICER);
            User state = ensureUser(users, encoder, "State Officer", "state_officer", "state_officer@example.com", "9876543214", Role.STATE_OFFICER);
            User admin = ensureUser(users, encoder, "System Administrator", "admin", "admin@example.com", "9876543215", Role.ADMIN);

            ensureCrop(crops, "Paddy", Season.KHARIF, "3000", "2183");
            ensureCrop(crops, "Wheat", Season.RABI, "2800", "2275");
            ensureCrop(crops, "Cotton", Season.KHARIF, "1800", "6620");
            ensureCrop(crops, "Groundnut", Season.ZAID, "2200", "6377");

            Policy paddy = ensurePolicy(policies, "PMFBY-DEMO-001", farmer, "Paddy", Season.KHARIF, "2.50", "100000", PolicyStatus.CLAIM_FILED);
            Policy wheat = ensurePolicy(policies, "PMFBY-DEMO-002", secondFarmer, "Wheat", Season.RABI, "3.20", "145000", PolicyStatus.ACTIVE);
            Policy cotton = ensurePolicy(policies, "PMFBY-DEMO-003", thirdFarmer, "Cotton", Season.KHARIF, "4.00", "210000", PolicyStatus.ENROLLED);
            Policy groundnut = ensurePolicy(policies, "PMFBY-DEMO-004", farmer, "Groundnut", Season.ZAID, "1.75", "72000", PolicyStatus.SETTLED);

            LossNotification flood = ensureLoss(losses, paddy, LossType.FLOOD, LossNotificationStatus.SURVEYED, "1.80", "65", "13.0827000", "80.2707000", 5);
            LossNotification drought = ensureLoss(losses, wheat, LossType.DROUGHT, LossNotificationStatus.SUBMITTED, "2.10", "42", "17.3850000", "78.4867000", 12);
            LossNotification pest = ensureLoss(losses, cotton, LossType.PEST, LossNotificationStatus.SURVEYOR_ASSIGNED, "1.60", "28", "21.1458000", "79.0882000", 3);

            SurveyAssignment floodSurvey = ensureSurvey(surveys, flood, surveyor, SurveyStatus.SUBMITTED, "CCE-DEMO-001", "65", "1200", "0.31", 3);
            ensureSurvey(surveys, drought, surveyor, SurveyStatus.ASSIGNED, "CCE-DEMO-002", "42", "1650", "0.48", 0);
            ensureSurvey(surveys, pest, surveyor, SurveyStatus.IN_PROGRESS, "CCE-DEMO-003", "28", "—", "0.57", 1);

            ensureClaim(claims, floodSurvey, paddy, ClaimStatus.APPROVED, "65000", "60000", null);

            ensureTemplate(templates, "CLAIM_STATUS", "Claim status update", "Your crop insurance claim status is {{status}}.");
            ensureTemplate(templates, "POLICY_RENEWAL", "Policy renewal reminder", "Your {{crop}} policy is due for review on {{date}}.");
            ensureIntegration(integrations, "PMFBY_DACFW", "PMFBY DAC&FW Gateway", IntegrationStatus.ACTIVE);
            ensureIntegration(integrations, "SATELLITE_NDVI", "Satellite Crop Health Service", IntegrationStatus.ACTIVE);
            ensureDeadline(deadlines, "PMFBY Regulatory Submission", "DAC&FW", 30, "PMFBY-DEMO-2026-001");
            ensureDeadline(deadlines, "Kharif Loss Assessment Window", "State Agriculture Department", 14, "KHA-LOSS-2026-001");
            ensureAudit(audits, admin);
        };
    }

    private User ensureUser(UserRepository repo, PasswordEncoder encoder, String name, String userId, String email, String phone, Role role) {
        return repo.findByEmailIgnoreCase(email).orElseGet(() -> {
            User user = new User();
            user.setName(name);
            user.setUserId(userId);
            user.setEmail(email);
            user.setPhoneNumber(phone);
            user.setPasswordHash(encoder.encode(DEMO_PASSWORD));
            user.setRole(role);
            user.setActive(true);
            user.setEmailVerified(true);
            user.setSupervisorApproved(true);
            user.setPasswordChangedAt(Instant.now());
            user.setPasswordExpiresAt(Instant.now().plusSeconds((role == Role.FARMER ? 180L : 90L) * 24L * 60L * 60L));
            return repo.save(user);
        });
    }

    private Crop ensureCrop(CropRepository repo, String name, Season season, String yield, String msp) {
        return repo.findByCropNameIgnoreCaseAndSeason(name, season).orElseGet(() -> {
            Crop crop = new Crop();
            crop.setCropName(name);
            crop.setSeason(season);
            crop.setStandardYieldKgHa(new BigDecimal(yield));
            crop.setMspRate(new BigDecimal(msp));
            crop.setActive(true);
            return repo.save(crop);
        });
    }

    private Policy ensurePolicy(PolicyRepository repo, String number, User farmer, String crop, Season season, String area, String sum, PolicyStatus status) {
        return repo.findAll().stream().filter(policy -> number.equals(policy.getPolicyNumber())).findFirst().orElseGet(() -> {
            Policy policy = new Policy();
            policy.setPolicyNumber(number);
            policy.setFarmer(farmer);
            policy.setCropName(crop);
            policy.setSeason(season);
            policy.setSownAreaHa(new BigDecimal(area));
            policy.setBankAccountNo("1234567890");
            policy.setSumInsured(new BigDecimal(sum));
            policy.setPremiumFarmer(new BigDecimal(sum).multiply(new BigDecimal("0.02")));
            policy.setPremiumState(new BigDecimal(sum).multiply(new BigDecimal("0.04")));
            policy.setPremiumCentre(new BigDecimal(sum).multiply(new BigDecimal("0.04")));
            policy.setStatus(status);
            policy.setEnrollmentDate(LocalDate.now().minusDays(10));
            policy.setPremiumRateCode("PMFBY-DEMO");
            return repo.save(policy);
        });
    }

    private LossNotification ensureLoss(LossNotificationRepository repo, Policy policy, LossType type, LossNotificationStatus status, String area, String percentage, String lat, String lng, int daysAgo) {
        return repo.findAll().stream().filter(loss -> loss.getPolicy().getId().equals(policy.getId()) && loss.getLossType() == type).findFirst().orElseGet(() -> {
            LossNotification loss = new LossNotification();
            loss.setPolicy(policy);
            loss.setLossType(type);
            loss.setAffectedAreaHa(new BigDecimal(area));
            loss.setLossPercentageEstimate(new BigDecimal(percentage));
            loss.setNotificationDate(Instant.now().minusSeconds(daysAgo * 24L * 60L * 60L));
            loss.setGeoLat(new BigDecimal(lat));
            loss.setGeoLng(new BigDecimal(lng));
            loss.setPhotoUrls(new ArrayList<>(List.of("demo-evidence-" + type.name().toLowerCase() + ".jpg")));
            loss.setStatus(status);
            return repo.save(loss);
        });
    }

    private SurveyAssignment ensureSurvey(SurveyAssignmentRepository repo, LossNotification loss, User surveyor, SurveyStatus status, String plotId, String percentage, String yield, String ndvi, int daysAgo) {
        return repo.findByNotificationId(loss.getId()).orElseGet(() -> {
            SurveyAssignment survey = new SurveyAssignment();
            survey.setNotification(loss);
            survey.setSurveyor(surveyor);
            survey.setAssignedDate(LocalDate.now().minusDays(Math.max(1, daysAgo + 1)));
            survey.setSurveyDate(daysAgo > 1 ? LocalDate.now().minusDays(daysAgo) : null);
            survey.setYieldAssessedKgHa("—".equals(yield) ? null : new BigDecimal(yield));
            survey.setLossAssessedPercentage(new BigDecimal(percentage));
            survey.setSurveyPhotos(new ArrayList<>(List.of("survey-evidence-" + plotId + ".jpg")));
            survey.setStatus(status);
            survey.setCcePlotId(plotId);
            survey.setCcePlotAreaHa(new BigDecimal("0.10"));
            survey.setSampleYieldKgHa("—".equals(yield) ? null : new BigDecimal(yield));
            survey.setSatelliteNdvi(new BigDecimal(ndvi));
            survey.setSatelliteVegetationHealth(new BigDecimal(ndvi).multiply(new BigDecimal("100")));
            survey.setSurveyGeoLat(loss.getGeoLat());
            survey.setSurveyGeoLng(loss.getGeoLng());
            survey.setAssessmentSource("CCE+SATELLITE");
            return repo.save(survey);
        });
    }

    private void ensureClaim(ClaimRepository repo, SurveyAssignment survey, Policy policy, ClaimStatus status, String claimed, String approved, String utr) {
        if (repo.findBySurveyId(survey.getId()).isPresent()) return;
        Claim claim = new Claim();
        claim.setPolicy(policy);
        claim.setSurvey(survey);
        claim.setClaimedAmount(new BigDecimal(claimed));
        claim.setApprovedAmount(approved == null ? null : new BigDecimal(approved));
        claim.setDbtUtr(utr);
        claim.setDisbursedDate(status == ClaimStatus.PAID ? LocalDate.now().minusDays(2) : null);
        claim.setStatus(status);
        repo.save(claim);
    }

    private void ensureTemplate(NotificationTemplateRepository repo, String code, String subject, String body) {
        if (repo.existsByCode(code)) return;
        NotificationTemplate template = new NotificationTemplate();
        template.setCode(code);
        template.setSubject(subject);
        template.setBody(body);
        template.setActive(true);
        repo.save(template);
    }

    private void ensureIntegration(ExternalIntegrationRepository repo, String code, String name, IntegrationStatus status) {
        ExternalIntegration integration = repo.findByCode(code).orElseGet(ExternalIntegration::new);
        integration.setCode(code);
        integration.setDisplayName(name);
        integration.setEnvironment("demo");
        integration.setStatus(status);
        integration.setLastCheckedAt(Instant.now());
        repo.save(integration);
    }

    private void ensureDeadline(ComplianceDeadlineRepository repo, String title, String regulator, int days, String reference) {
        boolean exists = repo.findAll().stream().anyMatch(deadline -> title.equals(deadline.getTitle()));
        if (exists) return;
        ComplianceDeadline deadline = new ComplianceDeadline();
        deadline.setTitle(title);
        deadline.setRegulator(regulator);
        deadline.setDueDate(LocalDate.now().plusDays(days));
        deadline.setReferenceNumber(reference);
        deadline.setNotes("Seeded operational demonstration record");
        deadline.setCompleted(false);
        repo.save(deadline);
    }

    private void ensureAudit(AuditLogRepository repo, User admin) {
        if (!repo.findAll().stream().anyMatch(log -> "DEMO-SEED".equals(log.getEntityId()))) {
            AuditLog log = new AuditLog();
            log.setUserId(admin.getId());
            log.setAction("SEED");
            log.setEntityType("SYSTEM");
            log.setEntityId("DEMO-SEED");
            log.setTimestamp(Instant.now());
            log.setIpAddress("127.0.0.1");
            log.setPurpose("development seed");
            log.setResult("SUCCESS");
            log.setEventHash("demo-seed-v2");
            repo.save(log);
        }
    }

    private void seedPermissions(PermissionService service, PermissionRepository permissions, RolePermissionRepository mappings) {
        String[] codes = {"POLICY_READ", "POLICY_WRITE", "LOSS_READ", "LOSS_WRITE", "CLAIM_READ", "CLAIM_WRITE", "REPORT_READ", "AUDIT_READ", "ADMIN_WRITE", "DOCUMENT_READ", "DOCUMENT_WRITE", "SCHEDULE_WRITE"};
        for (String code : codes) service.ensure(code, "Runtime permission " + code);
        List<Permission> all = permissions.findAll();
        List<RolePermission> existing = mappings.findAll();
        for (Role role : Role.values()) for (Permission permission : all) {
            boolean present = existing.stream().anyMatch(mapping -> mapping.getRole() == role && mapping.getPermission().getId().equals(permission.getId()));
            if (!present) {
                RolePermission mapping = new RolePermission();
                mapping.setRole(role);
                mapping.setPermission(permission);
                mapping.setEnabled(role != Role.GUEST);
                mappings.save(mapping);
            }
        }
    }
}
