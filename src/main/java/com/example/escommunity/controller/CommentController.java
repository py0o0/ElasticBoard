package com.example.escommunity.controller;

import com.example.escommunity.dto.CommentDto;
import com.example.escommunity.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/post/comment/{postId}")
    public ResponseEntity<?> write(@RequestHeader("Authorization") String token,
                                   @RequestBody CommentDto commentDto,
                                   @PathVariable Long postId) {
        return commentService.write(token, commentDto, postId);
    }

    @PostMapping("/comment/{commentId}")
    public ResponseEntity<?> reaction(@RequestHeader("Authorization") String token,
                                      @PathVariable Long commentId) {
        return commentService.reaction(token, commentId);
    }

    @PatchMapping("/comment/{commentId}")
    public ResponseEntity<?> update(@RequestHeader("Authorization") String token,
                                    @PathVariable Long commentId,
                                    @RequestPart("comment") CommentDto commentDto) {
        return commentService.update(token, commentId, commentDto);
    }

    @DeleteMapping("/comment/{commentId}")
    public ResponseEntity<?> delete(@RequestHeader("Authorization") String token,
                                    @PathVariable Long commentId) {
        return commentService.delete(token, commentId);
    }
}
