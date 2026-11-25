package com.examly.springapp.service;
import com.examly.springapp.entity.Permission;
import com.examly.springapp.entity.RolePermission;
import com.examly.springapp.enums.Role;
import com.examly.springapp.repository.PermissionRepository;
import com.examly.springapp.repository.RolePermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service("permissionService")
public class PermissionService {
    private final PermissionRepository permissions;
    private final RolePermissionRepository rolePermissions;
    public PermissionService(PermissionRepository permissions,RolePermissionRepository rolePermissions) {
        this.permissions=permissions;
        this.rolePermissions=rolePermissions;
    }
    @Transactional(readOnly=true) public boolean has(Role role,String code) {
        Optional<Permission> p=permissions.findAll().stream().filter(x->x.getCode().equalsIgnoreCase(code)).findFirst();
        return p.isPresent()&&rolePermissions.findAll().stream().anyMatch(rp->rp.isEnabled()&&rp.getRole()==role&&rp.getPermission().getId().equals(p.get().getId()));
    }
    @Transactional public Permission ensure(String code,String description) {
        return permissions.findAll().stream().filter(p->p.getCode().equalsIgnoreCase(code)).findFirst().orElseGet(()-> {
            Permission p=new Permission();p.setCode(code.toUpperCase(Locale.ROOT));p.setDescription(description);return permissions.save(p);
        });
    }
    @Transactional public void set(Role role,String code,boolean enabled) {
        Permission p=ensure(code,"Runtime permission "+code);
        RolePermission rp=rolePermissions.findAll().stream().filter(x->x.getRole()==role&&x.getPermission().getId().equals(p.getId())).findFirst().orElseGet(RolePermission::new);
        rp.setRole(role);
        rp.setPermission(p);
        rp.setEnabled(enabled);
        rolePermissions.save(rp);
    }
}
