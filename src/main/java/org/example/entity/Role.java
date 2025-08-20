package org.example.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
@Getter
@Setter
public class Role {

    public enum ERole {
        ROLE_USER("User", "Basic user privileges"),
        ROLE_ADMIN("Administrator", "Full system access"),
        ROLE_MODERATOR("Moderator", "Content moderation privileges"),
        ROLE_SUPPORT("Support", "Customer support access"),
        ROLE_DEVELOPER("Developer", "Development and testing access"),
        ROLE_VIP("VIP User", "Premium user privileges");

        private final String displayName;
        private final String description;

        ERole(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false, unique = true)
    private ERole name;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Связи с пользователями (для статистики)
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<User> users = new HashSet<>();

    public Role() {}

    public Role(ERole name) {
        this.name = name;
        this.description = name.getDescription();
    }

    // Полезные методы
    public String getDisplayName() {
        return name.getDisplayName();
    }

    public boolean isAdminRole() {
        return name == ERole.ROLE_ADMIN;
    }

    public boolean isModeratorOrHigher() {
        return name == ERole.ROLE_ADMIN || name == ERole.ROLE_MODERATOR;
    }
}