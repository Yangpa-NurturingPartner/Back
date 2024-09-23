package com.example.demo.model;

import lombok.Data;
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
    private Date register_time;
    private Date update_time;
}
