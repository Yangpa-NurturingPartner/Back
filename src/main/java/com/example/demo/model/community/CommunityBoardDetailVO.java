package com.example.demo.model.community;


import lombok.Data;

import java.util.List;

@Data
public class CommunityBoardDetailVO {
    // CommunityBoardVO와 CommunityCommentVO를 결합하기 위한 응답 DTO

    private CommunityBoardResponseDTO board;
    private List<CommunityCommentVO> comments;
    private List<CommunityFileVO> files;
}
