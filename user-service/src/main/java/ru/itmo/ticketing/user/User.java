package ru.itmo.ticketing.user;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Table("users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    private Long id;

    private String email;

    @Setter
    private String fullName;

    @Setter
    private UserRole role;

    private OffsetDateTime createdAt;

    public User(String email, String fullName, UserRole role) {
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.createdAt = OffsetDateTime.now();
    }
}
