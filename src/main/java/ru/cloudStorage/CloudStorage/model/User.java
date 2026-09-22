package ru.cloudStorage.CloudStorage.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "users")
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String userName;
    private String password;
    private String role;

    public User(String userName, String password) {
        this.userName = userName;
        this.password = password;
        this.role = "ROLE_USER";
    }
}
