package com.examly.springapp.controller;
import com.examly.springapp.dto.response.ApiResponse;
import com.examly.springapp.entity.User;
import com.examly.springapp.enums.Role;
import com.examly.springapp.repository.UserRepository;
import com.examly.springapp.service.AuditService;
import com.examly.springapp.util.SecurityUtil;
import jakarta.validation.constraints.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/users")
public class UserController {
    private final UserRepository repo;
    private final AuditService audit;
    public UserController(UserRepository repo,AuditService audit) {
        this.repo=repo;
        this.audit=audit;
    }
    @GetMapping("/surveyors") @PreAuthorize("hasAnyRole('INSURANCE_OFFICER','STATE_OFFICER','ADMIN')") public ApiResponse<?> surveyors() {
        return ApiResponse.ok("Surveyors",repo.findByRoleAndIsActiveTrue(Role.SURVEYOR).stream().map(u->Map.of("id",u.getId(),"name",u.getName(),"userId",u.getUserId())).toList());
    }
    @GetMapping("/profile") @PreAuthorize("isAuthenticated()") public ApiResponse<?> profile() {
        return ApiResponse.ok("Profile",view(current()));
    }
    @PutMapping("/profile") @PreAuthorize("isAuthenticated()") public ApiResponse<?> update(@RequestBody ProfileRequest r) {
        User u=current();
        if(r.name()!=null) {
            if(!r.name().matches("[A-Za-z ]{2,100}"))throw new IllegalArgumentException("Name must not contain numbers or special characters");
            u.setName(r.name().trim());
        }if(r.phone()!=null) {
            if(!r.phone().matches("\\d{10}"))throw new IllegalArgumentException("Phone Number must be exactly 10 digits long");
            if(repo.existsByPhoneNumber(r.phone())&&!r.phone().equals(u.getPhoneNumber()))throw new IllegalArgumentException("This phone number is already registered");
            u.setPhoneNumber(r.phone());
        }repo.save(u);
        audit.log("PROFILE_UPDATE","USER",String.valueOf(u.getId()),"SUCCESS",null,null,"profile management");
        return ApiResponse.ok("Profile updated",view(u));
    }
    private User current() {
        return repo.findByUserId(SecurityUtil.username()).orElseThrow(()->new IllegalArgumentException("User not found"));
    }
    private Map<String,Object> view(User u) {
        return Map.of("id",u.getId(),"userId",u.getUserId(),"name",u.getName(),"email",u.getEmail(),"phone",u.getPhoneNumber(),"role",u.getRole(),"active",u.isActive(),"emailVerified",u.isEmailVerified(),"supervisorApproved",u.isSupervisorApproved());
    }
    public record ProfileRequest(@Size(min=2,max=100)String name,@Pattern(regexp="\\d{10}")String phone) {
    }
}
