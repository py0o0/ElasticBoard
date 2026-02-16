package com.example.escommunity.repository;

import com.example.escommunity.entity.Comment;
import com.example.escommunity.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("""
        select c.post.postId, count(c.commentId)
        from Comment c
        where c.post.postId in :postIds
        group by c.post.postId
        """)
    List<Object[]> countByPostIds(List<Long> postIds);

    @Query("""
        select c from Comment c
        join fetch c.user
        where c.post = :post
        """)
    List<Comment> findByPostWithUser(Post post);
}
