package com.dealwithpapers.dealwithpapers.repository;

import com.dealwithpapers.dealwithpapers.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {
    @Query("SELECT p FROM Post p WHERE p.title LIKE %:title%")
    List<Post> findByTitleContaining(@Param("title") String title);

    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN p.author a WHERE " +
           "CAST(p.id AS string) = :searchTerm OR " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(a.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Post> searchByTerm(@Param("searchTerm") String searchTerm);
} 