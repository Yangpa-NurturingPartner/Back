package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "community_file")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommunityFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_no")
    private Long fileNo;

    @ManyToOne
    @JoinColumn(name = "board_no", nullable = false)
    private CommunityBoard communityBoard;

    @Column(name = "name", length = 250)
    private String name;

    @Column(name = "attached_img")
    private byte[] attachedImg;

    @Column(name = "post_date")
    private LocalDate postDate;
}