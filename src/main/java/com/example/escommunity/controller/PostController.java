package com.example.escommunity.controller;

import com.example.escommunity.dto.PostDto;
import com.example.escommunity.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/post")
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<?> write(@RequestHeader("Authorization") String token,
                                   @RequestPart("post") PostDto postDto,
                                   @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return postService.write(token, postDto, files);
    }

    @GetMapping
    public Page<?> getPosts(int page, int size){
        return postService.getPosts(page, size);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<?> getPost(@PathVariable Long postId) {
        return postService.getPost(postId);
    }

    @PostMapping("/{postId}")
    public ResponseEntity<?> reaction(@RequestHeader("Authorization") String token,
                                      @PathVariable Long postId) {
        return postService.reaction(token, postId);
    }

    @PatchMapping("/{postId}")
    public ResponseEntity<?> update(@RequestHeader("Authorization") String token,
                                    @PathVariable Long postId,
                                    @RequestPart("post") PostDto postDto){
        return postService.update(token, postId, postDto);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<?> delete(@RequestHeader("Authorization") String token,
                                    @PathVariable Long postId) {
        return postService.delete(token, postId);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searcch(@RequestParam String keyword,
                                     @RequestParam String type,
                                     int page, int size) {
        return postService.search(keyword, type, page, size);
    }

}
