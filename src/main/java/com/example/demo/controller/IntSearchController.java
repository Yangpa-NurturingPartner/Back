package com.example.demo.controller;

import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.community.CommunityService;
import com.example.demo.service.member.MemberUserService;
import com.example.demo.model.community.CommunityBoardVO;
import com.example.demo.service.community.CommunityMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/search")
public class IntSearchController {
    private final MemberUserService memberUserService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CommunityMapper communityMapper;

    @Autowired
    private com.example.demo.model.intSearch.SearchResultMapper searchMapper;

    @Autowired
    public IntSearchController(CommunityService communityService,
                               MemberUserService memberUserService,
                               JwtTokenProvider jwtTokenProvider) {
        this.memberUserService = memberUserService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.restTemplate = new RestTemplate();
    }

    @PostMapping("/total")
    public ResponseEntity<Map<String, Object>> search(@RequestBody Map<String, String> request) {
        try {
            String search = request.get("search");
            String token = request.get("token");

            // 사용자 번호 추출
            Long userNo = extractUserNoFromToken(token);

            Map<String, Object> searchRequest = new HashMap<>();
            searchRequest.put("query", search);
            searchRequest.put("user_no", userNo);

            // 사용자 검색에 맞는 임베딩된 데이터에서 결과 찾는 API 호출
            Map<String, Object> response = restTemplate.postForObject(
                    "http://221.148.97.238:9400/search/unified",
                    searchRequest,
                    Map.class
            );

            log.info(response.toString());
            Map<String, Object> results = fetchSearchResults(response);

            // Jsend 성공 응답 포맷
            Map<String, Object> jsendResponse = new HashMap<>();
            jsendResponse.put("status", "success");
            jsendResponse.put("data", results);

            return ResponseEntity.ok(jsendResponse);

        } catch (Exception e) {
            log.error("Error during search: ", e);
            // Jsend 에러 응답 포맷
            Map<String, Object> jsendError = new HashMap<>();
            jsendError.put("status", "error");
            jsendError.put("message", "검색 중 오류가 발생했습니다.");
            jsendError.put("data", e.getMessage());

            return ResponseEntity.status(500).body(jsendError);
        }
    }

    // api 결과 이용하여 db 접근해 매칭된 결과 가져오기
    private Map<String, Object> fetchSearchResults(Map<String, Object> searchResults) {
        Map<String, Object> results = new HashMap<>();

        // 비디오 결과 가져오기
        List<Integer> videoIds = (List<Integer>) searchResults.get("video_results_video_no");
        if (videoIds != null && !videoIds.isEmpty()) {
            List<Map<String, Object>> videoData = searchMapper.getVideoDataByIds(videoIds);
            results.put("video_results", videoData);
        } else {
            results.put("video_results", List.of()); // 비어있는 리스트로 처리
        }

        // 문서 결과 가져오기
        List<Integer> documentIds = (List<Integer>) searchResults.get("document_results_document_no");
        if (documentIds != null && !documentIds.isEmpty()) {
            List<Map<String, Object>> documentData = searchMapper.getDocumentDataByIds(documentIds);
            results.put("document_results", documentData);
        } else {
            results.put("document_results", List.of());
        }

        // 커뮤니티 게시판 결과 가져오기
        List<Integer> communityIds = (List<Integer>) searchResults.get("community_results_board_no");
        if (communityIds != null && !communityIds.isEmpty()) {
            List<CommunityBoardVO> communityData = searchMapper.getCommunityBoardsWithFilesByNos(communityIds);
            results.put("community_results", communityData);
        } else {
            results.put("community_results", List.of());
        }

        // 채팅 결과 가져오기
        List<Integer> chatIds = (List<Integer>) searchResults.get("chat_results_session_id");
        if (chatIds != null && !chatIds.isEmpty()) {
            List<Map<String, Object>> chatData = searchMapper.getChatDataByIds(chatIds);
            results.put("chat_results", chatData);
        } else {
            results.put("chat_results", List.of());
        }

        return results;
    }

    // 토큰에서 사용자 번호 추출
    private Long extractUserNoFromToken(String token) {
        String jwtToken = token.replace("Bearer ", "");
        Map<String, Object> userInfo = jwtTokenProvider.decodeToken(jwtToken);
        log.info("Decoded user info: {}", userInfo);
        return memberUserService.getUserNoByEmail(userInfo.get("email").toString());
    }
}