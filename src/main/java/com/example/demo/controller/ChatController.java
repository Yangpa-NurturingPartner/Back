package com.example.demo.controller;

import com.example.demo.model.ChatDetailVO;
import com.example.demo.model.ChatVO;
import com.example.demo.service.ChatMapper;
import com.example.demo.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatMapper chatMapper;

    @PostMapping("/message")
    public ResponseEntity<ChatDetailVO> yangpaChat(@RequestBody Map<String, Object> requestBody) {
        ChatDetailVO chatDetailVO = new ChatDetailVO();
        try {
            String session_id = (String) requestBody.get("session_id");
            String query = (String) requestBody.get("chat_detail");
            System.out.println("Received session_id: " + session_id);
            System.out.println("Request Body: " + requestBody);

            //세션이 종료된 경우 메시지 전송 불가
            if (chatService.isSessionEnded(session_id)) {
                return ResponseEntity.badRequest().body(null);
            }

            chatDetailVO.setQuery(query);
            chatDetailVO.setSession_id(session_id);

            Timestamp qa_time = Timestamp.valueOf(LocalDateTime.now());
            chatDetailVO.setQa_time(qa_time);

            //대화 기록 조회 후 응답 생성
            List<ChatDetailVO> history = chatMapper.getChatHistoryBySessionId(session_id);
            String answer = chatService.getAnswer(session_id, chatDetailVO.getQuery(), history);

            chatDetailVO.setAnswer(answer);
            chatMapper.saveChatDetail(chatDetailVO);

            return ResponseEntity.ok(chatDetailVO);
        } catch (Exception e) {
            log.error("Error in GPT Controller: {}", e.getMessage(), e);
            chatDetailVO.setAnswer("An error occurred: " + e.getMessage());
            return ResponseEntity.status(500).body(chatDetailVO);
        }
    }

    //채팅 종료
    @PostMapping("/end-chat")
    public ResponseEntity<String> endChatSession(@RequestParam String session_id) {
        try {
            List<ChatDetailVO> history = chatMapper.getChatHistoryBySessionId(session_id);
            chatService.endChatSession(session_id, history); // 서비스에서 세션 종료 처리
            return ResponseEntity.ok("채팅 세션이 종료되었습니다.");
        } catch (Exception e) {
            log.error("채팅 종료 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("채팅 종료 중 오류 발생");
        }
    }

    // 새로운 채팅
    @PostMapping("/start-new-chat")
    public ResponseEntity<Map<String, String>> startNewChat(@RequestParam(required = false) String oldSession_id) {
        log.info("startNewChat 호출됨. oldSession_id: {}", oldSession_id);
        try {
            // 기존 세션 처리
            List<ChatDetailVO> history = null;
            if (oldSession_id != null) {
                history = chatMapper.getChatHistoryBySessionId(oldSession_id);
            }


            // 새로운 세션 생성
            String newSession_id = chatService.createNewSession(history, oldSession_id);
            int user_no = 1;
            int child_id = 1;
            chatMapper.saveChatRoom(newSession_id, user_no, child_id);

            log.info("새로운 세션아이디: {}", newSession_id); // 로그 확인
            return ResponseEntity.ok(Map.of("session_id", newSession_id)); // session_id 반환
        } catch (Exception e) {
            log.error("새로운 채팅 세션 생성 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "세션 생성 불가"));
        }
    }


    //이전 채팅 불러오기
    @PostMapping("/chat-record")
    public List<ChatVO> getChatSummaries(@RequestBody List<String> sessionIds) {
        return chatMapper.getSummBySessionIds(sessionIds);
    }

    //해당 user가 했던 session_id가져오기
    @PostMapping("/get-userinfo")
    public ResponseEntity<List<String>> getUserSessionIds(@RequestBody Map<String, String> userMap) {
        try {
            int user_no = Integer.parseInt(userMap.get("user_no"));
            log.info("Received user_no: {}", user_no);

            //user_no 해당하는 session_id 가져오기
            List<String> sessionIds = chatMapper.getSessionIdsByUserId(user_no);

            if (sessionIds.isEmpty()) {
                log.info("No session IDs found for user: {}", user_no);
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(sessionIds);
        } catch (Exception e) {
            log.error("Error fetching session IDs for user {}: {}", userMap.get("user_no"), e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    //과거 채팅 상세보기
    @GetMapping("/chat-record-view/{sessionId}")
    public ResponseEntity<List<ChatDetailVO>> getChatDetails(@PathVariable String sessionId) {
        try {
            List<ChatDetailVO> chatDetails = chatService.getChatDetailsBySessionId(sessionId);

            if (chatDetails.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(chatDetails);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null); // 오류 처리
        }
    }
}
