package com.example.demo.service;

import com.example.demo.model.ChatDetailVO;
import com.example.demo.model.ChatVO;
import org.apache.ibatis.annotations.*;
import java.sql.Timestamp;

import java.util.List;

@Mapper
public interface ChatMapper {

    //채팅 내용 상세저장
    @Insert("INSERT INTO chat_detail (query, answer, qa_time, session_id) VALUES (#{query}, #{answer}, #{qa_time}, #{session_id}) RETURNING qa_id")
    @Options(useGeneratedKeys = true, keyProperty = "qa_id", keyColumn = "qa_id")
    Integer saveChatDetail(ChatDetailVO chatDetailVO);

    //요약내용 저장
    @Insert("INSERT INTO chat (session_id, summ_answer, end_time) VALUES (#{session_id}, #{summ_answer}, #{end_time}) RETURNING chat_id")
    Integer saveChat(String session_id, String summ_answer, Timestamp end_time);

    //세션아이디 저장
    @Insert("INSERT INTO chat_room (session_id) VALUES (#{session_id})")
    void saveChatRoom(String session_id);

    //첫번째 answer가져오기(요약 시 사용)
    @Select("SELECT answer FROM chat_detail WHERE session_id = #{session_id} ORDER BY qa_time ASC LIMIT 1")
    String getFirstAnswer(@Param("session_id") String session_id);

    @Select("SELECT * FROM chat_detail WHERE session_id = #{session_id} ORDER BY qa_time")
    List<ChatDetailVO> getChatHistoryBySessionId(@Param("session_id") String session_id);

    @Select({
            "<script>",
            "SELECT session_id, summ_answer, end_time  FROM chat  WHERE session_id IN ",
            "<foreach item='sessionId' collection='sessionIds' open='(' separator=',' close=')'>",
            "#{sessionId}</foreach> ",
            "ORDER BY end_time DESC",
            "</script>"
    })
    List<ChatVO> getSummBySessionIds(@Param("sessionIds") List<String> sessionIds);

    //user_no 가지고 있는 채팅방 목록 불러오기
    @Select("SELECT session_id FROM chat_room WHERE user_no = #{user_no}")
    List<String> getSessionIdsByUserId(Integer user_no);

    //과거 채팅 불러오기
    @Select("SELECT query, answer, qa_time FROM chat_detail WHERE session_id = #{sessionId} ORDER BY qa_time ASC")
    List<ChatDetailVO> getChatDetailsBySessionId(String sessionId);
}
