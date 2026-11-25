package com.examly.springapp.service;
import com.examly.springapp.dto.request.CropRequests.CropRequest;
import com.examly.springapp.entity.Crop;
import com.examly.springapp.enums.Season;
import com.examly.springapp.exception.ResourceNotFoundException;
import com.examly.springapp.repository.CropRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service public class CropService {
    private final CropRepository repo;
    private final AuditService audit;
    public CropService(CropRepository repo,AuditService audit) {
        this.repo=repo;
        this.audit=audit;
    }
    @Transactional public Crop create(CropRequest r) {
        Crop c=new Crop();
        c.setCropName(r.cropName().trim());
        c.setSeason(r.season());
        c.setStandardYieldKgHa(r.standardYieldKgHa());
        c.setMspRate(r.mspRate());
        Crop saved=repo.save(c);
        audit.log("CREATE","CROP",String.valueOf(saved.getId()),"SUCCESS",null,saved.getCropName(),"crop master");
        return saved;
    }
    public List<Crop> list(Season season) {
        return season==null?repo.findAll():repo.findBySeason(season);
    }
    public Crop get(Long id) {
        return repo.findById(id).orElseThrow(()->new ResourceNotFoundException("Crop not found"));
    }
    @Transactional public Crop update(Long id,CropRequest r) {
        Crop c=get(id);
        c.setCropName(r.cropName().trim());
        c.setSeason(r.season());
        c.setStandardYieldKgHa(r.standardYieldKgHa());
        c.setMspRate(r.mspRate());
        Crop saved=repo.save(c);
        audit.log("UPDATE","CROP",String.valueOf(id),"SUCCESS",null,saved.getCropName(),"crop master");
        return saved;
    }
    @Transactional public void delete(Long id) {
        Crop c=get(id);
        c.setActive(false);
        repo.save(c);
        audit.log("DEACTIVATE","CROP",String.valueOf(id),"SUCCESS","ACTIVE","INACTIVE","crop master");
    }
}
