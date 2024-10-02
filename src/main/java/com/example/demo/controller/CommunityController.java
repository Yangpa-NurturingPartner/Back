package com.example.demo.controller;

import com.example.demo.model.community.*;
import com.example.demo.model.PageRequestVO;
import com.example.demo.model.PageResponseVO;
import com.example.demo.security.JwtTokenProvider;
import com.example.demo.service.community.CommunityService;
import com.example.demo.service.member.MemberUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDate;
import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/community")
public class CommunityController {

    private final CommunityService communityService;
    private final MemberUserService memberUserService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${api.host}")
    private String apiHost;

    @Value("${fastapi.port}")
    private String fastApiPort;

    @Autowired
    public CommunityController(CommunityService communityService,
                               MemberUserService memberUserService,
                               JwtTokenProvider jwtTokenProvider) {
        this.communityService = communityService;
        this.memberUserService = memberUserService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.restTemplate = new RestTemplate();
    }

    // 게시물 목록 조회
    @PostMapping("/boards")
    public ResponseEntity<Map<String, Object>> getBoardList(@RequestBody PageRequestVO pageRequestVO) {
        PageResponseVO<CommunityBoardVO> result = communityService.getBoardList(pageRequestVO);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", result);
        response.put("message", "목록 조회에 성공 하였습니다.");
        return ResponseEntity.ok(response);
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
            Long newBoardNo = getNewBoardNo();
            CommunityBoardVO boardVO = buildCommunityBoard(newBoardNo, userNo, childId, boardCode, title, boardContents, file);

            communityService.createBoard(boardVO);

            saveBoardContentToExternalApi(newBoardNo, title, boardContents);

            response.put("status", "success");
            response.put("message", "게시물이 성공적으로 생성되었습니다.");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return handleJsendException("파일 처리 오류", e, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            return handleJsendException("게시물 생성 중 런타임 오류", e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleJsendException("게시물 생성 오류", e, HttpStatus.BAD_REQUEST);
        }
    }

    // board_no로 게시물 세부 정보, 댓글 및 파일 정보 조회
    @GetMapping("/{board_no}")
    public ResponseEntity<Map<String, Object>> getBoardDetails(@PathVariable("board_no") Long boardNo) {
        CommunityBoardDetailVO boardDetail = buildBoardDetailResponse(boardNo);
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("data", boardDetail);
        response.put("message", "조회에 성공 하였습니다.");
        return ResponseEntity.ok(response);
    }

    // 댓글 생성하는 api
    @PostMapping("/boards/addComment")
    public ResponseEntity<Map<String, Object>> addComment(@RequestBody CommentRequestDTO commentRequestDTO) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 새로운 comment_no를 생성
            Long newCommentNo = communityService.getNewCommentNo();

            Long userNo = extractUserNoFromToken(commentRequestDTO.getToken());
            String userEmail = memberUserService.getEmailByUserNo(userNo);

            LocalDate localDate = LocalDate.now();
            Date today = Date.valueOf(localDate);

            CommunityCommentVO commentVO = new CommunityCommentVO();
            commentVO.setComment_no(newCommentNo);
            commentVO.setBoard_no(commentRequestDTO.getBoard_no());
            commentVO.setParents_id(null);
            commentVO.setComments_name(userEmail);
            commentVO.setComments_contents(commentRequestDTO.getComments_contents());
            commentVO.setComments_date(today);

            // 댓글 저장, 갯수 + 1
            communityService.addComment(commentVO);
            communityService.incrementBoardCount(commentRequestDTO.getBoard_no());

            response.put("status", "success");
            response.put("message", "댓글이 성공적으로 추가되었습니다.");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return handleJsendException("댓글 생성 중 런타임 오류", e, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleJsendException("댓글 생성 오류", e, HttpStatus.BAD_REQUEST);
        }
    }

    // 사용자 검색에 따른 문자열 검색
    @PostMapping("/boards/search")
    public ResponseEntity<Map<String, Object>> searchBoards(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            String query = request.get("query");
            String period = request.get("period");

            Map<String, Object> payload = new HashMap<>();
            payload.put("query", query);
            payload.put("user_no", 0);

            // post통신 - 사용자 쿼리 받아 데이터 비교 후 board_no_list로 결과값 사용
            String searchApiUrl = "http://192.168.0.218:9000/search/community";
            ResponseEntity<Map> apiResponse = restTemplate.postForEntity(searchApiUrl, payload, Map.class);

            List<Integer> boardNoList = (List<Integer>) apiResponse.getBody().get("board_no_list");
            List<CommunityBoardVO> boards = communityService.getBoardsByNos(boardNoList);

            // 기간 필터링 처리
            if (period != null && !period.equals("all")) {
                LocalDate endDate = LocalDate.now();
                final LocalDate startDate;

                if (period.equals("week")) {
                    startDate = endDate.minusWeeks(1);
                } else if (period.equals("month")) {
                    startDate = endDate.minusMonths(1);
                } else {
                    startDate = endDate;
                }

                boards = boards.stream()
                        .filter(board -> {
                            LocalDate boardDate = board.getRegister_date().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                            return (boardDate.isEqual(startDate) || boardDate.isAfter(startDate)) && (boardDate.isBefore(endDate) || boardDate.isEqual(endDate));
                        })
                        .collect(Collectors.toList());
            }

            PageResponseVO<CommunityBoardVO> pageResponseVO = new PageResponseVO<>(boards, boards.size(), 1, boards.size());

            response.put("status", "success");
            response.put("data", pageResponseVO);
            response.put("message", "게시물 검색에 성공 하였습니다.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("게시물 검색 중 오류 발생: {}", e.getMessage());
            response.put("status", "error");
            response.put("message", "게시물 검색 중 오류가 발생했습니다.");
            response.put("code", HttpStatus.INTERNAL_SERVER_ERROR.value());
            response.put("data", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 토큰에서 사용자 번호 추출
    private Long extractUserNoFromToken(String token) {
        String jwtToken = token.replace("Bearer ", "");
        Map<String, Object> userInfo = jwtTokenProvider.decodeToken(jwtToken);
        log.info("Decoded user info: {}", userInfo);
        return memberUserService.getUserNoByEmail(userInfo.get("email").toString());
    }

    // 새로운 게시물 번호 생성
    private Long getNewBoardNo() {
        Long lastBoardNo = communityService.getLastInsertedBoardNo();
        return (lastBoardNo != null) ? lastBoardNo + 1 : 1;
    }

    // CommunityBoardVO 생성
    private CommunityBoardVO buildCommunityBoard(Long newBoardNo, Long userNo, Integer childId, Integer boardCode, String title, String boardContents, MultipartFile file) throws IOException {
        CommunityBoardVO boardVO = new CommunityBoardVO();
        boardVO.setBoard_no(Math.toIntExact(newBoardNo));
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

    // 외부 API에 게시물 내용 저장
    private void saveBoardContentToExternalApi(Long boardNo, String title, String boardContents) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("content", title + " " + boardContents);
        payload.put("board", boardNo);

        String externalApiUrl = "http://221.148.97.238:9400/embedded/community/contents";
//        String externalApiUrl = "http://" + apiHost + ":" + fastApiPort + "/embedded/community/contents";
        restTemplate.postForEntity(externalApiUrl, payload, String.class);
    }

    // 게시물 세부 정보 응답 생성
    private CommunityBoardDetailVO buildBoardDetailResponse(Long boardNo) {
        CommunityBoardVO board = communityService.getBoardByNo(boardNo);
        List<CommunityCommentVO> comments = communityService.getCommentsByBoardNo(boardNo);
        List<CommunityFileVO> files = communityService.getFilesByBoardNo(boardNo);

        CommunityBoardResponseDTO boardResponseDTO = new CommunityBoardResponseDTO();
        BeanUtils.copyProperties(board, boardResponseDTO);

        String userEmail = memberUserService.getEmailByUserNo(Long.valueOf(board.getUser_no()));
        boardResponseDTO.setEmail(userEmail);

        CommunityBoardDetailVO boardDetail = new CommunityBoardDetailVO();
        boardDetail.setBoard(boardResponseDTO);
        boardDetail.setComments(comments);
        boardDetail.setFiles(files);

        return boardDetail;
    }

    // 예외 처리 및 응답 생성
    private ResponseEntity<Map<String, Object>> handleException(Map<String, Object> response, String message, Exception e, HttpStatus status) {
        log.error("{}: {}", message, e.getMessage());
        response.put("state", status.value());
        response.put("log", message + ": " + e.getMessage());
        return ResponseEntity.status(status).body(response);
    }

    // 예외 처리 및 응답 생성 - Jsend방식
    private ResponseEntity<Map<String, Object>> handleJsendException(String message, Exception e, HttpStatus status) {
        log.error("{}: {}", message, e.getMessage());
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", message);
        response.put("code", status.value());
        response.put("data", e.getMessage());
        return ResponseEntity.status(status).body(response);
    }
}