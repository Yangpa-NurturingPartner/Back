package com.example.demo.model.member;

import java.io.Serializable;

import lombok.Data;

@Data
public class MemberUserVO implements Serializable {
    private Long userNo;
    private String userEmail;
}