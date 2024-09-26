package com.example.demo.model.community;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.sql.Date;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
public class CommunityBoardResponseDTO {
    private Integer board_no;
    private Integer user_no;
    private Integer child_id;
    private Integer board_code;
    private String title;
    private String board_contents;
    private Integer count;
    private Date register_date;
    private Date update_date;
}