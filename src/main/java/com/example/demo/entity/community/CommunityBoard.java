package com.example.demo.entity.community;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "community_board")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommunityBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_no")
    private Long boardNo;

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Column(name = "child_id", nullable = false)
    private Long childId;

    @Column(name = "board_code")
    private Short boardCode;

    @Column(name = "title", length = 50)
    private String title;

    @Column(name = "board_contents", length = 250)
    private String boardContents;

    @Column(name = "count")
    private Short count;

    @Column(name = "register_date")
    private LocalDate registerDate;

    @Column(name = "update_date")
    private LocalDate updateDate;
}