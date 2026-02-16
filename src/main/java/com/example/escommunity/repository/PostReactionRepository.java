package com.example.escommunity.repository;

import com.example.escommunity.entity.Post;
import com.example.escommunity.entity.PostReaction;
import com.example.escommunity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostReactionRepository extends JpaRepository<PostReaction, Long> {
    Optional<PostReaction> findByPostAndUser(Post post, User user);

    long countByPost(Post post);
}
