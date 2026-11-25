package com.examly.springapp.security;
import com.examly.springapp.entity.User;
import com.examly.springapp.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
@Service public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository repo;
    public CustomUserDetailsService(UserRepository r) {
        repo=r;
    } public UserDetails loadUserByUsername(String s)throws UsernameNotFoundException {
        User u=repo.findByUserId(s).orElseThrow(()->new UsernameNotFoundException("User not found"));
        return UserDetailsImpl.of(u);
    } public record UserDetailsImpl(Long id,String username,String password,String role,boolean enabled) implements UserDetails {
        static UserDetailsImpl of(User u) {
            return new UserDetailsImpl(u.getId(),u.getUserId(),u.getPasswordHash(),u.getRole().name(),u.isActive());
        } @Override public String getUsername() { return username; }
        @Override public String getPassword() { return password; }
        @Override public Collection<? extends GrantedAuthority> getAuthorities() {
            return List.of(new SimpleGrantedAuthority("ROLE_"+role));
        } public boolean isAccountNonExpired() {
            return true;
        } public boolean isAccountNonLocked() {
            return true;
        } public boolean isCredentialsNonExpired() {
            return true;
        } public boolean isEnabled() {
            return enabled;
        }
    }
}
