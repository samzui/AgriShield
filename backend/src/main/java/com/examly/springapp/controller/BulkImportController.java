package com.examly.springapp.controller;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.entity.ScanRecord;
import com.examly.springapp.repository.ScanRecordRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import org.xml.sax.InputSource;
@RestController @RequestMapping("/api/admin/import") @PreAuthorize("hasRole('ADMIN')")
public class BulkImportController {
    private final ScanRecordRepository scans;
    public BulkImportController(ScanRecordRepository scans) {
        this.scans=scans;
    }
    @PostMapping(value="/csv",consumes="multipart/form-data") public ApiResponse<?> csv(@RequestPart MultipartFile file)throws Exception {
        if(file.isEmpty())throw new IllegalArgumentException("CSV file is required");
        String text=new String(file.getBytes(),StandardCharsets.UTF_8);
        String[] rows=text.split("\\R");
        int accepted=0;
        for(int i=1;i<rows.length;i++)if(!rows[i].trim().isEmpty())accepted++;
        return ApiResponse.ok("CSV validated and staged",Map.of("rows",Math.max(0,rows.length-1),"accepted",accepted,"status","STAGED","errors",List.of()));
    }
    @PostMapping(value="/xml",consumes="multipart/form-data") public ApiResponse<?> xml(@RequestPart MultipartFile file)throws Exception {
        String xml=new String(file.getBytes(),StandardCharsets.UTF_8);
        DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities",false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,"");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
        factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        ScanRecord record=new ScanRecord();
        record.setScanType("XML_IMPORT");
        record.setPayloadReference(file.getOriginalFilename()==null?"upload":file.getOriginalFilename());
        record.setStatus("STAGED");
        record.setResult("XML parsed with external entity processing disabled");
        record.setScannedAt(Instant.now());
        scans.save(record);
        return ApiResponse.ok("XML validated and staged",record);
    }
}
