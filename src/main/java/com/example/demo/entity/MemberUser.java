package com.example.demo.entity;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "member_user")
public class MemberUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_no")
    private Long userNo;

    @Column(name = "user_email", nullable = false, unique = true)
    private String userEmail;

    // MemberProfile과의 일대다 관계 설정
    @OneToMany(mappedBy = "memberUser", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<MemberProfile> profiles;

    // 기본 생성자
    public MemberUser() {}

    // 이메일을 받는 생성자
    public MemberUser(String userEmail) {
        this.userEmail = userEmail;
    }

    // Getter 및 Setter
    public Long getUserNo() {
        return userNo;
    }

    public void setUserNo(Long userNo) {
        this.userNo = userNo;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public List<MemberProfile> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<MemberProfile> profiles) {
        this.profiles = profiles;
    }

    // equals 메서드
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MemberUser)) return false;
        MemberUser that = (MemberUser) o;
        return getUserNo() != null && getUserNo().equals(that.getUserNo());
    }

    // hashCode 메서드
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}