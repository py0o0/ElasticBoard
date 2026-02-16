package com.example.escommunity.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long postFileId;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private Post post;

    private String url;
}
