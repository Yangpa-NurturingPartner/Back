package com.example.demo.service.chat;

import com.example.demo.model.chat.ChatDetailVO;
import com.example.demo.model.chat.ChatVO;
import org.apache.ibatis.annotations.*;

import java.sql.Timestamp;
import java.util.List;

@Mapper
public interface ChatMapper {

    // 채팅 내용 상세 저장
    @Insert("INSERT INTO chat_detail (query, answer, qa_time, session_id) " +
            "VALUES (#{query}, #{answer}, #{qa_time}, #{session_id}) RETURNING qa_id")
    @Options(useGeneratedKeys = true, keyProperty = "qa_id", keyColumn = "qa_id")
    Integer saveChatDetail(ChatDetailVO chatDetailVO);

    // 요약 내용 저장
    @Insert("INSERT INTO chat (session_id, summ_answer, end_time) " +
            "VALUES (#{session_id}, #{summ_answer}, #{end_time}) RETURNING chat_id")
    Integer saveChat(String session_id, String summ_answer, Timestamp end_time);

    // 채팅방 저장
    @Insert("INSERT INTO chat_room (session_id, user_no, child_id) " +
            "VALUES (#{session_id}, #{user_no}, #{child_id})")
    void saveChatRoom(String session_id, int user_no, int child_id);

    // 첫 번째 answer 가져오기 (요약 시 사용)
    @Select("SELECT answer FROM chat_detail " +
            "WHERE session_id = #{session_id} " +
            "ORDER BY qa_time ASC LIMIT 1")
    String getFirstAnswer(String session_id);

    // 채팅 내역 가져오기
    @Select("SELECT * FROM chat_detail " +
            "WHERE session_id = #{session_id} " +
            "ORDER BY qa_time")
    List<ChatDetailVO> getChatHistoryBySessionId(String session_id);

    // 여러 session_id로 요약 가져오기
    @Select({
            "<script>",
            "SELECT * FROM chat WHERE session_id IN ",
            "<foreach item='sessionId' collection='sessionIds' open='(' separator=',' close=')'>",
            "#{sessionId}",
            "</foreach>",
            "ORDER BY end_time DESC",
            "</script>"
    })
    List<ChatVO> getSummBySessionIds(@Param("sessionIds") List<String> sessionIds);

    @Select({
            "<script>",
            "SELECT * FROM chat WHERE session_id IN ",
            "<foreach item='sessionId' collection='sessionIds' open='(' separator=',' close=')'>",
            "#{sessionId}",
            "</foreach>",
            "</script>"
    })
    List<ChatVO> getBySessionIds(@Param("sessionIds") List<String> sessionIds);

    // 특정 user_no가 가지고 있는 채팅방 목록 불러오기
    @Select("SELECT session_id FROM chat_room WHERE user_no = #{user_no}")
    List<String> getSessionIdsByUserId(Integer user_no);

    // 과거 채팅 내용 불러오기
    @Select("SELECT * FROM chat_detail " +
            "WHERE session_id = #{sessionId} " +
            "ORDER BY qa_time ASC")
    List<ChatDetailVO> getChatDetailsBySessionId(String sessionId);

    // 특정 session_id의 채팅 수 세기
    @Select("SELECT COUNT(*) FROM chat_detail WHERE session_id = #{session_id}")
    int countBySessionId(String session_id);
}