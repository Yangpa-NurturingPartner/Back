package com.example.demo.model.chat;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class ChatVO {

    private Integer chat_id;
    private String session_id;
    private String summ_answer;
    private Timestamp end_time;
}
