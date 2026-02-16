package com.example.escommunity.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long postId;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String createdAt;
    private String title;
    private String content;
    private long views;
    private long heartCount;
    private int isFile;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now().toString();
    }
}
