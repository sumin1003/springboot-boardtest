package com.example.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entity.Post;

public interface PostRepository extends JpaRepository<Post,Long> {

    List<Post> findByTitleContaining(String keyWord);
    
}
