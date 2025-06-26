package com.dealwithpapers.dealwithpapers.repository;

import com.dealwithpapers.dealwithpapers.entity.PostLike;
import com.dealwithpapers.dealwithpapers.entity.Post;
import com.dealwithpapers.dealwithpapers.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByUserAndPost(User user, Post post);
    List<PostLike> findByPost(Post post);
    long countByPostAndType(Post post, Integer type);
} 