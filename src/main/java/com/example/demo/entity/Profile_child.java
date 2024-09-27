package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "profile_child")
public class Profile_child {

    @Id
    @Column(name = "child_id")
    private Integer childId;  // @GeneratedValue 제거하여 수동 설정으로 변경

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_no", nullable = false)
    @JsonIgnore
    private MemberUser memberUser;

    @Column(name = "name")
    private String name;

    @Column(name = "sex")
    private Short sex;

    @Column(name = "birthdate")
    private LocalDate birthdate;

    @Column(name = "join_date")
    private LocalDate joinDate;

    @Column(name = "image_profile")
    private byte[] imageProfile;

    // Getter 및 Setter
    public Integer getChildId() {
        return childId;
    }

    public void setChildId(Integer childId) {
        this.childId = childId;
    }

    public MemberUser getMemberUser() {
        return memberUser;
    }

    public void setMemberUser(MemberUser memberUser) {
        this.memberUser = memberUser;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Short getSex() {
        return sex;
    }

    public void setSex(Short sex) {
        this.sex = sex;
    }

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(LocalDate birthdate) {
        this.birthdate = birthdate;
    }

    public LocalDate getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDate joinDate) {
        this.joinDate = joinDate;
    }

    public byte[] getImageProfile() {
        return imageProfile;
    }

    public void setImageProfile(byte[] imageProfile) {
        this.imageProfile = imageProfile;
    }
}
