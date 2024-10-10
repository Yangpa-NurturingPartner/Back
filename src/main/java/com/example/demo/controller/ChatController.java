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
import java.util.stream.Collectors;

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

    // 채팅 생성
    @PostMapping("/message")
    public ResponseEntity<Map<String, Object>> yangpaChat(@RequestBody Map<String, Object> requestBody) {
        try {
            String sessionId = (String) requestBody.get("session_id");
            String query = (String) requestBody.get("chat_detail");
            String userNo = (String) requestBody.get("token");
            Long user = extractUserNoFromToken(userNo);

            String existingSummAnswer = chatMapper.getFirstAnswer(sessionId);

            if (existingSummAnswer == null) {
                String summary = chatService.getSummary(query);
                chatMapper.saveChat(sessionId, summary, Timestamp.valueOf(LocalDateTime.now()));
            }

            if (chatService.isSessionEnded(sessionId)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "fail",
                        "data", null,
                        "message", "세션이 종료되었습니다."
                ));
            }

            // 기존의 chatDetailVO 생성
            ChatDetailVO chatDetailVO = createChatDetailVO(sessionId, query);

            // 기존 대화 이력 가져오기
            List<ChatDetailVO> history = chatMapper.getChatHistoryBySessionId(sessionId);

            // 대화 이력이 없는 경우에만 외부 API 호출
            if (history == null || history.isEmpty()) {
                // 외부 API 호출을 위한 데이터 구성
                String embeddedUrl = "http://221.148.97.238:9400/embedded/chat/contents";
                Map<String, Object> externalEmbeddedRequestBody = Map.of(
                        "user_no", user,
                        "session_id", sessionId,
                        "query", query,
                        "answer", "답변이 없습니다."
                );

                // 외부 서비스에 전송
                ResponseEntity<String> embeddedResponse = sendPostRequest(embeddedUrl, externalEmbeddedRequestBody);

                // 응답 처리
                if (!embeddedResponse.getStatusCode().is2xxSuccessful()) {
                    log.error("외부 서비스 요청 실패: {}", embeddedResponse.getBody());
                    return ResponseEntity.status(500).body(Map.of(
                            "status", "error",
                            "message", "외부 서비스 요청 실패: " + embeddedResponse.getBody(),
                            "data", null
                    ));
                }
            }

            // history 데이터를 새로운 API에 맞게 가공
            List<Map<String, String>> messages = history.stream()
                    .flatMap(chat -> List.of(
                            Map.of("role", "user", "content", chat.getQuery()),
                            Map.of("role", "assistant", "content", chat.getAnswer() != null ? chat.getAnswer() : "답변이 없습니다.")
                    ).stream())
                    .collect(Collectors.toList());

            messages.add(Map.of("role", "user", "content", query));

            String searchUrl = "http://221.148.97.238:9400/search/chat";
            Map<String, Object> externalRequestBody = Map.of("messages", messages);
            ResponseEntity<Map> response = sendPostRequestForSearch(searchUrl, externalRequestBody);
            Map<String, Object> responseBody = response.getBody();

            // 외부 API 응답 상태 확인
            if (!response.getStatusCode().is2xxSuccessful() || !responseBody.containsKey("result")) {
                log.error("외부 서비스 요청 실패: {}", response.getBody());
                return ResponseEntity.status(500).body(Map.of(
                        "status", "error",
                        "message", "외부 서비스 요청 실패: " + response.getBody(),
                        "data", null
                ));
            }

            // 응답에서 'assistant'의 content 추출
            List<Map<String, String>> result = (List<Map<String, String>>) responseBody.get("result");
            String answer = result.stream()
                    .filter(item -> "assistant".equals(item.get("role")))
                    .map(item -> item.get("content"))
                    .findFirst()
                    .orElse("답변이 없습니다.");

            // 답변을 chatDetailVO에 설정
            chatDetailVO.setAnswer(answer);

            // 채팅 상세 저장
            chatMapper.saveChatDetail(chatDetailVO);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", chatDetailVO,
                    "message", "요청이 성공적으로 처리되었습니다."
            ));

        } catch (Exception e) {
            log.error("Error in yangpaChat: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "예기치 못한 오류 발생: " + e.getMessage(),
                    "data", null
            ));
        }
    }



    // 채팅 종료
    @PostMapping("/end-chat")
    public ResponseEntity<Map<String, Object>> endChatSession(@RequestParam String sessionId) {
        try {
            List<ChatDetailVO> history = chatMapper.getChatHistoryBySessionId(sessionId);
            chatService.endChatSession(sessionId, history);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", null,
                    "message", SESSION_ENDED_MSG
            ));
        } catch (Exception e) {
            log.error("채팅 종료 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", CHAT_END_ERROR_MSG,
                    "data", null
            ));
        }
    }

    // 새로운 채팅
    @PostMapping("/start-new-chat")
    public ResponseEntity<Map<String, Object>> startNewChat(@RequestBody Map<String, Object> requestBody) {
        try {
            String oldSessionId = (String) requestBody.get("oldSession_id");
            String jwtToken = (String) requestBody.get("jwtToken");
            Integer childId = (Integer) requestBody.get("child_id");

            Long userNo = extractUserNoFromToken(jwtToken);
            List<ChatDetailVO> history = (oldSessionId != null) ? chatMapper.getChatHistoryBySessionId(oldSessionId) : null;

            String newSessionId = chatService.createNewSession(history, oldSessionId);
            chatMapper.saveChatRoom(newSessionId, Math.toIntExact(userNo), childId);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", Map.of("session_id", newSessionId),
                    "message", "새로운 채팅 세션이 성공적으로 생성되었습니다."
            ));
        } catch (Exception e) {
            log.error("{} {}", NEW_CHAT_ERROR_MSG, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", SESSION_CREATION_ERROR_MSG,
                    "data", null
            ));
        }
    }


    // 해당 user가 했던 session_id 가져와 이전 채팅 불러오기
    @PostMapping("/user-chat-record")
    public ResponseEntity<Map<String, Object>> getUserChatSummaries(@RequestBody Map<String, String> userMap) {
        try {
            Long userNo = extractUserNoFromToken(userMap.get("token"));
            log.info("Received user_no: {}", userNo);

            List<String> sessionIds = chatMapper.getSessionIdsByUserId(Math.toIntExact(userNo));
            if (sessionIds.isEmpty()) {
                log.info("No session IDs found for user: {}", userNo);
                return ResponseEntity.ok(Map.of(
                        "status", "fail",
                        "data", null,
                        "message", "해당 사용자는 세션 ID가 없습니다."
                ));
            }

            List<ChatVO> chatSummaries = chatMapper.getSummBySessionIds(sessionIds);
            if (chatSummaries.isEmpty()) {
                log.info("No chat summaries found for session IDs: {}", sessionIds);
                return ResponseEntity.ok(Map.of(
                        "status", "fail",
                        "data", null,
                        "message", "세션 ID에 해당하는 채팅 요약이 없습니다."
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", chatSummaries,
                    "message", "채팅 요약이 성공적으로 반환되었습니다."
            ));
        } catch (Exception e) {
            log.error("Error fetching chat summaries for user {}: {}", userMap.get("user_no"), e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "사용자 채팅 요약을 가져오는 중 오류 발생",
                    "data", null
            ));
        }
    }

    // 과거 채팅 상세보기
    @GetMapping("/chat-record-view/{sessionId}")
    public ResponseEntity<Map<String, Object>> getChatDetails(@PathVariable String sessionId) {
        try {
            List<ChatDetailVO> chatDetails = chatService.getChatDetailsBySessionId(sessionId);

            if (chatDetails.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "status", "fail",
                        "data", null,
                        "message", "해당 세션에 대한 채팅 상세 정보가 없습니다."
                ));
            }
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", chatDetails,
                    "message", "채팅 상세 정보가 성공적으로 반환되었습니다."
            ));
        } catch (Exception e) {
            log.error("Error fetching chat details for session {}: {}", sessionId, e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "채팅 상세 정보를 가져오는 중 오류 발생",
                    "data", null
            ));
        }
    }

    // 요약된 채팅 제목 검색
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> searchChatHistory(@RequestBody Map<String, String> requestBody) {
        try {
            String query = requestBody.get("query");
            String jwtToken = requestBody.get("token");

            Long userNo = extractUserNoFromToken(jwtToken);
            Map<String, Object> externalRequestBody = Map.of(
                    "query", query,
                    "user_no", userNo
            );
            String searchUrl = "http://192.168.0.218:9000/search/chat-history";

            ResponseEntity<Map> response = sendPostRequestForSearch(searchUrl, externalRequestBody);
            log.info(response.getBody().toString());

            if (!response.getStatusCode().is2xxSuccessful() || !response.hasBody()) {
                log.error("Failed to search chat history: {}", response.getBody());
                return ResponseEntity.status(500).body(Map.of(
                        "status", "error",
                        "message", "채팅 내역 검색 실패: " + response.getBody(),
                        "data", null
                ));
            }

            Map<String, Object> responseBody = response.getBody();
            List<String> sessionIds = (List<String>) responseBody.get("session_ids");

            if (sessionIds == null || sessionIds.isEmpty()) {
                log.info("No session IDs found for user: {}", userNo);
                return ResponseEntity.ok(Map.of(
                        "status", "fail",
                        "data", null,
                        "message", "해당 사용자에 대한 세션 ID가 없습니다."
                ));
            }

            List<ChatVO> chatSummaries = chatMapper.getSummBySessionIds(sessionIds);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", chatSummaries,
                    "message", "채팅 내역이 성공적으로 검색되었습니다."
            ));

        } catch (Exception e) {
            log.error("Error in searchChatHistory: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "status", "error",
                    "message", "채팅 내역 검색 중 오류 발생",
                    "data", null
            ));
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

    private ResponseEntity<Map> sendPostRequestForSearch(String url, Map<String, Object> requestBody) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        return restTemplate.postForEntity(url, requestEntity, Map.class);
    }
}