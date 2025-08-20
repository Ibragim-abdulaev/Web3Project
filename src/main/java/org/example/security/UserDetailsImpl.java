package org.example.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.example.entity.Role;
import org.example.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class UserDetailsImpl implements UserDetails {
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String username;
    private final String email;
    private final String displayName;
    private final String primaryWalletAddress;
    @JsonIgnore
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean enabled;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean twoFactorEnabled;
    private final boolean emailVerified;
    private final User.UserStatus userStatus;
    private final LocalDateTime createdAt;
    private final LocalDateTime lastLoginAt;
    private final Long loginCount;
    private final boolean profilePublic;
    private final boolean showBalance;
    private final String avatarUrl;

    public UserDetailsImpl(Long id, String username, String email, String displayName,
                           String primaryWalletAddress, String password,
                           Collection<? extends GrantedAuthority> authorities,
                           boolean enabled, boolean accountNonExpired, boolean accountNonLocked,
                           boolean credentialsNonExpired, boolean twoFactorEnabled,
                           boolean emailVerified, User.UserStatus userStatus,
                           LocalDateTime createdAt, LocalDateTime lastLoginAt, Long loginCount,
                           boolean profilePublic, boolean showBalance, String avatarUrl) {

        this.id = id;
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.primaryWalletAddress = primaryWalletAddress;
        this.password = password;
        this.authorities = authorities;
        this.enabled = enabled;
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
        this.twoFactorEnabled = twoFactorEnabled;
        this.emailVerified = emailVerified;
        this.userStatus = userStatus;
        this.createdAt = createdAt;
        this.lastLoginAt = lastLoginAt;
        this.loginCount = loginCount;
        this.profilePublic = profilePublic;
        this.showBalance = showBalance;
        this.avatarUrl = avatarUrl;
    }

    public UserDetailsImpl(Long id, String username, String email, String password,
                           Collection<? extends GrantedAuthority> authorities) {
        this(id, username, email, null, null, password, authorities,
                true, true, true, true, false, false, User.UserStatus.ACTIVE,
                LocalDateTime.now(), null, 0L, true, false, null);
    }

    public static UserDetailsImpl build(User user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .filter(Role::getIsActive)
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());

        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getPrimaryWalletAddress(),
                user.getPassword(),
                authorities,
                user.getStatus() == User.UserStatus.ACTIVE,
                user.getStatus() != User.UserStatus.DELETED,
                user.getStatus() != User.UserStatus.BANNED && user.getStatus() != User.UserStatus.SUSPENDED,
                true,
                user.getTwoFactorEnabled() != null ? user.getTwoFactorEnabled() : false,
                user.getIsVerified() != null ? user.getIsVerified() : false,
                user.getStatus(),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                user.getLoginCount() != null ? user.getLoginCount() : 0L,
                user.getProfilePublic() != null ? user.getProfilePublic() : true,
                user.getShowBalance() != null ? user.getShowBalance() : false,
                user.getAvatarUrl()
        );
    }

    public static UserDetailsImpl buildSimple(String username, String password, List<String> roles) {
        List<GrantedAuthority> authorities = roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new UserDetailsImpl(null, username, null, password, authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName != null && !displayName.trim().isEmpty() ? displayName : username;
    }

    public String getPrimaryWalletAddress() {
        return primaryWalletAddress;
    }

    public boolean isTwoFactorEnabled() {
        return twoFactorEnabled;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public User.UserStatus getUserStatus() {
        return userStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public Long getLoginCount() {
        return loginCount;
    }

    public boolean isProfilePublic() {
        return profilePublic;
    }

    public boolean isShowBalance() {
        return showBalance;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public boolean hasRole(String role) {
        return authorities.stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }

    public boolean hasAnyRole(String... roles) {
        for (String role : roles) {
            if (hasRole(role)) {
                return true;
            }
        }
        return false;
    }
    public boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }
    public boolean isModeratorOrHigher() {
        return hasAnyRole("ROLE_ADMIN", "ROLE_MODERATOR");
    }
    public boolean isVip() {
        return hasRole("ROLE_VIP");
    }
    public boolean isActiveUser() {
        return userStatus == User.UserStatus.ACTIVE && enabled && accountNonLocked;
    }
    public boolean needsEmailVerification() {
        return !emailVerified && email != null && !email.isEmpty();
    }
    public boolean isWalletOnlyUser() {
        return password == null || password.isEmpty();
    }
    public boolean isInactive(int daysThreshold) {
        if (lastLoginAt == null) {
            return false;
        }
        return lastLoginAt.isBefore(LocalDateTime.now().minusDays(daysThreshold));
    }

    public String getDisplayUsername() {
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName;
        }
        if (email != null && !email.trim().isEmpty()) {
            return email;
        }
        return username;
    }

    public String getShortWalletAddress() {
        if (primaryWalletAddress == null || primaryWalletAddress.length() < 10) {
            return primaryWalletAddress;
        }
        return primaryWalletAddress.substring(0, 6) + "..." +
                primaryWalletAddress.substring(primaryWalletAddress.length() - 4);
    }
    public List<String> getRoleNames() {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

    public String getStatusDisplay() {
        if (!enabled) return "Disabled";
        if (!accountNonLocked) return "Locked";
        if (!accountNonExpired) return "Expired";
        return userStatus != null ? userStatus.name() : "Unknown";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserDetailsImpl that = (UserDetailsImpl) o;
        return Objects.equals(id, that.id) && Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, username);
    }

    @Override
    public String toString() {
        return "UserDetailsImpl{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", enabled=" + enabled +
                ", accountNonLocked=" + accountNonLocked +
                ", twoFactorEnabled=" + twoFactorEnabled +
                ", userStatus=" + userStatus +
                ", authorities=" + authorities.size() +
                '}';
    }
    public UserDetailsImpl withUpdatedLoginInfo(LocalDateTime newLastLogin, Long newLoginCount) {
        return new UserDetailsImpl(
                this.id, this.username, this.email, this.displayName,
                this.primaryWalletAddress, this.password, this.authorities,
                this.enabled, this.accountNonExpired, this.accountNonLocked,
                this.credentialsNonExpired, this.twoFactorEnabled,
                this.emailVerified, this.userStatus,
                this.createdAt, newLastLogin, newLoginCount,
                this.profilePublic, this.showBalance, this.avatarUrl
        );
    }

    public UserDetailsImpl withUpdatedAuthorities(Collection<? extends GrantedAuthority> newAuthorities) {
        return new UserDetailsImpl(
                this.id, this.username, this.email, this.displayName,
                this.primaryWalletAddress, this.password, newAuthorities,
                this.enabled, this.accountNonExpired, this.accountNonLocked,
                this.credentialsNonExpired, this.twoFactorEnabled,
                this.emailVerified, this.userStatus,
                this.createdAt, this.lastLoginAt, this.loginCount,
                this.profilePublic, this.showBalance, this.avatarUrl
        );
    }
}