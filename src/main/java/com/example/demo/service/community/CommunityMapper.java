package com.example.demo.service.community;

import com.example.demo.model.community.CommunityBoardVO;
import com.example.demo.model.community.CommunityFileVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommunityMapper {

    // 게시물 목록 조회
    @Select("SELECT * FROM community_board ORDER BY board_no DESC LIMIT #{size} OFFSET #{skip}")
    List<CommunityBoardVO> getBoardList(int size, int skip);

    // 게시물 총 개수 조회
    @Select("SELECT COUNT(*) FROM community_board")
    int getTotalCount();

    // 게시물 저장 후 자동 생성된 board_no 값 반환
    @Insert("INSERT INTO community_board (user_no, child_id, board_code, title, board_contents, count, register_date, update_date) " +
            "VALUES (#{user_no}, #{child_id}, #{board_code}, #{title}, #{board_contents}, 0, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "board_no", keyColumn = "board_no")
    void insertBoard(CommunityBoardVO boardVO);

    // 파일 저장
    @Insert("INSERT INTO community_file (board_no, name, attached_img, post_date) " +
            "VALUES (#{board_no}, #{name}, #{attached_img}, NOW())")
    void insertFile(CommunityFileVO fileVO);

    // 가장 최근에 삽입된 board_no 가져오기
    @Select("SELECT MAX(board_no) FROM community_board")
    Long getLastInsertedBoardNo();
}