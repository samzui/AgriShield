package com.examly.springapp.controller;
import com.examly.springapp.service.ReportService;
import com.examly.springapp.service.ReportService.Filters;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('INSURANCE_OFFICER','STATE_OFFICER','ADMIN')")
public class ReportController {
    private final ReportService service;
    public ReportController(ReportService service) {
        this.service=service;
    }
    @GetMapping("/pmfby")
    public ResponseEntity<byte[]> pmfby(@RequestParam(defaultValue="csv")String format)throws Exception {
        String f=format.toLowerCase();
        byte[] data;
        String type;
        String ext;
        switch(f) {
            case "xlsx","excel"-> {
                data=service.excel();
                type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                ext="xlsx";
            }case "pdf"-> {
                data=service.pdf();
                type="application/pdf";
                ext="pdf";
            }case "csv"-> {
                data=service.csv();
                type="text/csv";
                ext="csv";
            }default->throw new IllegalArgumentException("Supported formats are csv, xlsx and pdf");
        }
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(type)).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=pmfby-report."+ext).body(data);
    }

    // Detailed per-record-type reports (survey, claim, policy, loss). Supports the same date-range/crop/season/
    // status/lossType filters the Reports page already collects, plus an optional `id` for a single-record
    // case report (e.g. one specific claim or survey) instead of the full filtered list.
    @GetMapping("/{type}")
    public ResponseEntity<byte[]> detail(@PathVariable String type,
                                          @RequestParam(defaultValue="csv")String format,
                                          @RequestParam(required=false)Long id,
                                          @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate from,
                                          @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate to,
                                          @RequestParam(required=false)String crop,
                                          @RequestParam(required=false)String season,
                                          @RequestParam(required=false)String status,
                                          @RequestParam(required=false)String lossType)throws Exception {
        String f=format.toLowerCase();
        Filters filters=new Filters(id,from,to,crop,season,status,lossType);
        byte[] data;
        String contentType;
        String ext;
        switch(f) {
            case "xlsx","excel"-> {
                data=service.detailExcel(type,filters);
                contentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                ext="xlsx";
            }case "pdf"-> {
                data=service.detailPdf(type,filters);
                contentType="application/pdf";
                ext="pdf";
            }case "csv"-> {
                data=service.detailCsv(type,filters);
                contentType="text/csv";
                ext="csv";
            }default->throw new IllegalArgumentException("Supported formats are csv, xlsx and pdf");
        }
        String filename=type.toLowerCase()+(id!=null?"-"+id:"")+"-report."+ext;
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType)).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename="+filename).body(data);
    }
}
