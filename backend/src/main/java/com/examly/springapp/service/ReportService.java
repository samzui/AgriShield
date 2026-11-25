package com.examly.springapp.service;
import com.examly.springapp.entity.*;
import com.examly.springapp.enums.ClaimStatus;
import com.examly.springapp.enums.SurveyStatus;
import com.examly.springapp.repository.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {
    private final AnalyticsService analytics;
    private final PolicyRepository policyRepo;
    private final LossNotificationRepository lossRepo;
    private final SurveyAssignmentRepository surveyRepo;
    private final ClaimRepository claimRepo;
    private final StatusHistoryRepository statusHistoryRepo;

    public ReportService(AnalyticsService analytics, PolicyRepository policyRepo, LossNotificationRepository lossRepo, SurveyAssignmentRepository surveyRepo, ClaimRepository claimRepo, StatusHistoryRepository statusHistoryRepo) {
        this.analytics = analytics;
        this.policyRepo = policyRepo;
        this.lossRepo = lossRepo;
        this.surveyRepo = surveyRepo;
        this.claimRepo = claimRepo;
        this.statusHistoryRepo = statusHistoryRepo;
    }

    // ---------- PMFBY consolidated summary (existing endpoint, enriched content) ----------

    public byte[] csv() {
        var m = pmfbySummary();
        StringBuilder sb = new StringBuilder("metric,value\n");
        m.forEach((k, v) -> sb.append(esc(k)).append(",").append(esc(v)).append("\n"));
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] excel() throws IOException {
        var m = pmfbySummary();
        try (var wb = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sh = wb.createSheet("PMFBY");
            var header = sh.createRow(0);
            header.createCell(0).setCellValue("Metric");
            header.createCell(1).setCellValue("Value");
            int r = 1;
            for (var e : m.entrySet()) {
                var row = sh.createRow(r++);
                row.createCell(0).setCellValue(e.getKey());
                row.createCell(1).setCellValue(String.valueOf(e.getValue()));
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    public byte[] pdf() throws IOException {
        var m = pmfbySummary();
        List<String> lines = m.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).toList();
        return renderPdf("PMFBY Crop Insurance Operational Report", lines);
    }

    @Transactional(readOnly = true)
    protected LinkedHashMap<String, Object> pmfbySummary() {
        var d = analytics.dashboard();
        List<Claim> allClaims = claimRepo.findAll();
        long initiated = allClaims.stream().filter(c -> c.getStatus() == ClaimStatus.INITIATED).count();
        long underReview = allClaims.stream().filter(c -> c.getStatus() == ClaimStatus.UNDER_REVIEW).count();
        long approved = allClaims.stream().filter(c -> c.getStatus() == ClaimStatus.APPROVED).count();
        long rejected = allClaims.stream().filter(c -> c.getStatus() == ClaimStatus.REJECTED).count();
        BigDecimal disbursed = allClaims.stream().filter(c -> c.getStatus() == ClaimStatus.PAID).map(Claim::getApprovedAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        long totalSurveys = surveyRepo.count();
        long completedSurveys = surveyRepo.findAll().stream().filter(s -> s.getStatus() == SurveyStatus.SUBMITTED || s.getStatus() == SurveyStatus.VERIFIED).count();
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("Policies", d.policies());
        out.put("Active Policies", d.activePolicies());
        out.put("Loss Notifications", d.losses());
        out.put("Surveys Conducted", totalSurveys);
        out.put("Surveys Completed", completedSurveys);
        out.put("Claims Filed", d.claims());
        out.put("Claims Initiated", initiated);
        out.put("Claims Under Review", underReview);
        out.put("Claims Approved", approved);
        out.put("Claims Rejected", rejected);
        out.put("Claims Paid", d.paidClaims());
        out.put("Total Exposure (Sum Insured)", d.exposure());
        out.put("Total Disbursed (DBT)", disbursed);
        out.put("Claim Ratio (%)", d.claimRatio());
        out.put("Audit Events", d.auditEvents());
        out.put("Generated At", Instant.now());
        return out;
    }

    // ---------- Detailed per-type reports ----------

    public record Filters(Long id, LocalDate from, LocalDate to, String crop, String season, String status, String lossType) {
    }

    private record ReportData(String title, List<String> headers, List<List<String>> rows) {
    }

    @Transactional(readOnly = true)
    public byte[] detailCsv(String type, Filters f) {
        return toCsv(build(type, f));
    }

    @Transactional(readOnly = true)
    public byte[] detailExcel(String type, Filters f) throws IOException {
        return toExcel(build(type, f));
    }

    @Transactional(readOnly = true)
    public byte[] detailPdf(String type, Filters f) throws IOException {
        var r = build(type, f);
        List<String> lines = new ArrayList<>();
        if (r.rows().isEmpty()) lines.add("No matching records for the selected filters.");
        int n = 1;
        for (var row : r.rows()) {
            lines.add("Record " + n++);
            for (int i = 0; i < r.headers().size(); i++) lines.add("  " + r.headers().get(i) + ": " + (i < row.size() ? row.get(i) : ""));
            lines.add("");
        }
        return renderPdf(r.title(), lines);
    }

    private ReportData build(String type, Filters f) {
        return switch (type.toUpperCase()) {
            case "SURVEY" -> surveyReport(f);
            case "CLAIM" -> claimReport(f);
            case "POLICY" -> policyReport(f);
            case "LOSS" -> lossReport(f);
            default -> throw new IllegalArgumentException("Unsupported report type: " + type + ". Supported: survey, claim, policy, loss.");
        };
    }

    private ReportData surveyReport(Filters f) {
        List<String> headers = List.of("Survey ID", "Farmer", "Phone", "Policy Number", "Crop", "Season", "Loss Notification ID", "Loss Type",
            "Affected Area (ha)", "GPS Location", "Assigned Surveyor", "Assigned Date", "Survey Date", "Loss Assessed %", "Yield Assessed (kg/ha)",
            "Assessment Source", "Evidence Photos", "Survey Status", "Loss Notification Status");
        List<SurveyAssignment> source = f.id() != null
            ? surveyRepo.findByIdWithDetails(f.id()).map(List::of).orElseGet(List::of)
            : surveyRepo.findAllWithDetails();
        List<List<String>> rows = source.stream()
            .filter(s -> f.id() != null || inRange(s.getSurveyDate(), f))
            .filter(s -> f.id() != null || matches(f.crop(), s.getNotification().getPolicy().getCropName()))
            .filter(s -> f.id() != null || matches(f.season(), s.getNotification().getPolicy().getSeason().name()))
            .filter(s -> f.id() != null || matches(f.status(), s.getStatus().name()))
            .map(s -> {
                Policy p = s.getNotification().getPolicy();
                User farmer = p.getFarmer();
                String gps = s.getSurveyGeoLat() != null ? s.getSurveyGeoLat() + ", " + s.getSurveyGeoLng()
                    : s.getNotification().getGeoLat() != null ? s.getNotification().getGeoLat() + ", " + s.getNotification().getGeoLng() : "N/A";
                return List.of(str(s.getId()), farmer.getName(), farmer.getPhoneNumber(), nvl(p.getPolicyNumber()), p.getCropName(), p.getSeason().name(),
                    str(s.getNotification().getId()), s.getNotification().getLossType().name(), str(s.getNotification().getAffectedAreaHa()), gps,
                    s.getSurveyor().getName(), str(s.getAssignedDate()), str(s.getSurveyDate()), str(s.getLossAssessedPercentage()), str(s.getYieldAssessedKgHa()),
                    nvl(s.getAssessmentSource()), String.valueOf(s.getSurveyPhotos() == null ? 0 : s.getSurveyPhotos().size()), s.getStatus().name(), s.getNotification().getStatus().name());
            }).toList();
        return new ReportData("Survey Report", headers, rows);
    }

    private ReportData claimReport(Filters f) {
        List<String> headers = List.of("Claim ID", "Farmer", "Policy Number", "Crop", "Season", "Sown Area (ha)", "Sum Insured", "Loss Notification ID",
            "Loss Type", "Affected Area (ha)", "Estimated Loss %", "Survey ID", "Survey Status", "Surveyed Loss %", "Claimed Amount", "Approved Amount",
            "Claim Status", "Approval / Rejection Reason", "Disbursement Status", "DBT UTR", "Disbursed Date", "Created At");
        List<Claim> source = f.id() != null ? claimRepo.findById(f.id()).map(List::of).orElseGet(List::of) : claimRepo.findAll();
        List<List<String>> rows = source.stream()
            .filter(c -> f.id() != null || inRange(c.getCreatedAt(), f))
            .filter(c -> f.id() != null || matches(f.crop(), c.getPolicy().getCropName()))
            .filter(c -> f.id() != null || matches(f.season(), c.getPolicy().getSeason().name()))
            .filter(c -> f.id() != null || matches(f.status(), c.getStatus().name()))
            .map(c -> {
                Policy p = c.getPolicy();
                SurveyAssignment s = c.getSurvey();
                LossNotification n = s != null ? s.getNotification() : null;
                String reason = statusHistoryRepo.findByEntityTypeAndEntityIdOrderByChangedAtAsc("CLAIM", c.getId()).stream()
                    .filter(h -> "APPROVED".equals(h.getNewStatus()) || "REJECTED".equals(h.getNewStatus()))
                    .reduce((first, second) -> second).map(StatusHistory::getReason).orElse("N/A");
                return List.of(str(c.getId()), p.getFarmer().getName(), nvl(p.getPolicyNumber()), p.getCropName(), p.getSeason().name(), str(p.getSownAreaHa()),
                    str(p.getSumInsured()), n != null ? str(n.getId()) : "N/A", n != null ? n.getLossType().name() : "N/A", n != null ? str(n.getAffectedAreaHa()) : "N/A",
                    n != null ? str(n.getLossPercentageEstimate()) : "N/A", s != null ? str(s.getId()) : "N/A", s != null ? s.getStatus().name() : "N/A",
                    s != null ? str(s.getLossAssessedPercentage()) : "N/A", str(c.getClaimedAmount()), c.getApprovedAmount() != null ? str(c.getApprovedAmount()) : "N/A",
                    c.getStatus().name(), reason, c.getDbtUtr() != null ? "DISBURSED" : (c.getStatus() == ClaimStatus.APPROVED ? "PENDING" : "N/A"),
                    nvl(c.getDbtUtr()), c.getDisbursedDate() != null ? str(c.getDisbursedDate()) : "N/A", str(c.getCreatedAt()));
            }).toList();
        return new ReportData("Claim Report", headers, rows);
    }

    private ReportData policyReport(Filters f) {
        List<String> headers = List.of("Policy Number", "Farmer", "Phone", "Crop", "Season", "Sown Area (ha)", "Sum Insured", "Premium (Farmer)",
            "Premium (State)", "Premium (Centre)", "Enrollment Date", "Policy Status", "Linked Loss Reports", "Linked Claims");
        List<Policy> source = f.id() != null ? policyRepo.findById(f.id()).map(List::of).orElseGet(List::of) : policyRepo.findAll();
        List<LossNotification> allLosses = lossRepo.findAll();
        List<Claim> allClaims = claimRepo.findAll();
        List<List<String>> rows = source.stream()
            .filter(p -> f.id() != null || inRange(p.getEnrollmentDate(), f))
            .filter(p -> f.id() != null || matches(f.crop(), p.getCropName()))
            .filter(p -> f.id() != null || matches(f.season(), p.getSeason().name()))
            .filter(p -> f.id() != null || matches(f.status(), p.getStatus().name()))
            .map(p -> {
                long linkedLosses = allLosses.stream().filter(l -> l.getPolicy().getId().equals(p.getId())).count();
                long linkedClaims = allClaims.stream().filter(c -> c.getPolicy().getId().equals(p.getId())).count();
                return List.of(nvl(p.getPolicyNumber()), p.getFarmer().getName(), p.getFarmer().getPhoneNumber(), p.getCropName(), p.getSeason().name(),
                    str(p.getSownAreaHa()), str(p.getSumInsured()), str(p.getPremiumFarmer()), str(p.getPremiumState()), str(p.getPremiumCentre()),
                    str(p.getEnrollmentDate()), p.getStatus().name(), String.valueOf(linkedLosses), String.valueOf(linkedClaims));
            }).toList();
        return new ReportData("Policy Report", headers, rows);
    }

    private ReportData lossReport(Filters f) {
        List<String> headers = List.of("Loss ID", "Farmer", "Policy Number", "Crop", "Loss Type", "Affected Area (ha)", "Estimated Loss %",
            "GPS Location", "Evidence Photos", "Notification Date", "Survey Status", "Assigned Surveyor", "Loss Notification Status");
        List<LossNotification> source = f.id() != null ? lossRepo.findById(f.id()).map(List::of).orElseGet(List::of) : lossRepo.findAll();
        List<List<String>> rows = source.stream()
            .filter(l -> f.id() != null || inRange(l.getNotificationDate(), f))
            .filter(l -> f.id() != null || matches(f.crop(), l.getPolicy().getCropName()))
            .filter(l -> f.id() != null || matches(f.lossType(), l.getLossType().name()))
            .filter(l -> f.id() != null || matches(f.status(), l.getStatus().name()))
            .map(l -> {
                var assignment = surveyRepo.findByNotificationId(l.getId()).orElse(null);
                String gps = l.getGeoLat() != null ? l.getGeoLat() + ", " + l.getGeoLng() : "N/A";
                return List.of(str(l.getId()), l.getPolicy().getFarmer().getName(), nvl(l.getPolicy().getPolicyNumber()), l.getPolicy().getCropName(),
                    l.getLossType().name(), str(l.getAffectedAreaHa()), str(l.getLossPercentageEstimate()), gps,
                    String.valueOf(l.getPhotoUrls() == null ? 0 : l.getPhotoUrls().size()), str(l.getNotificationDate()),
                    assignment != null ? assignment.getStatus().name() : "NOT ASSIGNED", assignment != null ? assignment.getSurveyor().getName() : "N/A",
                    l.getStatus().name());
            }).toList();
        return new ReportData("Loss Report", headers, rows);
    }

    // ---------- Filter helpers ----------

    private boolean matches(String filter, String actual) {
        return filter == null || filter.isBlank() || "ALL".equalsIgnoreCase(filter) || filter.equalsIgnoreCase(actual);
    }

    private boolean inRange(LocalDate d, Filters f) {
        if (f.from() == null) return true;
        if (d == null) return false;
        if (d.isBefore(f.from())) return false;
        if (f.to() != null && d.isAfter(f.to())) return false;
        return true;
    }

    private boolean inRange(Instant t, Filters f) {
        return inRange(t == null ? null : t.atZone(ZoneOffset.UTC).toLocalDate(), f);
    }

    private String str(Object v) {
        return v == null ? "N/A" : String.valueOf(v);
    }

    private String nvl(String v) {
        return v == null || v.isBlank() ? "N/A" : v;
    }

    // ---------- Rendering ----------

    private String esc(Object v) {
        String s = v == null ? "" : String.valueOf(v);
        return (s.contains(",") || s.contains("\"") || s.contains("\n")) ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
    }

    private byte[] toCsv(ReportData r) {
        StringBuilder sb = new StringBuilder(String.join(",", r.headers())).append("\n");
        for (var row : r.rows()) sb.append(row.stream().map(this::esc).collect(Collectors.joining(","))).append("\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] toExcel(ReportData r) throws IOException {
        try (var wb = new XSSFWorkbook(); var out = new ByteArrayOutputStream()) {
            var sh = wb.createSheet(r.title().length() > 31 ? r.title().substring(0, 31) : r.title());
            var header = sh.createRow(0);
            for (int c = 0; c < r.headers().size(); c++) header.createCell(c).setCellValue(r.headers().get(c));
            int rowIdx = 1;
            for (var row : r.rows()) {
                var xr = sh.createRow(rowIdx++);
                for (int c = 0; c < row.size(); c++) xr.createCell(c).setCellValue(row.get(c));
            }
            wb.write(out);
            return out.toByteArray();
        }
    }

    private byte[] renderPdf(String title, List<String> lines) throws IOException {
        try (var doc = new PDDocument(); var out = new ByteArrayOutputStream()) {
            PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont plain = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDPage page = new PDPage();
            doc.addPage(page);
            PDPageContentStream stream = new PDPageContentStream(doc, page);
            float y = 750;
            stream.beginText();
            stream.setFont(bold, 15);
            stream.newLineAtOffset(50, y);
            stream.showText(title);
            stream.endText();
            y -= 18;
            stream.beginText();
            stream.setFont(plain, 8);
            stream.newLineAtOffset(50, y);
            stream.showText("Generated: " + Instant.now());
            stream.endText();
            y -= 22;
            for (String rawLine : lines) {
                String line = truncate(rawLine, 105);
                if (y < 55) {
                    stream.close();
                    page = new PDPage();
                    doc.addPage(page);
                    stream = new PDPageContentStream(doc, page);
                    y = 750;
                }
                boolean isHeading = !rawLine.startsWith(" ") && !rawLine.isBlank();
                stream.beginText();
                stream.setFont(isHeading ? bold : plain, isHeading ? 10 : 9);
                stream.newLineAtOffset(50, y);
                stream.showText(line.isBlank() ? " " : line);
                stream.endText();
                y -= isHeading ? 15 : 12;
            }
            stream.close();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }
}
