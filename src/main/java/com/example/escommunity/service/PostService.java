package com.example.escommunity.service;

import com.example.escommunity.dto.CommentDto;
import com.example.escommunity.dto.PostDto;
import com.example.escommunity.dto.PostListDto;
import com.example.escommunity.entity.*;
import com.example.escommunity.jwt.JwtUtil;
import com.example.escommunity.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class

PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostFileRepository postFileRepository;
    private final PostReactionRepository postReactionRepository;
    private final CommentRepository commentRepository;
    private final PostDocumentRepository postDocumentRepository;

    private final JwtUtil jwtUtil;
    private final AwsS3Service awsS3Service;
    private final RedisService redisService;

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

    public ResponseEntity<?> write(String token, PostDto postDto, List<MultipartFile> files) {
        Optional<User> user = verifyToken(token);
        if(user.isEmpty()) {
            return ResponseEntity.badRequest().body("유효하지 않은 토큰");
        }

        Post post = Post.builder()
                .title(postDto.getTitle())
                .content(postDto.getContent())
                .views(0)
                .heartCount(0)
                .user(user.get())
                .isFile(files != null && !files.isEmpty() ? 1 : 0)
                .build();

        post = postRepository.save(post);

        List<String> urls = new ArrayList<>();
        if(files != null && !files.isEmpty()){
            for(MultipartFile file : files){
                try{
                    String url = awsS3Service.upload(file);

                    PostFile postFile = PostFile.builder()
                            .post(post)
                            .url(url)
                            .build();
                    postFileRepository.save(postFile);

                    urls.add(url);
                }catch(Exception e){
                    urls.forEach(awsS3Service::delete);
                    postRepository.delete(post);
                    return ResponseEntity.badRequest().body("잘못된 파일 형식");
                }
            }
        }

        // Elasticsearch에 문서 저장
        PostDocument document = PostDocument.builder()
                .postId(post.getPostId())
                .content(post.getContent())
                .email(user.get().getEmail())
                .title(post.getTitle())
                .build();
        postDocumentRepository.save(document);

        postDto = PostDto.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .views(post.getViews())
                .heartCount(post.getHeartCount())
                .email(user.get().getEmail())
                .urls(urls)
                .userId(user.get().getUserId())
                .isFile(post.getIsFile())
                .build();

        return ResponseEntity.ok().body(postDto);
    }

    public Page<?> getPosts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PostListDto> postDtoPage = postRepository.findPostList(pageable); // user와 join 해서 한번 쿼리
        List<PostListDto> postListDto = postDtoPage.getContent();

        List<Long> postIds = postListDto.stream().map(PostListDto::getPostId).toList();

        // in query로 댓글 개수 한꺼번에 조회 후 매핑
        Map<Long, Long> commentCountMap = commentRepository.countByPostIds(postIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));

        // N+1 문제 해결을 위해 2번의 쿼리

        postListDto.forEach(postDto ->
                postDto.setCommentCount(commentCountMap.getOrDefault(postDto.getPostId(), 0L)));

        return new PageImpl<>(postListDto, pageable, postDtoPage.getTotalElements());
    }

    public ResponseEntity<?> getPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글"));

        redisService.incrementView(post.getPostId()); // 조회수 증가

        List<PostFile> postFile = postFileRepository.findByPost(post);
        List<String> urls = postFile.stream()
                .map(PostFile::getUrl)
                .collect(Collectors.toList());

        PostDto postDto = PostDto.builder()
                .postId(post.getPostId())
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .title(post.getTitle())
                .views(post.getViews())
                .heartCount(post.getHeartCount())
                .email(post.getUser().getEmail())
                .userId(post.getUser().getUserId())
                .urls(urls)
                .build();

        List<Comment> commentList = commentRepository.findByPostWithUser(post); // join fetch로 user 정보 한번에 조회
        List<CommentDto> commentDtoList = commentList.stream().map(comment -> CommentDto.builder()
                .commentId(comment.getCommentId())
                .content(comment.getContent())
                .email(comment.getUser().getEmail())
                .userId(comment.getUser().getUserId())
                .createdAt(comment.getCreatedAt())
                .heartCount(comment.getHeartCount())
                .build()).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "post", postDto,
                "comments", commentDtoList
        ));
    }

    @Transactional // 2개의 쿼리가 하나의 트랜잭션
    public ResponseEntity<?> reaction(String token, Long postId) {
        User user = verifyToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글"));

        Optional<PostReaction> reactionOpt = postReactionRepository.findByPostAndUser(post, user);
        PostReaction postReaction = reactionOpt.orElse(null);
        if(postReaction == null) {
            postReaction = PostReaction.builder()
                    .post(post)
                    .user(user)
                    .build();
            postReactionRepository.save(postReaction);
        } else {
            postReactionRepository.delete(postReaction);
        }

        // 동시성 문제 방시를 위해 Setter 사용 X, 쿼리로 좋아요 개수 다시 세기
        long heartCount = postReactionRepository.countByPost(post);
        post.setHeartCount(heartCount);

        return ResponseEntity.ok("리액션 성공");
    }

    @Transactional
    public ResponseEntity<?> update(String token, Long postId, PostDto postDto) {
        User user = verifyToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글"));

        if(post == null) {
            return ResponseEntity.badRequest().body("존재하지 않는 게시글");
        }

        if(!post.getUser().getEmail().equals(user.getEmail())) {
            return ResponseEntity.badRequest().body("본인의 게시글만 수정할 수 있습니다.");
        }
        post.setTitle(postDto.getTitle());
        post.setContent(postDto.getContent());

        PostDocument document = PostDocument.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .build();

        postDocumentRepository.save(document); // Elasticsearch 문서 업데이트

        return ResponseEntity.ok("게시글 수정 성공");
    }

    @Transactional
    public ResponseEntity<?> delete(String token, Long postId) {
        User user = verifyToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 토큰"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글"));

        if(post == null) {
            return ResponseEntity.badRequest().body("존재하지 않는 게시글");
        }

        if(!post.getUser().getEmail().equals(user.getEmail())) {
            return ResponseEntity.badRequest().body("본인의 게시글만 삭제할 수 있습니다.");
        }
        postRepository.delete(post);
        postDocumentRepository.deleteById(postId); // Elasticsearch 문서 삭제

        return ResponseEntity.ok("게시글 삭제 성공");
    }


    public ResponseEntity<?> search(String keyword, String type, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        List<PostDocument> postDocument;

        switch(type){ // relevance 점수로 es에서 조회
            case "title" :
                postDocument = postDocumentRepository.findByTitleContaining(keyword, pageable); break;
            case "content" :
                postDocument = postDocumentRepository.findByContentContaining(keyword, pageable); break;
            case "title_content" :
                postDocument = postDocumentRepository.findByTitleContainingOrContentContaining(keyword, keyword, pageable); break;
            default:
                return ResponseEntity.badRequest().body("잘못된 검색 타입");
        }

        long total = postDocumentRepository.count(); // ES에서 총 문서 수 조회

        List<Long> postIdList = postDocument.stream() // ES에서 postId 추출
                .map(PostDocument::getPostId)
                .toList();

        List<Post> postList = postRepository.findAllByIdWithUser(postIdList); //Join Fetch로 user 정보 한번에 조회
        Map<Long, Post> postMap = postList.stream()
                .collect(Collectors.toMap(Post::getPostId, post -> post));
        postList = postIdList.stream()
                .map(postMap::get)
                .toList(); // ES에서 조회된 순서대로 정렬

        List<PostListDto> postDtoList = postList.stream()
                .map(post -> PostListDto.builder()
                        .postId(post.getPostId())
                        .title(post.getTitle())
                        .views(post.getViews())
                        .heartCount(post.getHeartCount())
                        .email(post.getUser().getEmail())
                        .userId(post.getUser().getUserId())
                        .createdAt(post.getCreatedAt())
                        .build())
                .toList();

        Map<Long, Long> commmentCountMap = commentRepository.countByPostIds(postIdList).stream() // 댓글 수 in쿼리로 한번에 조회 후 map
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
        postDtoList.forEach(postDto ->
                postDto.setCommentCount(commmentCountMap.getOrDefault(postDto.getPostId(), 0L)));



        return ResponseEntity.ok(new PageImpl<>(postDtoList, pageable, total));
    }
}
