package com.example.demo.controller;

import com.example.demo.model.community.CommunityBoardVO;
import com.example.demo.model.PageRequestVO;
import com.example.demo.model.PageResponseVO;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.community.CommunityService;
import com.example.demo.service.member.MemberUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/community")
public class CommunityController {

    private final CommunityService communityService;
    private final MemberUserService memberUserService;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public CommunityController(CommunityService communityService, MemberUserService memberUserService, JwtTokenProvider jwtTokenProvider) {
        this.communityService = communityService;
        this.memberUserService = memberUserService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Value("${jwt.secret}")
    private String jwtSecret;

    // 게시물 목록 조회
    @GetMapping("/boards")
    public PageResponseVO<CommunityBoardVO> getBoardList(PageRequestVO pageRequestVO) {
        return communityService.getBoardList(pageRequestVO);
    }

    // 게시물 생성 (파일 업로드 포함)
    @PostMapping("/boards/makeBoard")
    public ResponseEntity<Map<String, Object>> createBoard(
            @RequestParam("token") String token,
            @RequestParam("child_id") Integer childId,
            @RequestParam("board_code") Integer boardCode,
            @RequestParam("title") String title,
            @RequestParam("board_contents") String boardContents,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            Long userNo = extractUserNoFromToken(token);

            CommunityBoardVO boardVO = buildCommunityBoard(userNo, childId, boardCode, title, boardContents, file);

            communityService.createBoard(boardVO);

            response.put("state", HttpStatus.OK.value());
            response.put("log", "Board Created Successfully");
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("File processing error: {}", e.getMessage());
            return buildErrorResponse(response, HttpStatus.INTERNAL_SERVER_ERROR, "File processing error: " + e.getMessage());

        } catch (RuntimeException e) {
            log.error("Board creation runtime error: {}", e.getMessage());
            return buildErrorResponse(response, HttpStatus.BAD_REQUEST, "Board creation error: " + e.getMessage());

        } catch (Exception e) {
            log.error("Board creation error: {}", e.getMessage());
            return buildErrorResponse(response, HttpStatus.BAD_REQUEST, "Board creation error: " + e.getMessage());
        }
    }

    // 토큰에서 사용자 번호 추출
    private Long extractUserNoFromToken(String token) {
        String jwtToken = token.replace("Bearer ", "");
        Map<String, Object> userInfo = jwtTokenProvider.decodeToken(jwtToken);
        log.info("Decoded user info: {}", userInfo);
        return memberUserService.getUserNoByEmail(userInfo.get("email").toString());
    }

    // CommunityBoardVO 생성
    private CommunityBoardVO buildCommunityBoard(Long userNo, Integer childId, Integer boardCode, String title, String boardContents, MultipartFile file) throws IOException {
        CommunityBoardVO boardVO = new CommunityBoardVO();
        boardVO.setUser_no(Math.toIntExact(userNo));
        boardVO.setChild_id(childId);
        boardVO.setBoard_code(boardCode);
        boardVO.setTitle(title);
        boardVO.setBoard_contents(boardContents);

        if (file != null && !file.isEmpty()) {
            boardVO.setFile(file);
        }
        return boardVO;
    }

    // 에러 응답 생성 메서드
    private ResponseEntity<Map<String, Object>> buildErrorResponse(Map<String, Object> response, HttpStatus status, String logMessage) {
        response.put("state", status.value());
        response.put("log", logMessage);
        return ResponseEntity.status(status).body(response);
    }
}