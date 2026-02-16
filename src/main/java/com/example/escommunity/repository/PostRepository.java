package com.example.escommunity.repository;

import com.example.escommunity.dto.PostListDto;
import com.example.escommunity.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    @Query("""
        select new com.example.escommunity.dto.PostListDto(
            p.postId,
            p.createdAt,
            p.title,
            p.views,
            p.heartCount,
            u.userId,
            u.email,
            null
        )
        from Post p
        join p.user u
    """)
    Page<PostListDto> findPostList(Pageable pageable);

    @Query("""
        select p
        from Post p
        join fetch p.user u
        where p.postId in :postIdList
    """)
    List<Post> findAllByIdWithUser(List<Long> postIdList);
}
