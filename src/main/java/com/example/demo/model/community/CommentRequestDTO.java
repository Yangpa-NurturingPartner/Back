package com.example.demo.model.community;

import lombok.Data;

@Data
public class CommentRequestDTO {
    private String token;
    private Long board_no;
    private String comments_contents;
}
