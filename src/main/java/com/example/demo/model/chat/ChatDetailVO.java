package com.example.demo.model.chat;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class ChatDetailVO {

    private Integer qa_id;
    private String query;
    private String answer;
    private String session_id;
    private Timestamp qa_time;

    public ChatDetailVO() {
    }
}
