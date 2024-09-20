package com.example.demo.service;

import com.example.demo.model.ChatDetailVO;
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

    @Autowired
    private ChatMapper chatMapper;

    private final OpenAiService openAiService;
    private final Map<String, String> sessionStatusMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Boolean> sessionMap = new ConcurrentHashMap<>();

    public ChatService(@Value("${spring.ai.openai.api-key}") String openaiApiKey) {
        this.openAiService = new OpenAiService(openaiApiKey, Duration.ofSeconds(30));
    }

    public String getAnswer(String session_id, String query, List<ChatDetailVO> history) {
        if (isSessionEnded(session_id)) {
            endChatSession(session_id, history);
            return "세션 종료";
        }

        //기존 대화 기록x
        if (history == null || history.isEmpty()) {
            return getNewAnswer(query);
        }

        //대화 기록 불러오기 및 처리
        List<ChatMessage> messages = history.stream()
                .map(chat -> List.of(
                        new ChatMessage("user", chat.getQuery()),
                        new ChatMessage("assistant", chat.getAnswer() != null ? chat.getAnswer() : "답변이 없습니다.") // null 값 처리
                ))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        messages.add(new ChatMessage("user", query));

        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4o-mini")
                .messages(messages)
                .maxTokens(1500)
                .temperature(0.7)
                .build();

        String gptResponse = openAiService.createChatCompletion(request)
                .getChoices().get(0).getMessage().getContent().trim();

        return gptResponse;
    }

    //세션 종료 여부 확인
    public boolean isSessionEnded(String sessionId) {
        if (sessionId == null) {
            throw new IllegalArgumentException("세션 아이디가 없습니다");
        }
        return sessionMap.getOrDefault(sessionId, false);
    }


    private String getNewAnswer(String query) {
        ChatMessage queryMessage = new ChatMessage("user", "다음 육아 질문에 대한 간단하게 답변을 해줘: " + query);

        ChatCompletionRequest answerRequest = ChatCompletionRequest.builder()
                .model("gpt-4o-mini")
                .messages(List.of(queryMessage))
                .maxTokens(1500)
                .temperature(0.7)
                .build();

        return openAiService.createChatCompletion(answerRequest)
                .getChoices().get(0).getMessage().getContent().trim();
    }

    public String getSummary(String answer) {
        ChatMessage summaryMessage = new ChatMessage("user", "다음 텍스트의 대화 주제를 10~20글자 사이로 요약해: " + answer);

        ChatCompletionRequest summaryRequest = ChatCompletionRequest.builder()
                .model("gpt-4o-mini")
                .messages(Collections.singletonList(summaryMessage))
                .maxTokens(300)
                .temperature(0.5)
                .build();

        String summ_answer = openAiService.createChatCompletion(summaryRequest)
                .getChoices().get(0).getMessage().getContent().trim();

        return summ_answer;
    }

    //새 세션 발급
    public String createNewSession(List<ChatDetailVO> history, String oldSessionId) {
        if (oldSessionId != null && sessionStatusMap.containsKey(oldSessionId)) {
            endChatSession(oldSessionId, history);
        }

        String session_id = UUID.randomUUID().toString();
        sessionStatusMap.put(session_id, "active");
        return session_id;
    }

    //기존 세션 만료
    public void endChatSession(String session_id, List<ChatDetailVO> history) {
        sessionStatusMap.put(session_id, "inactive");

//        String fullConversation = history.stream()
//                .map(chat -> "질문: " + chat.getQuery() + "\n답변: " + chat.getAnswer())
//                .collect(Collectors.joining("\n\n"));

        String first_answer = chatMapper.getFirstAnswer(session_id);
        String summ_answer = getSummary(first_answer);
        Timestamp end_time = Timestamp.valueOf(LocalDateTime.now());
        chatMapper.saveChat(session_id, summ_answer, end_time);

        System.out.println("요약 저장 완료");
    }

  public List<ChatDetailVO> getChatDetailsBySessionId(String sessionId) {
            return chatMapper.getChatDetailsBySessionId(sessionId);
    }
}


