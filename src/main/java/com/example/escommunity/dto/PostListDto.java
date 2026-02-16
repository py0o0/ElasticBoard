package com.example.escommunity.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostListDto {
    private Long postId;
    private String createdAt;
    private String title;
    private Long views;
    private Long heartCount;

    private Long userId;
    private String email;

    private Long commentCount;
}
