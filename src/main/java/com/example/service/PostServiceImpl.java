package com.example.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.DTO.PostDto;
import com.example.DTO.PostResponseDto;
import com.example.entity.Post;
import com.example.repository.PostRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService{
    
    private final PostRepository postRepository;
    
    @Override
    @Transactional
    public Long newPost(PostDto dto) {
        Post post = new Post(dto.getPostId() ,dto.getTitle(),dto.getName(),dto.getContent());
        return postRepository.save(post).getPostId();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostResponseDto> postList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("postId").descending());
        return postRepository.findAll(pageable) //stream()이 없어도 됨. Pageable은 이미 map을 가지고 있어서 map() 한 방 (Page in, Page out)
                            .map(PostResponseDto::new);
    }

    @Override
    @Transactional
    public PostResponseDto postOne(Long postId) {
        Post post=postRepository.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException( postId+"번 게시글을 찾지 못했습니다."));
        return new PostResponseDto(post);
    }

    @Override
    @Transactional
    public void postUpdate(PostDto dto, Long postId) {
        Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new IllegalArgumentException("삭제할 게시글이 없습니다."+postId));
        post.update(dto);
    }

    @Override
    @Transactional
    public void postDelete(Long postId) {
        postRepository.deleteById(postId);
    }
    
    @Transactional(readOnly = true)
    public List<PostResponseDto> postSearch(String keyword) {
       return postRepository.findByTitleContaining(keyword).stream()
                            .map(PostResponseDto::new)
                            .collect(Collectors.toList());
    }
}
