package com.example.demo.service.member;

import com.example.demo.model.member.MemberUserVO;
import com.example.demo.repository.member.MemberUserDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MemberUserService {

    @Autowired
    private MemberUserDAO memberUserDAO;

    // 이메일을 통해 user_no 조회하는 메소드
    public Long getUserNoByEmail(String email) {
        MemberUserVO memberUser = memberUserDAO.findByEmail(email);
        return memberUser != null ? memberUser.getUserNo() : null;
    }
}