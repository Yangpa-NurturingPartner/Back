package com.example.demo.repository.member;

import com.example.demo.model.member.MemberUserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class MemberUserDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 이메일로 user_no 조회
    public MemberUserVO findByEmail(String email) {
        String sql = "SELECT user_no, user_email FROM member_user WHERE user_email = ?";
        return jdbcTemplate.queryForObject(sql, new Object[]{email}, new MemberUserRowMapper());
    }

    // ResultSet을 MemberUserVO로 매핑
    private static class MemberUserRowMapper implements RowMapper<MemberUserVO> {
        @Override
        public MemberUserVO mapRow(ResultSet rs, int rowNum) throws SQLException {
            MemberUserVO memberUser = new MemberUserVO();
            memberUser.setUserNo(rs.getLong("user_no"));
            memberUser.setUserEmail(rs.getString("user_email"));
            return memberUser;
        }
    }
}