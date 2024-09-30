package com.example.demo.service.chat;

import com.example.demo.model.chat.ChatDetailVO;
import com.theokanning.openai.OpenAiService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChatService {

    // 데이터베이스와의 상호 작용을 위한 매퍼
    private final ChatMapper chatMapper;

    // OpenAI API 호출을 위한 서비스
    private final OpenAiService openAiService;

    // 세션 상태를 관리하는 맵 (active/inactive 상태)
    private final Map<String, String> sessionStatusMap = new ConcurrentHashMap<>();

    // 세션의 종료 여부를 관리하는 맵
    private final Map<String, Boolean> sessionMap = new ConcurrentHashMap<>();

    // 의존성 주입을 통한 chatMapper와 OpenAiService 초기화
    @Autowired
    public ChatService(ChatMapper chatMapper, @Value("${spring.ai.openai.api-key}") String openaiApiKey) {
        this.chatMapper = chatMapper;
        this.openAiService = new OpenAiService(openaiApiKey, Duration.ofSeconds(30));
    }

    /**
     * 주어진 세션 ID와 질문을 사용하여 답변을 생성세션이 종료되었으면 종료 처리하고, 그렇지 않으면 대화 기록을 기반으로 응답을 생성
     */
    public String getAnswer(String sessionId, String query, List<ChatDetailVO> history) {
        if (isSessionEnded(sessionId)) {
            endChatSession(sessionId, history);
            return "세션 종료";
        }

        // 대화 이력이 없으면 새로운 응답 생성, 이력이 있으면 기존 대화 기반 응답 생성
        return history == null || history.isEmpty() ? getNewAnswer(query) : getChatResponse(sessionId, query, history);
    }

    // 이전 대화 내역을 기반으로 새로운 질문에 대한 GPT 응답을 생성
    private String getChatResponse(String sessionId, String query, List<ChatDetailVO> history) {
        // 대화 이력을 OpenAI의 메시지 형식으로 변환
        List<ChatMessage> messages = history.stream()
                .map(chat -> List.of(
                        new ChatMessage("user", chat.getQuery()),
                        new ChatMessage("assistant", chat.getAnswer() != null ? chat.getAnswer() : "답변이 없습니다.")
                ))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        // 새 질문 추가
        messages.add(new ChatMessage("user", query));

        // OpenAI API 요청 생성 및 응답 반환
        ChatCompletionRequest request = buildChatRequest(messages);
        return openAiService.createChatCompletion(request)
                .getChoices().get(0).getMessage().getContent().trim();
    }

    // 주어진 메시지 리스트를 사용하여 OpenAI API 요청을 생성
    private ChatCompletionRequest buildChatRequest(List<ChatMessage> messages) {
        return ChatCompletionRequest.builder()
                .model("gpt-4o-mini")
                .messages(messages)
                .maxTokens(1500)
                .temperature(0.7)
                .build();
    }

    // 세션이 종료되었는지 확인
    public boolean isSessionEnded(String sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("세션 아이디가 없습니다");
        }
        return sessionMap.getOrDefault(sessionId, false);
    }

    // 새로운 질문에 대한 간단한 응답을 생성
    private String getNewAnswer(String query) {
        // 질문을 OpenAI 메시지로 변환
        ChatMessage queryMessage = new ChatMessage("user", "다음 육아 질문에 대한 간단하게 답변을 해줘: " + query);

        // OpenAI API 요청을 통해 응답 반환
        return getGptResponse(Collections.singletonList(queryMessage));
    }

    // 주어진 답변을 8~15자 사이의 요약으로 생성
    public String getSummary(String answer) {
        // 요약 요청 메시지 생성
        ChatMessage summaryMessage = new ChatMessage("user", "다음 텍스트의 대화 주제를 8~15글자 사이로 요약해: " + answer);

        // OpenAI API 요청을 통해 요약 생성 및 반환
        return getGptResponse(Collections.singletonList(summaryMessage));
    }

    // 주어진 메시지 리스트를 사용하여 OpenAI API 요청을 생성하고 응답을 반환
    private String getGptResponse(List<ChatMessage> messages) {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4o-mini")
                .messages(messages)
                .maxTokens(300)
                .temperature(0.5)
                .build();

        // OpenAI API로부터 응답을 받아서 반환
        return openAiService.createChatCompletion(request)
                .getChoices().get(0).getMessage().getContent().trim();
    }

    // 새로운 세션을 생성하고 이전 세션이 주어지면 종료 처리
    public String createNewSession(List<ChatDetailVO> history, String oldSessionId) {
        if (oldSessionId != null && sessionStatusMap.containsKey(oldSessionId)) {
            endChatSession(oldSessionId, history);
        }

        // 새로운 세션 ID 생성 및 활성화 상태로 저장
        String sessionId = UUID.randomUUID().toString();
        sessionStatusMap.put(sessionId, "active");
        return sessionId;
    }

    // 주어진 세션을 종료하고 필요하면 대화 요약을 저장
    public void endChatSession(String sessionId, List<ChatDetailVO> history) {
        sessionStatusMap.put(sessionId, "inactive");
        processSessionSummary(sessionId);
    }

    // 세션의 첫 번째 답변을 요약할 수 있는지 확인하고 요약이 가능하면 데이터베이스에 저장
    private void processSessionSummary(String sessionId) {
        String firstAnswer = chatMapper.getFirstAnswer(sessionId);
        int count = chatMapper.countBySessionId(sessionId);

        // 첫 번째 답변이 있고 대화 이력이 없으면 요약 생성
        if (isSummarizable(firstAnswer, count)) {
            String summary = getSummary(firstAnswer);
            Timestamp endTime = Timestamp.valueOf(LocalDateTime.now());
            chatMapper.saveChat(sessionId, summary, endTime);
            System.out.println("요약 저장 완료");
        } else {
            System.out.println("대화 내용 없어서 요약 불가");
        }
    }

    // 첫 번째 답변이 요약 가능하고 대화 이력이 없는지 확인

    private boolean isSummarizable(String firstAnswer, int count) {
        return firstAnswer != null && !firstAnswer.trim().isEmpty() && count == 0;
    }

    // 데이터베이스에서 특정 세션의 대화 내역을 가져옴
    public List<ChatDetailVO> getChatDetailsBySessionId(String sessionId) {
        return chatMapper.getChatDetailsBySessionId(sessionId);
    }
}