package com.example.escommunity.repository;

import com.example.escommunity.entity.Comment;
import com.example.escommunity.entity.CommentReaction;
import com.example.escommunity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentReactionRepository extends JpaRepository<CommentReaction, Long> {
    Optional<CommentReaction> findByCommentAndUser(Comment comment, User user);

    long countByComment(Comment comment);
}
