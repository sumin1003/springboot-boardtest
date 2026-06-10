package com.example.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.example.DTO.PostDto;
import com.example.DTO.PostResponseDto;

public interface PostService {

    Long newPost(PostDto dto);
    Page<PostResponseDto> postList(int page, int size);
    PostResponseDto postOne(Long postId);
    void postUpdate(PostDto dto, Long postId);
    void postDelete(Long postId);
    List<PostResponseDto> postSearch(String keyword);
}
