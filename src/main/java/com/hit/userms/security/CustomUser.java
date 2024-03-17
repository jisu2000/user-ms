package com.hit.userms.security;import com.hit.userms.model.UserEO;import lombok.AllArgsConstructor;import lombok.Builder;import lombok.Data;import lombok.NoArgsConstructor;import org.springframework.security.core.GrantedAuthority;import org.springframework.security.core.authority.SimpleGrantedAuthority;import org.springframework.security.core.userdetails.User;import org.springframework.security.core.userdetails.UserDetails;import java.util.Collection;import java.util.HashSet;import java.util.Set;@NoArgsConstructor@AllArgsConstructor@Data@Builderpublic class CustomUser implements UserDetails {    private UserEO userEO;    @Override    public Collection<? extends GrantedAuthority> getAuthorities() {        Set<GrantedAuthority> authorities = new HashSet<>();        authorities.add(new SimpleGrantedAuthority("ROLE_" + userEO.getRole().name()));        return authorities;    }    @Override    public String getPassword() {        return userEO.getPassword();    }    @Override    public String getUsername() {        return userEO.getEmail();    }    @Override    public boolean isAccountNonExpired() {        return true;    }    @Override    public boolean isAccountNonLocked() {        return true;    }    @Override    public boolean isCredentialsNonExpired() {        return true;    }    @Override    public boolean isEnabled() {        return true;    }}