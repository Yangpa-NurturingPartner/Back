package com.example.demo.model.community;

import lombok.Data;

@Data
public class CommunityFileVO {
    private Integer file_no;
    private Integer board_no;
    private String name;
    private byte[] attached_img;
}