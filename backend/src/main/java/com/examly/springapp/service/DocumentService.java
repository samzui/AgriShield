package com.examly.springapp.service;
import com.examly.springapp.dto.request.DocumentRequests.RollbackRequest;
import com.examly.springapp.entity.*;
import com.examly.springapp.enums.DocumentStatus;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.integration.DocumentStorageService;
import com.examly.springapp.repository.*;
import com.examly.springapp.util.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
@Service
public class DocumentService {
    private final DocumentRepository docs;
    private final DocumentVersionRepository versions;
    private final DocumentStorageService storage;
    private final DigitalSignatureRepository signatures;
    private final AuditService audit;
    private final UserRepository users;
    public DocumentService(DocumentRepository docs,DocumentVersionRepository versions,DocumentStorageService storage,DigitalSignatureRepository signatures,AuditService audit,UserRepository users) {
        this.docs=docs;
        this.versions=versions;
        this.storage=storage;
        this.signatures=signatures;
        this.audit=audit;
        this.users=users;
    }
    @Transactional public Document upload(String entityType,Long entityId,String category,String sensitivity,LocalDate expiry,LocalDate retention,MultipartFile file)throws IOException {
        validateFile(file);
        String key=storage.store(file);
        Document d=new Document();
        d.setEntityType(entityType);
        d.setEntityId(entityId);
        d.setCategory(category);
        d.setSensitivity(sensitivity);
        d.setOriginalFilename(file.getOriginalFilename());
        d.setStorageKey(key);
        d.setExpiryDate(expiry);
        d.setRetentionDate(retention);
        d.setMimeType(file.getContentType());
        d.setSizeBytes(file.getSize());
        d.setChecksum(checksum(file.getBytes()));
        d.setStatus(DocumentStatus.ACTIVE);
        d.setRetentionPolicyCode("DEFAULT_7Y");
        d.setSigned(false);
        docs.save(d);
        saveVersion(d,key,d.getChecksum(),"Initial upload");
        audit.log("UPLOAD","DOCUMENT",String.valueOf(d.getId()),"SUCCESS",null,d.getOriginalFilename(),"document upload");
        return d;
    }
    @Transactional public Document addVersion(Long id,MultipartFile file,String comment)throws IOException {
        validateFile(file);
        Document d=get(id);
        String key=storage.store(file);
        String sum=checksum(file.getBytes());
        d.setStorageKey(key);
        d.setChecksum(sum);
        d.setOriginalFilename(file.getOriginalFilename());
        d.setMimeType(file.getContentType());
        d.setSizeBytes(file.getSize());
        d.setSigned(false);
        docs.save(d);
        saveVersion(d,key,sum,comment);
        audit.log("DOCUMENT_VERSION","DOCUMENT",String.valueOf(id),"SUCCESS",null,null,"document version");
        return d;
    }
    public Document get(Long id) {
        return docs.findById(id).orElseThrow(()->new ResourceNotFoundException("Document not found"));
    }
    public List<DocumentVersion> versions(Long id) {
        get(id);
        return versions.findByDocumentIdOrderByVersionNumberDesc(id);
    }
    @Transactional public Document rollback(Long id,RollbackRequest r) {
        Document d=get(id);
        DocumentVersion v=versions.findByDocumentIdAndVersionNumber(id,r.versionNumber()).orElseThrow(()->new ResourceNotFoundException("Document version not found"));
        d.setStorageKey(v.getStorageKey());
        d.setChecksum(v.getChecksum());
        d.setSigned(false);
        Document saved=docs.save(d);
        audit.log("ROLLBACK","DOCUMENT",String.valueOf(id),"SUCCESS",null,String.valueOf(r.versionNumber()),r.reason());
        return saved;
    }
    @Transactional public DigitalSignature sign(Long id,String reference,String algorithm) {
        Document d=get(id);
        DigitalSignature s=new DigitalSignature();
        s.setDocument(d);
        s.setSignerUserId(currentUserId());
        s.setSignatureReference(reference);
        s.setAlgorithm(algorithm);
        s.setSignedAt(Instant.now());
        d.setSigned(true);
        docs.save(d);
        DigitalSignature saved=signatures.save(s);
        audit.log("SIGN","DOCUMENT",String.valueOf(id),"SUCCESS",null,reference,"digital signature");
        return saved;
    }
    // The signer's real name is resolved here (not just the raw signerUserId) so the document UI can show
    // "signed by" without a separate user lookup — reuses the existing DigitalSignature/User data as-is.
    public Map<String,Object> latestSignatureView(Long documentId) {
        return signatures.findByDocumentId(documentId).stream()
            .max(Comparator.comparing(DigitalSignature::getSignedAt))
            .<Map<String,Object>>map(s -> {
                Map<String,Object> m = new LinkedHashMap<>();
                m.put("id", s.getId());
                m.put("signerUserId", s.getSignerUserId());
                m.put("signerName", s.getSignerUserId() != null ? users.findById(s.getSignerUserId()).map(User::getName).orElse(null) : null);
                m.put("signatureReference", s.getSignatureReference());
                m.put("algorithm", s.getAlgorithm());
                m.put("signedAt", s.getSignedAt());
                return m;
            }).orElse(null);
    }
    public List<Document> search(String q) {
        String query=q==null?"":q.toLowerCase();
        return docs.findAll().stream().filter(d->contains(d.getOriginalFilename(),query)||contains(d.getCategory(),query)||contains(d.getEntityType(),query)||contains(d.getSensitivity(),query)).toList();
    }
    @Transactional public void archiveExpired() {
        LocalDate today=LocalDate.now();
        docs.findAll().stream().filter(d->d.getRetentionDate()!=null&&!d.getRetentionDate().isAfter(today)&&d.getStatus()==DocumentStatus.ACTIVE).forEach(d-> {
            d.setStatus(DocumentStatus.ARCHIVED);docs.save(d);audit.log("ARCHIVE","DOCUMENT",String.valueOf(d.getId()),"SUCCESS","ACTIVE","ARCHIVED","retention policy");
        });
    }
    private void saveVersion(Document d,String key,String sum,String comment) {
        int next=versions.findByDocumentIdOrderByVersionNumberDesc(d.getId()).stream().mapToInt(DocumentVersion::getVersionNumber).max().orElse(0)+1;
        DocumentVersion v=new DocumentVersion();
        v.setDocument(d);
        v.setVersionNumber(next);
        v.setStorageKey(key);
        v.setChecksum(sum);
        v.setUploadedBy(currentUserId());
        v.setUploadedAt(Instant.now());
        v.setComment(comment);
        versions.save(v);
    }
    private void validateFile(MultipartFile file) {
        if(file==null||file.isEmpty())throw new IllegalArgumentException("Document file is required");
        if(file.getSize()>20L*1024*1024)throw new IllegalArgumentException("Document exceeds the 20 MB limit");
        String type=file.getContentType();
        if(type==null||!(type.equals("application/pdf")||type.startsWith("image/")||type.equals("text/csv")||type.contains("spreadsheetml")))throw new IllegalArgumentException("Unsupported document MIME type");
    }
    private String checksum(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        }catch(Exception e) {
            throw new IllegalStateException(e);
        }
    }
    private boolean contains(String value,String q) {
        return value!=null&&value.toLowerCase().contains(q);
    }
    // SecurityUtil.username() returns the authenticated user's string userId (e.g. "insurance_officer"),
    // not the numeric primary key — it must be resolved through the user repository, not parsed directly.
    private Long currentUserId() {
        try {
            return users.findByUserId(SecurityUtil.username()).map(User::getId).orElse(null);
        }catch(Exception e) {
            return null;
        }
    }
}
