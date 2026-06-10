package com.example.DTO;

import com.example.entity.Post;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostResponseDto {

    private Long postId;
    private String title;
    private String name;
    private String content;

    public PostResponseDto(Post post) {
        this.postId=post.getPostId();
        this.title=post.getTitle();
        this.name=post.getName();
        this.content=post.getContent();
    }
}
