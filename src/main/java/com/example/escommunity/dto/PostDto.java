package com.example.escommunity.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostDto {
    private Long postId;
    private String createdAt;
    private String title;
    private String content;
    private Long views;
    private Long heartCount;
    private Integer isFile;
    private List<String> urls;

    private Long userId;
    private String email;
}
