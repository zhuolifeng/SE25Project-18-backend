package com.dealwithpapers.dealwithpapers.repository;

import com.dealwithpapers.dealwithpapers.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
 
public interface TagRepository extends JpaRepository<Tag, Long> {
    Optional<Tag> findByName(String name);
} 