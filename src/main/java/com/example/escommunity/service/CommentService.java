package com.example.escommunity.service;

import com.example.escommunity.dto.CommentDto;
import com.example.escommunity.entity.*;
import com.example.escommunity.jwt.JwtUtil;
import com.example.escommunity.repository.CommentReactionRepository;
import com.example.escommunity.repository.CommentRepository;
import com.example.escommunity.repository.PostRepository;
import com.example.escommunity.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentReactionRepository commentReactionRepository;

    private final JwtUtil jwtUtil;

    private Optional<User> verifyToken(String token) {    // 토큰 검증 함수
        try {
            long userId = jwtUtil.getUserid(token);
            User user = userRepository.findById(userId).orElse(null);
            if(user == null) {
                return Optional.empty();
            }
            return Optional.of(user);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public ResponseEntity<?> write(String token, CommentDto commentDto, Long postId) {
        Optional<User> user = verifyToken(token);
        if(user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }
        Post post = postRepository.findById(postId).orElse(null);
        if (post == null) {
            return ResponseEntity.badRequest().body("존재하지 않는 게시글입니다.");
        }

        Comment comment = Comment.builder()
                .content(commentDto.getContent())
                .heartCount(0)
                .post(post)
                .user(user.get())
                .build();

        commentRepository.save(comment);

        commentDto = CommentDto.builder()
                .commentId(comment.getCommentId())
                .content(comment.getContent())
                .heartCount(comment.getHeartCount())
                .createdAt(comment.getCreatedAt())
                .build();
        return ResponseEntity.ok(commentDto);
    }

    @Transactional
    public ResponseEntity<?> reaction(String token, Long commentId) {
        User user = verifyToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글"));

        Optional<CommentReaction> reactionOpt = commentReactionRepository.findByCommentAndUser(comment, user);
        CommentReaction commentReaction = reactionOpt.orElse(null);
        if(commentReaction == null) {
            commentReaction = CommentReaction.builder()
                    .comment(comment)
                    .user(user)
                    .build();
            commentReactionRepository.save(commentReaction);
        } else {
            commentReactionRepository.delete(commentReaction);
        }

        // 동시성 문제 방시를 위해 Setter 사용 X, 쿼리로 좋아요 개수 다시 세기
        long heartCount = commentReactionRepository.countByComment(comment);
        comment.setHeartCount(heartCount);

        return ResponseEntity.ok("리액션 성공");
    }

    @Transactional
    public ResponseEntity<?> update(String token, Long commentId, CommentDto commentDto) {
        User user = verifyToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글"));

        if(comment == null) {
            return ResponseEntity.badRequest().body("존재하지 않는 댓글");
        }

        if(!comment.getUser().getEmail().equals(user.getEmail())) {
            return ResponseEntity.badRequest().body("본인의 댓글만 수정할 수 있습니다.");
        }
        comment.setContent(commentDto.getContent());
        return ResponseEntity.ok("댓글 수정 성공");
    }

    @Transactional
    public ResponseEntity<?> delete(String token, Long commentId) {
        User user = verifyToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글"));

        if(comment == null) {
            return ResponseEntity.badRequest().body("존재하지 않는 댓글");
        }

        if(!comment.getUser().getEmail().equals(user.getEmail())) {
            return ResponseEntity.badRequest().body("본인의 댓글만 삭제할 수 있습니다.");
        }
        commentRepository.delete(comment);
        return ResponseEntity.ok("댓글 삭제 성공");
    }
}
