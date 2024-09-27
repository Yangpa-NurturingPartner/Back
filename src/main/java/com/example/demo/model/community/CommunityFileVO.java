package com.example.demo.model.community;

import lombok.Data;

import java.sql.Date;

@Data
public class CommunityFileVO {
    private Long file_no;
    private Long board_no;
    private String name;
    private byte[] attached_img;
    private Date post_date;
}