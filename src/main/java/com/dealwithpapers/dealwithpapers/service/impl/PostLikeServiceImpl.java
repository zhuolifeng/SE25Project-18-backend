package com.dealwithpapers.dealwithpapers.service.impl;

import com.dealwithpapers.dealwithpapers.entity.Post;
import com.dealwithpapers.dealwithpapers.entity.PostLike;
import com.dealwithpapers.dealwithpapers.entity.User;
import com.dealwithpapers.dealwithpapers.repository.PostLikeRepository;
import com.dealwithpapers.dealwithpapers.repository.PostRepository;
import com.dealwithpapers.dealwithpapers.repository.UserRepository;
import com.dealwithpapers.dealwithpapers.service.PostLikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PostLikeServiceImpl implements PostLikeService {
    @Autowired
    private PostLikeRepository postLikeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PostRepository postRepository;

    @Override
    public void like(Long userId, Long postId) {
        operate(userId, postId, 1);
    }

    @Override
    public void dislike(Long userId, Long postId) {
        operate(userId, postId, -1);
    }

    private void operate(Long userId, Long postId, int type) {
        User user = userRepository.findById(userId).orElseThrow();
        Post post = postRepository.findById(postId).orElseThrow();
        Optional<PostLike> likeOpt = postLikeRepository.findByUserAndPost(user, post);
        if (likeOpt.isPresent()) {
            PostLike like = likeOpt.get();
            if (like.getType() == type) {
                // 已经点过同类型，什么都不做
                return;
            } else {
                like.setType(type);
                like.setCreateTime(LocalDateTime.now());
                postLikeRepository.save(like);
            }
        } else {
            PostLike like = new PostLike();
            like.setUser(user);
            like.setPost(post);
            like.setType(type);
            like.setCreateTime(LocalDateTime.now());
            postLikeRepository.save(like);
        }
    }

    @Override
    public void cancel(Long userId, Long postId) {
        User user = userRepository.findById(userId).orElseThrow();
        Post post = postRepository.findById(postId).orElseThrow();
        postLikeRepository.findByUserAndPost(user, post).ifPresent(postLikeRepository::delete);
    }

    @Override
    public int countLikes(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow();
        return (int) postLikeRepository.countByPostAndType(post, 1);
    }

    @Override
    public int countDislikes(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow();
        return (int) postLikeRepository.countByPostAndType(post, -1);
    }

    @Override
    public Integer getUserLikeType(Long userId, Long postId) {
        User user = userRepository.findById(userId).orElse(null);
        Post post = postRepository.findById(postId).orElse(null);
        if (user == null || post == null) return null;
        return postLikeRepository.findByUserAndPost(user, post).map(PostLike::getType).orElse(null);
    }
} 