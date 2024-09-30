package com.example.demo.controller;

import com.example.demo.model.chat.ChatDetailVO;
import com.example.demo.model.chat.ChatVO;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.chat.ChatMapper;
import com.example.demo.service.chat.ChatService;
import com.example.demo.service.member.MemberUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final ChatMapper chatMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberUserService memberUserService;

    private static final String SESSION_ENDED_MSG = "세션이 종료되었습니다.";
    private static final String CHAT_END_ERROR_MSG = "채팅 종료 중 오류 발생";
    private static final String NEW_CHAT_ERROR_MSG = "새로운 채팅 세션 생성 오류: ";
    private static final String SESSION_CREATION_ERROR_MSG = "세션을 생성 불가";

    //채팅 생성
    @PostMapping("/message")
    public ResponseEntity<ChatDetailVO> yangpaChat(@RequestBody Map<String, Object> requestBody) {
        try {
            String sessionId = (String) requestBody.get("session_id");
            String query = (String) requestBody.get("chat_detail");
            String userNo = (String) requestBody.get("token");
            Long user = extractUserNoFromToken(userNo);

            // chat 테이블에 해당 session_id가 존재하는지 확인
            String existingSummAnswer = chatMapper.getFirstAnswer(sessionId);

            if (existingSummAnswer == null) {
                String summary = chatService.getSummary(query);

                chatMapper.saveChat(sessionId, summary, Timestamp.valueOf(LocalDateTime.now()));
            }

            // 세션 종료 여부 확인
            if (chatService.isSessionEnded(sessionId)) {
                return ResponseEntity.badRequest().body(null);
            }

            ChatDetailVO chatDetailVO = createChatDetailVO(sessionId, query);

            List<ChatDetailVO> history = chatMapper.getChatHistoryBySessionId(sessionId);
            String answer = chatService.getAnswer(sessionId, chatDetailVO.getQuery(), history);

            chatDetailVO.setAnswer(answer);
            chatMapper.saveChatDetail(chatDetailVO);

            // 새로운 POST 요청을 외부 URL에 보내는 로직 추가
            String embeddedUrl = "http://192.168.0.218:9000/embedded/chat/contents";
            Map<String, Object> externalRequestBody = Map.of(
                    "user_no", user,
                    "session_id", sessionId,
                    "query", query,
                    "answer", answer
            );

            // HTTP POST 요청 보내기
            ResponseEntity<String> response = sendPostRequest(embeddedUrl, externalRequestBody);

            // 요청 성공 여부에 따른 처리
            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok(chatDetailVO);
            } else {
                log.error("외부 서비스 요청 실패: {}", response.getBody());
                return ResponseEntity.status(500).body(createErrorChatDetailVO(new Exception("외부 서비스 요청 실패: " + response.getBody())));
            }

        } catch (Exception e) {
            log.error("Error in yangpaChat: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(createErrorChatDetailVO(e));
        }
    }

    //채팅 종료
    @PostMapping("/end-chat")
    public ResponseEntity<String> endChatSession(@RequestParam String sessionId) {
        try {
            List<ChatDetailVO> history = chatMapper.getChatHistoryBySessionId(sessionId);
            chatService.endChatSession(sessionId, history);
            return ResponseEntity.ok(SESSION_ENDED_MSG);
        } catch (Exception e) {
            log.error("채팅 종료 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(CHAT_END_ERROR_MSG);
        }
    }

    // 새로운 채팅
    @PostMapping("/start-new-chat")
    public ResponseEntity<Map<String, String>> startNewChat(@RequestBody Map<String, Object> requestBody) {
        try {
            String oldSessionId = (String) requestBody.get("oldSession_id");
            String jwtToken = (String) requestBody.get("jwtToken");
            Integer childId = (Integer) requestBody.get("child_id");

            Long userNo = extractUserNoFromToken(jwtToken);

            List<ChatDetailVO> history = (oldSessionId != null) ?
                    chatMapper.getChatHistoryBySessionId(oldSessionId) : null;

            // 새로운 채팅 세션 생성
            String newSessionId = chatService.createNewSession(history, oldSessionId);

            // 추출한 userNo와 제공된 childId로 새로운 채팅방 저장
            chatMapper.saveChatRoom(newSessionId, Math.toIntExact(userNo), childId);

            // 새로 생성된 세션 ID 반환
            return ResponseEntity.ok(Map.of("session_id", newSessionId));

        } catch (Exception e) {
            log.error("{} {}", NEW_CHAT_ERROR_MSG, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", SESSION_CREATION_ERROR_MSG));
        }
    }

    //해당 user가 했던 session_id가져와 이전 채팅 불러오기
    @PostMapping("/user-chat-record")
    public ResponseEntity<List<ChatVO>> getUserChatSummaries(@RequestBody Map<String, String> userMap) {
        try {
            Long userNo = extractUserNoFromToken(userMap.get("token"));
            log.info("Received user_no: {}", userNo);

            // 사용자와 관련된 모든 세션 ID를 가져옴
            List<String> sessionIds = chatMapper.getSessionIdsByUserId(Math.toIntExact(userNo));

            if (sessionIds.isEmpty()) {
                log.info("No session IDs found for user: {}", userNo);
                return ResponseEntity.noContent().build();
            }
            List<ChatVO> chatSummaries = chatMapper.getSummBySessionIds(sessionIds);

            if (chatSummaries.isEmpty()) {
                log.info("No chat summaries found for session IDs: {}", sessionIds);
                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.ok(chatSummaries);
        } catch (Exception e) {
            log.error("Error fetching chat summaries for user {}: {}", userMap.get("user_no"), e.getMessage(), e);
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
            log.error("Error fetching chat details for session {}: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.status(500).body(null);
        }
    }

    private ChatDetailVO createChatDetailVO(String sessionId, String query) {
        ChatDetailVO chatDetailVO = new ChatDetailVO();
        chatDetailVO.setQuery(query);
        chatDetailVO.setSession_id(sessionId);
        chatDetailVO.setQa_time(Timestamp.valueOf(LocalDateTime.now()));
        return chatDetailVO;
    }

    private ChatDetailVO createErrorChatDetailVO(Exception e) {
        ChatDetailVO chatDetailVO = new ChatDetailVO();
        chatDetailVO.setAnswer("An error occurred: " + e.getMessage());
        return chatDetailVO;
    }

    private Long extractUserNoFromToken(String token) {
        String jwtToken = token.replace("Bearer ", "");
        Map<String, Object> userInfo = jwtTokenProvider.decodeToken(jwtToken);
        log.info("Decoded user info: {}", userInfo);
        return memberUserService.getUserNoByEmail(userInfo.get("email").toString());
    }

    // 외부 URL로 POST 요청을 보내는 유틸리티 메소드
    private ResponseEntity<String> sendPostRequest(String url, Map<String, Object> requestBody) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        return restTemplate.postForEntity(url, requestEntity, String.class);
    }
}