package com.example.demo.model.intSㄹearch;

import com.example.demo.model.community.CommunityBoardVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface SearchResultMapper {

    // 비디오 데이터 가져오는 메서드
    @Select("<script>" +
            "SELECT * FROM data_video WHERE video_no IN " +
            "<foreach item='id' collection='videoIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Map<String, Object>> getVideoDataByIds(@Param("videoIds") List<Integer> videoIds);

    // 문서 데이터 가져오는 메서드
    @Select("<script>" +
            "SELECT * FROM data_document WHERE document_no IN " +
            "<foreach item='id' collection='documentIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Map<String, Object>> getDocumentDataByIds(@Param("documentIds") List<Integer> documentIds);

    // 채팅 데이터 가져오는 메서드
    @Select("<script>" +
            "SELECT summ_answer, session_id FROM chat WHERE session_id IN " +
            "<foreach item='id' collection='chatIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<Map<String, Object>> getChatDataByIds(@Param("chatIds") List<Integer> chatIds);

    // 커뮤니티 게시판 데이터 가져오는 메서드
    @Select("<script>" +
            "SELECT * FROM community_board WHERE board_no IN " +
            "<foreach item='id' collection='communityIds' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<CommunityBoardVO> getCommunityBoardsWithFilesByNos(@Param("communityIds") List<Integer> communityIds);
}