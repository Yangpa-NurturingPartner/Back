package com.example.demo.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "member_profile")
public class MemberProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "profile_id")
    private Long profileId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_no", unique = true, nullable = false)
    private MemberUser memberUser;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "join_date")
    private LocalDate joinDate;

    public MemberProfile() {}

    public MemberProfile(MemberUser memberUser, String userName) {
        this.memberUser = memberUser;
        this.userName = userName;
        this.joinDate = LocalDate.now();
    }

    // Getter 및 Setter 메서드
    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public MemberUser getMemberUser() {
        return memberUser;
    }

    public void setMemberUser(MemberUser memberUser) {
        this.memberUser = memberUser;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDate joinDate) {
        this.joinDate = joinDate;
    }
}