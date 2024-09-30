package com.example.demo.model.community;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.sql.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class CommunityCommentVO {
    private Long comment_no;
    private Long board_no;
    private Integer parents_id;
    private String comments_name;
    private String comments_contents;
    private Date comments_date;
}