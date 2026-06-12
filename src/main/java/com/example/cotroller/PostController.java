package com.example.cotroller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.DTO.PostDto;
import com.example.DTO.PostResponseDto;
import com.example.service.PostService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;



@RestController
@RequestMapping("/post")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping("/newpost")
    public Long newPost(@Valid @RequestBody PostDto dto) {
        return postService.newPost(dto);
    }

    @GetMapping("/list")
    public Page<PostResponseDto> postList(@RequestParam(value="page", defaultValue="0") int page, 
                                        @RequestParam(value = "size", defaultValue = "10") int size) {
        return postService.postList(page, size);
    }

    @GetMapping("/{postid}")
    public PostResponseDto postOne(@PathVariable("postid") Long postId){
        return postService.postOne(postId);
    }

    @PutMapping("/{postid}")
    public void postUpdate(@RequestBody PostDto dto, @PathVariable("postid") Long postId) {
        postService.postUpdate(dto, postId);
    }

    @DeleteMapping("/{postid}")
    public void postDelete(@PathVariable("postid") Long postId) {
        postService.postDelete(postId);
    }

    @GetMapping("/search")
    public List<PostResponseDto> postSearch(@RequestParam("keyword") String keyword) {
        return postService.postSearch(keyword);
    }
    
}