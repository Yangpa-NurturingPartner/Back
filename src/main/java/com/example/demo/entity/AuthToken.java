package com.example.demo.entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "auth_token")
public class AuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long tokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_no", nullable = false)
    private MemberUser memberUser;

    @Column(name = "access_token")
    private String accessToken;

    @Column(name = "iat")
    @Temporal(TemporalType.TIMESTAMP)
    private Date iat;

    // 기본 생성자
    public AuthToken() {}

    // 매개변수를 사용하는 생성자
    public AuthToken(MemberUser memberUser, String accessToken, Date iat) {
        this.memberUser = memberUser;
        this.accessToken = accessToken;
        this.iat = iat;
    }

    // Getter 및 Setter
    public Long getTokenId() {
        return tokenId;
    }

    public void setTokenId(Long tokenId) {
        this.tokenId = tokenId;
    }

    public MemberUser getMemberUser() {
        return memberUser;
    }

    public void setMemberUser(MemberUser memberUser) {
        this.memberUser = memberUser;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Date getIat() {
        return iat;
    }

    public void setIat(Date iat) {
        this.iat = iat;
    }
}