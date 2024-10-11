package com.example.demo.service.community;

import com.example.demo.model.community.CommunityBoardVO;
import com.example.demo.model.community.CommunityCommentVO;
import com.example.demo.model.community.CommunityFileVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CommunityMapper {

    // 게시물 목록 조회 (파일 정보 포함)
    @Select("<script>" +
            "SELECT b.*, f.name AS file_name, f.attached_img, f.post_date " +
            "FROM community_board b " +
            "LEFT JOIN community_file f ON b.board_no = f.board_no " +
            "<where>" +
            "<if test='boardCode != null and boardCode != 0'>b.board_code = #{boardCode}</if>" +
            "</where>" +
            "ORDER BY b.board_no DESC LIMIT #{size} OFFSET #{skip}" +
            "</script>")
    List<CommunityBoardVO> getBoardListWithFiles(@Param("size") int size, @Param("skip") int skip, @Param("boardCode") Integer boardCode);

    // 게시물 총 개수 조회
    @Select("<script>" +
            "SELECT COUNT(*) FROM community_board " +
            "<where>" +
            "<if test='boardCode != null and boardCode != 0'>board_code = #{boardCode}</if>" +
            "</where>" +
            "</script>")
    int getTotalCount(@Param("boardCode") Integer boardCode);

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

    // board_no로 모든 댓글 조회
    @Select("SELECT * FROM community_comments WHERE board_no = #{board_no}")
    List<CommunityCommentVO> getCommentsByBoardNo(@Param("board_no") Long boardNo);

    // 특정 board_no로 게시물 가져오기
    @Select("SELECT * FROM community_board WHERE board_no = #{board_no}")
    CommunityBoardVO getBoardByNo(@Param("board_no") Long boardNo);

    // board_no로 파일 조회
    @Select("SELECT * FROM community_file WHERE board_no = #{board_no}")
    List<CommunityFileVO> getFilesByBoardNo(@Param("board_no") Long boardNo);

    // 가장 최근에 삽입된 comment_no 가져오기
    @Select("SELECT MAX(comment_no) FROM community_comments")
    Long getLastInsertedCommentNo();

    // 댓글 저장
    @Insert("INSERT INTO community_comments (comment_no, board_no, parents_id, comments_name, comments_contents, comments_date) " +
            "VALUES (#{comment_no}, #{board_no}, #{parents_id}, #{comments_name}, #{comments_contents}, #{comments_date})")
    void insertComment(CommunityCommentVO commentVO);

    // board_no에 해당하는 게시물의 count 값을 +1로 업데이트
    @Update("UPDATE community_board SET count = count + 1 WHERE board_no = #{board_no}")
    void updateBoardCount(@Param("board_no") Long boardNo);

    @Select("<script>" +
            "SELECT b.*, f.file_no, f.name AS file_name, f.attached_img, f.post_date " +
            "FROM community_board b " +
            "LEFT JOIN community_file f ON b.board_no = f.board_no " +
            "WHERE b.board_no IN " +
            "<foreach item='item' index='index' collection='boardNos' open='(' separator=',' close=')'>" +
            "#{item}" +
            "</foreach>" +
            "</script>")
    List<CommunityBoardVO> getBoardsWithFilesByNos(@Param("boardNos") List<Integer> boardNos);
}