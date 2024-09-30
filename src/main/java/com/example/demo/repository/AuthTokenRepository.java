package com.example.demo.repository;

import com.example.demo.entity.AuthToken;
import com.example.demo.entity.MemberUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    List<AuthToken> findByMemberUser(MemberUser memberUser);

    boolean existsByAccessToken(String token);
}