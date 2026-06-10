package com.example.DTO;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PostDto {
    
    private Long postId;

    @NotBlank(message = "제목이 누락되었습니다.")
    private String title;

    @NotBlank(message = "작성자가 누락되었습니다.")
    private String name;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

}
