package com.dealwithpapers.dealwithpapers.repository;

import com.dealwithpapers.dealwithpapers.entity.PostTag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PostTagRepository extends JpaRepository<PostTag, Long> {
    Optional<PostTag> findByName(String name);
} 