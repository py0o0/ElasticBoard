package com.example.escommunity.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommentDto {
    private Long commentId;
    private String createdAt;
    private String content;
    private Long heartCount;

    private Long userId;
    private String email;
}
