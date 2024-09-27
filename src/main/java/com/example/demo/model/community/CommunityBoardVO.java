package com.example.demo.model.community;

import lombok.Data;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Date;

@Data
public class CommunityBoardVO {
    private Integer board_no;
    private Integer user_no;
    private Integer child_id;
    private Integer board_code;
    private String title;
    private String board_contents;
    private Integer count;
    private Date register_date;
    private Date update_date;

    private MultipartFile file;

    private String file_name;
    private byte[] attached_img;
    private Date post_date;

    private String imageUrl;

    public String getImageUrl() {
        if (attached_img != null && attached_img.length > 0) {
            return "data:image/png;base64," + Base64.encodeBase64String(attached_img);
        }
        return null;
    }
}